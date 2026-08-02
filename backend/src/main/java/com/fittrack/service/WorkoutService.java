package com.fittrack.service;

import com.fittrack.domain.Exercise;
import com.fittrack.domain.User;
import com.fittrack.domain.Workout;
import com.fittrack.domain.WorkoutSet;
import com.fittrack.domain.WorkoutTemplate;
import com.fittrack.repository.WorkoutRepository;
import com.fittrack.repository.WorkoutTemplateRepository;
import com.fittrack.web.dto.WorkoutRequest;
import com.fittrack.web.dto.WorkoutResponse;
import com.fittrack.web.dto.WorkoutSetRequest;
import com.fittrack.web.dto.WorkoutSetResponse;
import java.time.Instant;
import java.util.HashSet;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkoutService {

	private final WorkoutRepository workoutRepository;
	private final WorkoutTemplateRepository templateRepository;
	private final ExerciseService exerciseService;

	public WorkoutService(
			WorkoutRepository workoutRepository,
			WorkoutTemplateRepository templateRepository,
			ExerciseService exerciseService
	) {
		this.workoutRepository = workoutRepository;
		this.templateRepository = templateRepository;
		this.exerciseService = exerciseService;
	}

	@Transactional(readOnly = true)
	public List<WorkoutResponse> list(User user, Instant from, Instant to) {
		return workoutRepository.findByUserIdAndStartedAtRange(user.getId(), from, to).stream()
				.map(w -> toResponse(w, false))
				.toList();
	}

	@Transactional(readOnly = true)
	public WorkoutResponse get(User user, String id) {
		return toResponse(requireOwned(user, id), true);
	}

	@Transactional
	public WorkoutResponse create(User user, WorkoutRequest request) {
		Workout workout = new Workout();
		workout.setUser(user);
		applyMetadata(user, workout, request);
		replaceSets(user, workout, request.sets());
		workoutRepository.save(workout);
		return toResponse(workout, true);
	}

	@Transactional
	public WorkoutResponse update(User user, String id, WorkoutRequest request) {
		Workout workout = requireOwned(user, id);
		applyMetadata(user, workout, request);
		replaceSets(user, workout, request.sets());
		workoutRepository.save(workout);
		return toResponse(workout, true);
	}

	@Transactional
	public void delete(User user, String id) {
		Workout workout = requireOwned(user, id);
		workoutRepository.delete(workout);
	}

	/**
	 * Reassigns setNumbers from the client. Uses a two-phase update to avoid unique-constraint clashes.
	 */
	@Transactional
	public WorkoutResponse reorderSets(User user, String id, List<com.fittrack.web.dto.ReorderSetItem> items) {
		Workout workout = requireOwned(user, id);
		Map<String, WorkoutSet> byId = workout.getSets().stream()
				.collect(Collectors.toMap(WorkoutSet::getId, s -> s));
		if (items.size() != byId.size() || items.stream().anyMatch(i -> !byId.containsKey(i.setId()))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reorder must include every set exactly once");
		}
		assertUniqueSetNumbers(items.stream().map(com.fittrack.web.dto.ReorderSetItem::setNumber).toList());
		int temp = -1;
		for (com.fittrack.web.dto.ReorderSetItem item : items) {
			byId.get(item.setId()).setSetNumber(temp--);
		}
		workoutRepository.flush();
		for (com.fittrack.web.dto.ReorderSetItem item : items) {
			byId.get(item.setId()).setSetNumber(item.setNumber());
		}
		workoutRepository.flush();
		workoutRepository.save(workout);
		return toResponse(workout, true);
	}

	@Transactional
	public WorkoutResponse saveCloned(Workout workout) {
		workoutRepository.save(workout);
		return toResponse(workout, true);
	}

	@Transactional
	public WorkoutResponse start(User user, String id) {
		Workout workout = requireOwned(user, id);
		if (workout.getStartedAt() == null) {
			workout.setStartedAt(Instant.now());
			workoutRepository.save(workout);
		}
		return toResponse(workout, true);
	}

	@Transactional
	public WorkoutResponse complete(User user, String id) {
		Workout workout = requireOwned(user, id);
		Instant now = Instant.now();
		if (workout.getStartedAt() == null) {
			workout.setStartedAt(now);
		}
		workout.setEndedAt(now);
		workout.setCompleted(true);
		workout.setTotalWeightLifted(computeTotal(workout.getSets()));
		workoutRepository.save(workout);
		return toResponse(workout, true);
	}

	private void applyMetadata(User user, Workout workout, WorkoutRequest request) {
		workout.setStartedAt(request.startedAt());
		workout.setEndedAt(request.endedAt());
		workout.setName(request.name());
		workout.setCompleted(request.completed() != null && request.completed());
		workout.setDifficulty(request.difficulty());
		workout.setNotes(request.notes());
		if (StringUtils.hasText(request.sourceTemplateId())) {
			WorkoutTemplate template = templateRepository.findById(request.sourceTemplateId())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source template not found"));
			boolean owner = template.getUser().getId().equals(user.getId());
			boolean pub = template.getVisibility() == com.fittrack.domain.TemplateVisibility.PUBLIC;
			if (!owner && !pub) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Source template not accessible");
			}
			workout.setSourceTemplate(template);
		}
		else {
			workout.setSourceTemplate(null);
		}
	}

	private void replaceSets(User user, Workout workout, List<WorkoutSetRequest> setRequests) {
		workout.getSets().clear();
		workoutRepository.flush();
		if (setRequests == null || setRequests.isEmpty()) {
			workout.setTotalWeightLifted(null);
			return;
		}
		assertUniqueSetNumbers(setRequests.stream().map(WorkoutSetRequest::setNumber).toList());
		for (WorkoutSetRequest req : setRequests) {
			Exercise exercise = exerciseService.requireUsableBy(user, req.exerciseId());
			WorkoutSet set = new WorkoutSet();
			set.setWorkout(workout);
			set.setExercise(exercise);
			set.setSetNumber(req.setNumber());
			set.setReps(req.reps());
			set.setWeightKg(req.weightKg());
			set.setDurationSeconds(req.durationSeconds());
			set.setDistanceMeters(req.distanceMeters());
			set.setCompleted(req.completed() == null || req.completed());
			set.setRpe(req.rpe());
			set.setNotes(req.notes());
			workout.getSets().add(set);
		}
		workout.setTotalWeightLifted(computeTotal(workout.getSets()));
	}

	static Double computeTotal(List<WorkoutSet> sets) {
		return SetWeightTotals.compute(sets.stream()
				.map(s -> (SetWeightTotals.WeightedSet) new SetWeightTotals.WeightedSet() {
					@Override
					public Integer reps() {
						return s.getReps();
					}

					@Override
					public Double weightKg() {
						return s.getWeightKg();
					}
				})
				.toList());
	}

	static void assertUniqueSetNumbers(List<Integer> setNumbers) {
		Set<Integer> seen = new HashSet<>();
		for (Integer number : setNumbers) {
			if (number == null || !seen.add(number)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate or missing setNumber");
			}
		}
	}

	private Workout requireOwned(User user, String id) {
		Workout workout = workoutRepository.findWithSetsById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workout not found"));
		if (!workout.getUser().getId().equals(user.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the owner of this workout");
		}
		return workout;
	}

	WorkoutResponse toResponse(Workout workout, boolean includeSets) {
		int setCount = workout.getSets().size();
		List<WorkoutSetResponse> sets = includeSets
				? workout.getSets().stream()
						.sorted(Comparator.comparingInt(WorkoutSet::getSetNumber))
						.map(this::toSetResponse)
						.toList()
				: List.of();
		return new WorkoutResponse(
				workout.getId(),
				workout.getUser().getId(),
				workout.getStartedAt(),
				workout.getEndedAt(),
				workout.getName(),
				workout.isCompleted(),
				workout.getTotalWeightLifted(),
				workout.getDifficulty(),
				workout.getNotes(),
				workout.getSourceTemplate() != null ? workout.getSourceTemplate().getId() : null,
				workout.getCreatedAt(),
				workout.getUpdatedAt(),
				setCount,
				sets
		);
	}

	private WorkoutSetResponse toSetResponse(WorkoutSet set) {
		return new WorkoutSetResponse(
				set.getId(),
				set.getExercise().getId(),
				set.getExercise().getName(),
				set.getSetNumber(),
				set.getReps(),
				set.getWeightKg(),
				set.getDurationSeconds(),
				set.getDistanceMeters(),
				set.isCompleted(),
				set.getRpe(),
				set.getNotes()
		);
	}
}
