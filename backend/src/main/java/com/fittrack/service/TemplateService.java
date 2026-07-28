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
import java.util.List;
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
		workout.setPerformedAt(request.performedAt());
		workout.setName(request.name() != null ? request.name() : template.getName());
		workout.setDurationSeconds(template.getDurationSeconds());
		workout.setDifficulty(template.getDifficulty());
		workout.setNotes(template.getNotes());
		workout.setSourceTemplate(template);

		for (TemplateSet templateSet : template.getSets()) {
			Exercise exercise = exerciseService.requireUsableBy(user, templateSet.getExercise().getId());
			WorkoutSet set = new WorkoutSet();
			set.setWorkout(workout);
			set.setExercise(exercise);
			set.setSetNumber(templateSet.getSetNumber());
			set.setReps(templateSet.getReps());
			set.setWeightKg(templateSet.getWeightKg());
			set.setDurationSeconds(templateSet.getDurationSeconds());
			set.setDistanceMeters(templateSet.getDistanceMeters());
			set.setRpe(templateSet.getRpe());
			set.setNotes(templateSet.getNotes());
			set.setCompleted(true);
			workout.getSets().add(set);
		}
		workout.setTotalWeightLifted(WorkoutService.computeTotal(workout.getSets()));
		return workoutService.saveCloned(workout);
	}

	private void applyMetadata(WorkoutTemplate template, TemplateRequest request) {
		template.setName(request.name());
		template.setDurationSeconds(request.durationSeconds());
		template.setDifficulty(request.difficulty());
		template.setNotes(request.notes());
		template.setVisibility(request.visibility() != null ? request.visibility() : TemplateVisibility.PRIVATE);
	}

	private void replaceSets(User user, WorkoutTemplate template, List<TemplateSetRequest> setRequests) {
		template.getSets().clear();
		if (setRequests == null || setRequests.isEmpty()) {
			template.setTotalWeightLifted(null);
			return;
		}
		WorkoutService.assertUniqueSetNumbers(setRequests.stream().map(TemplateSetRequest::setNumber).toList());
		boolean isPublic = template.getVisibility() == TemplateVisibility.PUBLIC;
		for (TemplateSetRequest req : setRequests) {
			Exercise exercise = exerciseService.requireUsableBy(user, req.exerciseId());
			if (isPublic && exercise.isCustom()) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PUBLIC templates may only include catalog exercises");
			}
			TemplateSet set = new TemplateSet();
			set.setTemplate(template);
			set.setExercise(exercise);
			set.setSetNumber(req.setNumber());
			set.setReps(req.reps());
			set.setWeightKg(req.weightKg());
			set.setDurationSeconds(req.durationSeconds());
			set.setDistanceMeters(req.distanceMeters());
			set.setRpe(req.rpe());
			set.setNotes(req.notes());
			template.getSets().add(set);
		}
		template.setTotalWeightLifted(SetWeightTotals.compute(template.getSets().stream()
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
				.toList()));
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
		List<TemplateSetResponse> sets = includeSets
				? template.getSets().stream().map(this::toSetResponse).toList()
				: List.of();
		return new TemplateResponse(
				template.getId(),
				template.getUser().getId(),
				template.getName(),
				template.getDurationSeconds(),
				template.getTotalWeightLifted(),
				template.getDifficulty(),
				template.getNotes(),
				template.getVisibility(),
				template.getCreatedAt(),
				template.getUpdatedAt(),
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
				set.getRpe(),
				set.getNotes()
		);
	}
}
