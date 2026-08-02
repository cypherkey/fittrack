package com.fittrack.service;

import com.fittrack.domain.Exercise;
import com.fittrack.domain.TemplateSet;
import com.fittrack.domain.TemplateVisibility;
import com.fittrack.domain.User;
import com.fittrack.domain.Workout;
import com.fittrack.domain.WorkoutSet;
import com.fittrack.domain.WorkoutTemplate;
import com.fittrack.repository.WorkoutTemplateRepository;
import com.fittrack.web.dto.CloneTemplateRequest;
import com.fittrack.web.dto.TemplateRequest;
import com.fittrack.web.dto.TemplateResponse;
import com.fittrack.web.dto.TemplateSetRequest;
import com.fittrack.web.dto.TemplateSetResponse;
import com.fittrack.web.dto.WorkoutResponse;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TemplateService {

	private final WorkoutTemplateRepository templateRepository;
	private final ExerciseService exerciseService;
	private final WorkoutService workoutService;

	public TemplateService(
			WorkoutTemplateRepository templateRepository,
			ExerciseService exerciseService,
			WorkoutService workoutService
	) {
		this.templateRepository = templateRepository;
		this.exerciseService = exerciseService;
		this.workoutService = workoutService;
	}

	@Transactional(readOnly = true)
	public List<TemplateResponse> list(User user, TemplateVisibility visibility) {
		List<WorkoutTemplate> templates;
		if (visibility == TemplateVisibility.PUBLIC) {
			templates = templateRepository.findByVisibilityOrderByUpdatedAtDesc(TemplateVisibility.PUBLIC);
		}
		else {
			templates = templateRepository.findByUser_IdOrderByUpdatedAtDesc(user.getId());
		}
		return templates.stream().map(t -> toResponse(t, false)).toList();
	}

	@Transactional(readOnly = true)
	public TemplateResponse get(User user, String id) {
		return toResponse(requireReadable(user, id), true);
	}

	@Transactional
	public TemplateResponse create(User user, TemplateRequest request) {
		WorkoutTemplate template = new WorkoutTemplate();
		template.setUser(user);
		applyMetadata(template, request);
		replaceSets(user, template, request.sets());
		templateRepository.save(template);
		return toResponse(template, true);
	}

	@Transactional
	public TemplateResponse update(User user, String id, TemplateRequest request) {
		WorkoutTemplate template = requireOwned(user, id);
		applyMetadata(template, request);
		replaceSets(user, template, request.sets());
		templateRepository.save(template);
		return toResponse(template, true);
	}

	@Transactional
	public void delete(User user, String id) {
		templateRepository.delete(requireOwned(user, id));
	}

	@Transactional
	public WorkoutResponse cloneToWorkout(User user, String id, CloneTemplateRequest request) {
		WorkoutTemplate template = requireReadable(user, id);
		Workout workout = new Workout();
		workout.setUser(user);
		workout.setStartedAt(null);
		workout.setEndedAt(null);
		workout.setCompleted(false);
		workout.setName(request.name() != null ? request.name() : template.getName());
		workout.setDifficulty(template.getDifficulty());
		workout.setNotes(template.getNotes());
		workout.setSourceTemplate(template);

		for (TemplateSet templateSet : template.getSets()) {
			WorkoutSet set = new WorkoutSet();
			set.setWorkout(workout);
			set.setExercise(templateSet.getExercise());
			set.setSetNumber(templateSet.getSetNumber());
			set.setReps(templateSet.getReps());
			set.setWeightKg(templateSet.getWeightKg());
			set.setDurationSeconds(templateSet.getDurationSeconds());
			set.setDistanceMeters(templateSet.getDistanceMeters());
			set.setRpe(null);
			set.setNotes(templateSet.getNotes());
			set.setCompleted(false);
			workout.getSets().add(set);
		}
		workout.setTotalWeightLifted(WorkoutService.computeTotal(workout.getSets()));
		return workoutService.saveCloned(workout);
	}

	/**
	 * Reassigns setNumbers from the client. Uses a two-phase update to avoid unique-constraint clashes.
	 */
	@Transactional
	public TemplateResponse reorderSets(User user, String id, List<com.fittrack.web.dto.ReorderSetItem> items) {
		WorkoutTemplate template = requireOwned(user, id);
		Map<String, TemplateSet> byId = template.getSets().stream()
				.collect(Collectors.toMap(TemplateSet::getId, s -> s));
		if (items.size() != byId.size() || items.stream().anyMatch(i -> !byId.containsKey(i.setId()))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reorder must include every set exactly once");
		}
		WorkoutService.assertUniqueSetNumbers(items.stream().map(com.fittrack.web.dto.ReorderSetItem::setNumber).toList());
		int temp = -1;
		for (com.fittrack.web.dto.ReorderSetItem item : items) {
			byId.get(item.setId()).setSetNumber(temp--);
		}
		templateRepository.flush();
		for (com.fittrack.web.dto.ReorderSetItem item : items) {
			byId.get(item.setId()).setSetNumber(item.setNumber());
		}
		templateRepository.flush();
		templateRepository.save(template);
		return toResponse(template, true);
	}

	private void applyMetadata(WorkoutTemplate template, TemplateRequest request) {
		template.setName(request.name());
		template.setDifficulty(request.difficulty());
		template.setNotes(request.notes());
		template.setVisibility(request.visibility() != null ? request.visibility() : TemplateVisibility.PRIVATE);
	}

	private void replaceSets(User user, WorkoutTemplate template, List<TemplateSetRequest> setRequests) {
		template.getSets().clear();
		templateRepository.flush();
		if (setRequests == null || setRequests.isEmpty()) {
			return;
		}
		WorkoutService.assertUniqueSetNumbers(setRequests.stream().map(TemplateSetRequest::setNumber).toList());
		for (TemplateSetRequest req : setRequests) {
			Exercise exercise = exerciseService.requireUsableBy(user, req.exerciseId());
			TemplateSet set = new TemplateSet();
			set.setTemplate(template);
			set.setExercise(exercise);
			set.setSetNumber(req.setNumber());
			set.setReps(req.reps());
			set.setWeightKg(req.weightKg());
			set.setDurationSeconds(req.durationSeconds());
			set.setDistanceMeters(req.distanceMeters());
			set.setNotes(req.notes());
			template.getSets().add(set);
		}
	}

	private WorkoutTemplate requireOwned(User user, String id) {
		WorkoutTemplate template = templateRepository.findWithSetsById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));
		if (!template.getUser().getId().equals(user.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the owner of this template");
		}
		return template;
	}

	private WorkoutTemplate requireReadable(User user, String id) {
		WorkoutTemplate template = templateRepository.findWithSetsById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found"));
		boolean owner = template.getUser().getId().equals(user.getId());
		boolean pub = template.getVisibility() == TemplateVisibility.PUBLIC;
		if (!owner && !pub) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Template not found");
		}
		return template;
	}

	private TemplateResponse toResponse(WorkoutTemplate template, boolean includeSets) {
		int setCount = template.getSets().size();
		List<TemplateSetResponse> sets = includeSets
				? template.getSets().stream()
						.sorted(Comparator.comparingInt(TemplateSet::getSetNumber))
						.map(this::toSetResponse)
						.toList()
				: List.of();
		return new TemplateResponse(
				template.getId(),
				template.getUser().getId(),
				template.getName(),
				template.getDifficulty(),
				template.getNotes(),
				template.getVisibility(),
				template.getCreatedAt(),
				template.getUpdatedAt(),
				setCount,
				sets
		);
	}

	private TemplateSetResponse toSetResponse(TemplateSet set) {
		return new TemplateSetResponse(
				set.getId(),
				set.getExercise().getId(),
				set.getExercise().getName(),
				set.getSetNumber(),
				set.getReps(),
				set.getWeightKg(),
				set.getDurationSeconds(),
				set.getDistanceMeters(),
				set.getNotes()
		);
	}
}
