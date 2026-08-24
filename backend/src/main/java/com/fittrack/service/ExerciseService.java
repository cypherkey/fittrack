package com.fittrack.service;

import com.fittrack.domain.Equipment;
import com.fittrack.domain.Exercise;
import com.fittrack.domain.ExerciseHasMuscle;
import com.fittrack.domain.Muscle;
import com.fittrack.domain.UserFavoriteExercise;
import com.fittrack.domain.TrackedParameters;
import com.fittrack.domain.User;
import com.fittrack.repository.EquipmentRepository;
import com.fittrack.repository.ExerciseHasImageRepository;
import com.fittrack.repository.ExerciseHasMuscleRepository;
import com.fittrack.repository.ExerciseRepository;
import com.fittrack.repository.MuscleRepository;
import com.fittrack.repository.UserFavoriteExerciseRepository;
import com.fittrack.repository.TemplateSetRepository;
import com.fittrack.repository.WorkoutSetRepository;
import com.fittrack.domain.WorkoutSet;
import com.fittrack.web.dto.ExerciseHistoryEntryResponse;
import com.fittrack.web.dto.ExerciseImageResponse;
import com.fittrack.web.dto.ExerciseMuscleResponse;
import com.fittrack.web.dto.ExerciseRequest;
import com.fittrack.web.dto.ExerciseResponse;
import com.fittrack.web.dto.MuscleLinkRequest;
import com.fittrack.web.dto.PageResponse;
import com.fittrack.web.dto.TrackedParametersRequest;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExerciseService {

	private final ExerciseRepository exerciseRepository;
	private final EquipmentRepository equipmentRepository;
	private final MuscleRepository muscleRepository;
	private final ExerciseHasMuscleRepository exerciseHasMuscleRepository;
	private final ExerciseHasImageRepository exerciseHasImageRepository;
	private final TemplateSetRepository templateSetRepository;
	private final WorkoutSetRepository workoutSetRepository;
	private final UserFavoriteExerciseRepository userFavoriteExerciseRepository;

	public ExerciseService(
			ExerciseRepository exerciseRepository,
			EquipmentRepository equipmentRepository,
			MuscleRepository muscleRepository,
			ExerciseHasMuscleRepository exerciseHasMuscleRepository,
			ExerciseHasImageRepository exerciseHasImageRepository,
			TemplateSetRepository templateSetRepository,
			WorkoutSetRepository workoutSetRepository,
			UserFavoriteExerciseRepository userFavoriteExerciseRepository
	) {
		this.exerciseRepository = exerciseRepository;
		this.equipmentRepository = equipmentRepository;
		this.muscleRepository = muscleRepository;
		this.exerciseHasMuscleRepository = exerciseHasMuscleRepository;
		this.exerciseHasImageRepository = exerciseHasImageRepository;
		this.templateSetRepository = templateSetRepository;
		this.workoutSetRepository = workoutSetRepository;
		this.userFavoriteExerciseRepository = userFavoriteExerciseRepository;
	}

	@Transactional(readOnly = true)
	public PageResponse<ExerciseResponse> list(
			User user,
			String q,
			String muscle,
			String equipment,
			String category,
			boolean customOnly,
			boolean favoriteOnly,
			int page,
			int size
	) {
		int safePage = Math.max(page, 0);
		int safeSize = Math.min(Math.max(size, 1), 100);
		Specification<Exercise> spec = visibleTo(user.getId());
		spec = and(spec, customOnlySpec(customOnly));
		spec = and(spec, favoriteOnlySpec(user.getId(), favoriteOnly));
		spec = and(spec, nameContains(q));
		spec = and(spec, categoryEquals(category));
		spec = and(spec, equipmentMatches(equipment));
		spec = and(spec, muscleMatches(muscle));

		Page<Exercise> result = exerciseRepository.findAll(
				spec,
				PageRequest.of(safePage, safeSize, Sort.by("name").ascending())
		);
		List<ExerciseResponse> content = toResponses(user, result.getContent());
		return new PageResponse<>(
				content,
				result.getNumber(),
				result.getSize(),
				result.getTotalElements(),
				result.getTotalPages()
		);
	}

	@Transactional(readOnly = true)
	public ExerciseResponse get(User user, String id) {
		Exercise exercise = exerciseRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exercise not found"));
		return toResponse(exercise, isFavorite(user, exercise.getId()));
	}

	@Transactional
	public ExerciseResponse setFavorite(User user, String id, boolean favorite) {
		Exercise exercise = exerciseRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exercise not found"));
		if (favorite) {
			if (!userFavoriteExerciseRepository.existsByUser_IdAndExercise_Id(user.getId(), exercise.getId())) {
				UserFavoriteExercise row = new UserFavoriteExercise();
				row.setUser(user);
				row.setExercise(exercise);
				userFavoriteExerciseRepository.save(row);
			}
		}
		else {
			userFavoriteExerciseRepository.deleteByUser_IdAndExercise_Id(user.getId(), exercise.getId());
		}
		return toResponse(exercise, favorite);
	}

	@Transactional(readOnly = true)
	public List<ExerciseHistoryEntryResponse> history(User user, String id) {
		if (!exerciseRepository.existsById(id)) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Exercise not found");
		}
		List<WorkoutSet> sets = new ArrayList<>(workoutSetRepository.findByExerciseIdAndUserId(id, user.getId()));
		sets.sort(Comparator
				.comparing((WorkoutSet ws) -> ws.getWorkout().getStartedAt(), Comparator.nullsLast(Comparator.reverseOrder()))
				.thenComparingInt(WorkoutSet::getSetNumber));
		return sets.stream()
				.map(ws -> new ExerciseHistoryEntryResponse(
						ws.getWorkout().getStartedAt(),
						ws.getSetNumber(),
						ws.getReps(),
						ws.getWeightKg(),
						ws.getRpe()
				))
				.toList();
	}

	@Transactional
	public ExerciseResponse create(User user, ExerciseRequest request) {
		Exercise exercise = new Exercise();
		exercise.setId(UUID.randomUUID().toString());
		exercise.setCustom(true);
		exercise.setAddedBy(user);
		applyRequest(exercise, request);
		exerciseRepository.save(exercise);
		replaceMuscles(exercise, request.muscles());
		return toResponse(exercise, isFavorite(user, exercise.getId()));
	}

	@Transactional
	public ExerciseResponse update(User user, String id, ExerciseRequest request) {
		Exercise exercise = requireOwnedCustom(user, id);
		applyRequest(exercise, request);
		exerciseRepository.save(exercise);
		replaceMuscles(exercise, request.muscles());
		return toResponse(exercise, isFavorite(user, exercise.getId()));
	}

	@Transactional
	public ExerciseResponse updateTrackedParameters(User user, String id, TrackedParametersRequest request) {
		if (!user.isAdmin()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins may update tracked parameters");
		}
		Exercise exercise = requireVisible(user, id);
		exercise.setTrackedParameters(request.getTrackedParameters());
		if (request.isVideoUrlPresent()) {
			String videoUrl = request.getVideoUrl();
			exercise.setVideoUrl(StringUtils.hasText(videoUrl) ? videoUrl.trim() : null);
		}
		exerciseRepository.save(exercise);
		return toResponse(exercise, isFavorite(user, exercise.getId()));
	}

	@Transactional
	public void delete(User user, String id) {
		Exercise exercise = requireOwnedCustom(user, id);
		if (templateSetRepository.existsByExercise_Id(id) || workoutSetRepository.existsByExercise_Id(id)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Exercise is referenced by templates or workouts");
		}
		exerciseHasMuscleRepository.deleteByExerciseId(id);
		exerciseHasImageRepository.deleteByExerciseId(id);
		userFavoriteExerciseRepository.deleteByExercise_Id(id);
		exerciseRepository.delete(exercise);
	}

	/**
	 * Resolves an exercise for use on a template or workout set.
	 * Any existing exercise (catalog or custom) may be referenced.
	 */
	@Transactional(readOnly = true)
	public Exercise requireUsableBy(User user, String exerciseId) {
		return exerciseRepository.findById(exerciseId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exercise not found: " + exerciseId));
	}

	private void applyRequest(Exercise exercise, ExerciseRequest request) {
		exercise.setName(request.name());
		exercise.setForce(request.force());
		exercise.setLevel(request.level());
		exercise.setMechanic(request.mechanic());
		exercise.setInstructions(request.instructions() != null ? request.instructions() : "");
		exercise.setVideoUrl(StringUtils.hasText(request.videoUrl()) ? request.videoUrl().trim() : null);
		exercise.setCategory(request.category());
		exercise.setTrackedParameters(
				request.trackedParameters() != null
						? request.trackedParameters()
						: TrackedParameters.REPS | TrackedParameters.WEIGHT
		);
		if (StringUtils.hasText(request.equipmentId())) {
			Equipment equipment = equipmentRepository.findById(request.equipmentId())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Equipment not found"));
			exercise.setEquipment(equipment);
		}
		else {
			exercise.setEquipment(null);
		}
	}

	private void replaceMuscles(Exercise exercise, List<MuscleLinkRequest> muscles) {
		exerciseHasMuscleRepository.deleteByExerciseId(exercise.getId());
		if (muscles == null || muscles.isEmpty()) {
			return;
		}
		List<ExerciseHasMuscle> links = new ArrayList<>();
		for (MuscleLinkRequest link : muscles) {
			Muscle muscle = muscleRepository.findById(link.muscleId())
					.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Muscle not found: " + link.muscleId()));
			ExerciseHasMuscle row = new ExerciseHasMuscle();
			row.setExercise(exercise);
			row.setMuscle(muscle);
			row.setPrimary(link.primary());
			links.add(row);
		}
		exerciseHasMuscleRepository.saveAll(links);
	}

	private Exercise requireOwnedCustom(User user, String id) {
		Exercise exercise = exerciseRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exercise not found"));
		if (!exercise.isCustom()) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Catalog exercises are read-only");
		}
		if (exercise.getAddedBy() == null || !exercise.getAddedBy().getId().equals(user.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the owner of this exercise");
		}
		return exercise;
	}

	private Exercise requireVisible(User user, String id) {
		Exercise exercise = exerciseRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exercise not found"));
		if (!exercise.isCustom()) {
			return exercise;
		}
		if (exercise.getAddedBy() == null || !exercise.getAddedBy().getId().equals(user.getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the owner of this exercise");
		}
		return exercise;
	}

	private List<ExerciseResponse> toResponses(User user, List<Exercise> exercises) {
		if (exercises.isEmpty()) {
			return List.of();
		}
		Set<String> ids = exercises.stream().map(Exercise::getId).collect(Collectors.toSet());
		Set<String> favoriteIds = userFavoriteExerciseRepository
				.findByUser_IdAndExercise_IdIn(user.getId(), ids)
				.stream()
				.map(row -> row.getExercise().getId())
				.collect(Collectors.toCollection(HashSet::new));
		return exercises.stream().map(ex -> toResponse(ex, favoriteIds.contains(ex.getId()))).toList();
	}

	private boolean isFavorite(User user, String exerciseId) {
		return userFavoriteExerciseRepository.existsByUser_IdAndExercise_Id(user.getId(), exerciseId);
	}

	private ExerciseResponse toResponse(Exercise exercise, boolean favorite) {
		List<ExerciseMuscleResponse> muscles = exerciseHasMuscleRepository.findByExercise_Id(exercise.getId()).stream()
				.map(link -> new ExerciseMuscleResponse(
						link.getMuscle().getId(),
						link.getMuscle().getName(),
						link.isPrimary()
				))
				.toList();
		List<ExerciseImageResponse> images = exerciseHasImageRepository.findByExercise_IdOrderBySortOrderAsc(exercise.getId()).stream()
				.map(link -> new ExerciseImageResponse(
						link.getImage().getId(),
						link.getImage().getPath(),
						link.getImage().getAltText(),
						link.getSortOrder(),
						link.getImage().getContentType(),
						link.getImage().getContentBase64()
				))
				.toList();
		Equipment equipment = exercise.getEquipment();
		return new ExerciseResponse(
				exercise.getId(),
				exercise.getName(),
				exercise.getForce(),
				exercise.getLevel(),
				exercise.getMechanic(),
				equipment != null ? equipment.getId() : null,
				equipment != null ? equipment.getName() : null,
				exercise.getInstructions(),
				exercise.getVideoUrl(),
				exercise.getCategory(),
				exercise.getTrackedParameters(),
				exercise.isCustom(),
				favorite,
				exercise.getAddedBy() != null ? exercise.getAddedBy().getId() : null,
				muscles,
				images
		);
	}

	private static Specification<Exercise> and(Specification<Exercise> base, Specification<Exercise> extra) {
		return extra == null ? base : base.and(extra);
	}

	private static Specification<Exercise> visibleTo(String userId) {
		return (root, query, cb) -> {
			Join<Exercise, User> addedBy = root.join("addedBy", JoinType.LEFT);
			return cb.or(
					cb.isFalse(root.get("custom")),
					cb.equal(addedBy.get("id"), userId)
			);
		};
	}

	private static Specification<Exercise> customOnlySpec(boolean customOnly) {
		if (!customOnly) {
			return null;
		}
		return (root, query, cb) -> cb.isTrue(root.get("custom"));
	}

	private static Specification<Exercise> favoriteOnlySpec(String userId, boolean favoriteOnly) {
		if (!favoriteOnly) {
			return null;
		}
		return (root, query, cb) -> {
			var subquery = query.subquery(String.class);
			var favorite = subquery.from(UserFavoriteExercise.class);
			subquery.select(favorite.get("exercise").get("id"));
			subquery.where(cb.and(
					cb.equal(favorite.get("user").get("id"), userId),
					cb.equal(favorite.get("exercise").get("id"), root.get("id"))
			));
			return cb.exists(subquery);
		};
	}

	private static Specification<Exercise> nameContains(String q) {
		if (!StringUtils.hasText(q)) {
			return null;
		}
		String pattern = "%" + q.trim().toLowerCase() + "%";
		return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern);
	}

	private static Specification<Exercise> categoryEquals(String category) {
		if (!StringUtils.hasText(category)) {
			return null;
		}
		return (root, query, cb) -> cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase());
	}

	private static Specification<Exercise> equipmentMatches(String equipment) {
		if (!StringUtils.hasText(equipment)) {
			return null;
		}
		String value = equipment.trim().toLowerCase();
		return (root, query, cb) -> {
			Join<Exercise, Equipment> join = root.join("equipment", JoinType.LEFT);
			return cb.or(
					cb.equal(cb.lower(join.get("id")), value),
					cb.equal(cb.lower(join.get("name")), value)
			);
		};
	}

	private static Specification<Exercise> muscleMatches(String muscle) {
		if (!StringUtils.hasText(muscle)) {
			return null;
		}
		String value = muscle.trim().toLowerCase();
		return (root, query, cb) -> {
			var subquery = query.subquery(String.class);
			var ehm = subquery.from(ExerciseHasMuscle.class);
			subquery.select(ehm.get("exercise").get("id"));
			Predicate musclePred = cb.or(
					cb.equal(cb.lower(ehm.get("muscle").get("id")), value),
					cb.equal(cb.lower(ehm.get("muscle").get("name")), value)
			);
			subquery.where(cb.and(cb.equal(ehm.get("exercise").get("id"), root.get("id")), musclePred));
			return cb.exists(subquery);
		};
	}
}
