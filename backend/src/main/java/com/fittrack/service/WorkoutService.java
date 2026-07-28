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
import java.util.List;
import java.util.Set;
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
		return workoutRepository.findByUserIdAndPerformedAtRange(user.getId(), from, to).stream()
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

	@Transactional
	public WorkoutResponse saveCloned(Workout workout) {
		workoutRepository.save(workout);
		return toResponse(workout, true);
	}

	private void applyMetadata(User user, Workout workout, WorkoutRequest request) {
		workout.setPerformedAt(request.performedAt());
		workout.setName(request.name());
		workout.setDurationSeconds(request.durationSeconds());
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
		List<WorkoutSetResponse> sets = includeSets
				? workout.getSets().stream().map(this::toSetResponse).toList()
				: List.of();
		return new WorkoutResponse(
				workout.getId(),
				workout.getUser().getId(),
				workout.getPerformedAt(),
				workout.getName(),
				workout.getDurationSeconds(),
				workout.getTotalWeightLifted(),
				workout.getDifficulty(),
				workout.getNotes(),
				workout.getSourceTemplate() != null ? workout.getSourceTemplate().getId() : null,
				workout.getCreatedAt(),
				workout.getUpdatedAt(),
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
