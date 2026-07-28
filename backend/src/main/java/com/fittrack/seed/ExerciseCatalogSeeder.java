package com.fittrack.seed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fittrack.domain.Equipment;
import com.fittrack.domain.Exercise;
import com.fittrack.domain.ExerciseHasImage;
import com.fittrack.domain.ExerciseHasMuscle;
import com.fittrack.domain.ExerciseLevel;
import com.fittrack.domain.ExerciseMechanic;
import com.fittrack.domain.Image;
import com.fittrack.domain.Muscle;
import com.fittrack.domain.TrackedParameters;
import com.fittrack.repository.EquipmentRepository;
import com.fittrack.repository.ExerciseHasImageRepository;
import com.fittrack.repository.ExerciseHasMuscleRepository;
import com.fittrack.repository.ExerciseRepository;
import com.fittrack.repository.ImageRepository;
import com.fittrack.repository.MuscleRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@Order(200)
public class ExerciseCatalogSeeder implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(ExerciseCatalogSeeder.class);
	private static final String RESOURCE = "data/exercises.json";

	private final ObjectMapper objectMapper;
	private final EquipmentRepository equipmentRepository;
	private final MuscleRepository muscleRepository;
	private final ImageRepository imageRepository;
	private final ExerciseRepository exerciseRepository;
	private final ExerciseHasMuscleRepository exerciseHasMuscleRepository;
	private final ExerciseHasImageRepository exerciseHasImageRepository;

	public ExerciseCatalogSeeder(
			ObjectMapper objectMapper,
			EquipmentRepository equipmentRepository,
			MuscleRepository muscleRepository,
			ImageRepository imageRepository,
			ExerciseRepository exerciseRepository,
			ExerciseHasMuscleRepository exerciseHasMuscleRepository,
			ExerciseHasImageRepository exerciseHasImageRepository
	) {
		this.objectMapper = objectMapper;
		this.equipmentRepository = equipmentRepository;
		this.muscleRepository = muscleRepository;
		this.imageRepository = imageRepository;
		this.exerciseRepository = exerciseRepository;
		this.exerciseHasMuscleRepository = exerciseHasMuscleRepository;
		this.exerciseHasImageRepository = exerciseHasImageRepository;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) throws Exception {
		ClassPathResource resource = new ClassPathResource(RESOURCE);
		if (!resource.exists()) {
			log.warn("Exercise catalog resource {} not found; skipping seed", RESOURCE);
			return;
		}
		List<SeedExercise> seeds;
		try (InputStream in = resource.getInputStream()) {
			seeds = objectMapper.readValue(in, new TypeReference<>() {
			});
		}
		if (seeds == null || seeds.isEmpty()) {
			log.info("Exercise catalog is empty; nothing to seed");
			return;
		}

		Map<String, Equipment> equipmentByName = new HashMap<>();
		equipmentRepository.findAll().forEach(e -> equipmentByName.put(e.getName().toLowerCase(Locale.ROOT), e));
		Map<String, Muscle> muscleByName = new HashMap<>();
		muscleRepository.findAll().forEach(m -> muscleByName.put(m.getName().toLowerCase(Locale.ROOT), m));
		Map<String, Image> imageByPath = new HashMap<>();
		imageRepository.findAll().forEach(i -> imageByPath.put(i.getPath(), i));

		int upserted = 0;
		for (SeedExercise seed : seeds) {
			if (!StringUtils.hasText(seed.id()) || !StringUtils.hasText(seed.name())) {
				continue;
			}
			Equipment equipment = null;
			if (StringUtils.hasText(seed.equipment())) {
				equipment = equipmentByName.computeIfAbsent(seed.equipment().toLowerCase(Locale.ROOT), key -> {
					Equipment e = new Equipment();
					e.setName(seed.equipment());
					return equipmentRepository.save(e);
				});
			}

			Exercise exercise = exerciseRepository.findById(seed.id()).orElseGet(Exercise::new);
			exercise.setId(seed.id());
			exercise.setName(seed.name());
			exercise.setForce(seed.force());
			exercise.setLevel(mapLevel(seed.level()));
			exercise.setMechanic(mapMechanic(seed.mechanic()));
			exercise.setEquipment(equipment);
			exercise.setInstructions(toMarkdown(seed.instructions()));
			exercise.setCategory(seed.category());
			exercise.setTrackedParameters(trackedForCategory(seed.category()));
			exercise.setCustom(false);
			exercise.setAddedBy(null);
			exerciseRepository.save(exercise);

			exerciseHasMuscleRepository.deleteByExerciseId(exercise.getId());
			java.util.Set<String> linkedMuscles = new java.util.HashSet<>();
			linkMuscles(exercise, seed.primaryMuscles(), true, muscleByName, linkedMuscles);
			linkMuscles(exercise, seed.secondaryMuscles(), false, muscleByName, linkedMuscles);

			exerciseHasImageRepository.deleteByExerciseId(exercise.getId());
			if (seed.images() != null) {
				int order = 0;
				for (String path : seed.images()) {
					if (!StringUtils.hasText(path)) {
						continue;
					}
					Image image = imageByPath.computeIfAbsent(path, p -> {
						Image img = new Image();
						img.setPath(p);
						return imageRepository.save(img);
					});
					ExerciseHasImage link = new ExerciseHasImage();
					link.setExercise(exercise);
					link.setImage(image);
					link.setSortOrder(order++);
					exerciseHasImageRepository.save(link);
				}
			}
			upserted++;
		}
		log.info("Seeded/updated {} catalog exercises from {}", upserted, RESOURCE);
	}

	private void linkMuscles(
			Exercise exercise,
			List<String> names,
			boolean primary,
			Map<String, Muscle> muscleByName,
			java.util.Set<String> linkedMuscles
	) {
		if (names == null) {
			return;
		}
		for (String name : names) {
			if (!StringUtils.hasText(name)) {
				continue;
			}
			Muscle muscle = muscleByName.computeIfAbsent(name.toLowerCase(Locale.ROOT), key -> {
				Muscle m = new Muscle();
				m.setName(name);
				return muscleRepository.save(m);
			});
			if (!linkedMuscles.add(muscle.getId())) {
				continue;
			}
			ExerciseHasMuscle link = new ExerciseHasMuscle();
			link.setExercise(exercise);
			link.setMuscle(muscle);
			link.setPrimary(primary);
			exerciseHasMuscleRepository.save(link);
		}
	}

	static ExerciseLevel mapLevel(String level) {
		if (!StringUtils.hasText(level)) {
			return ExerciseLevel.BEGINNER;
		}
		return switch (level.trim().toLowerCase(Locale.ROOT)) {
			case "intermediate" -> ExerciseLevel.INTERMEDIATE;
			case "expert" -> ExerciseLevel.EXPERT;
			default -> ExerciseLevel.BEGINNER;
		};
	}

	static ExerciseMechanic mapMechanic(String mechanic) {
		if (!StringUtils.hasText(mechanic)) {
			return null;
		}
		return switch (mechanic.trim().toLowerCase(Locale.ROOT)) {
			case "compound" -> ExerciseMechanic.COMPOUND;
			case "isolation" -> ExerciseMechanic.ISOLATION;
			default -> null;
		};
	}

	static String toMarkdown(List<String> instructions) {
		if (instructions == null || instructions.isEmpty()) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < instructions.size(); i++) {
			sb.append(i + 1).append(". ").append(instructions.get(i));
			if (i < instructions.size() - 1) {
				sb.append('\n');
			}
		}
		return sb.toString();
	}

	static int trackedForCategory(String category) {
		if (!StringUtils.hasText(category)) {
			return TrackedParameters.REPS | TrackedParameters.WEIGHT;
		}
		return switch (category.trim().toLowerCase(Locale.ROOT)) {
			case "cardio" -> TrackedParameters.DURATION | TrackedParameters.DISTANCE;
			case "stretching", "plyometrics" -> TrackedParameters.DURATION | TrackedParameters.REPS;
			default -> TrackedParameters.REPS | TrackedParameters.WEIGHT;
		};
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record SeedExercise(
			String id,
			String name,
			String force,
			String level,
			String mechanic,
			String equipment,
			List<String> primaryMuscles,
			List<String> secondaryMuscles,
			List<String> instructions,
			String category,
			List<String> images
	) {
	}
}
