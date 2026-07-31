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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
	private static final String IMAGE_CLASSPATH_PREFIX = "data/exercise-images/";
	private static final String IMAGE_DOWNLOAD_BASE =
			"https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/";

	private final ObjectMapper objectMapper;
	private final EquipmentRepository equipmentRepository;
	private final MuscleRepository muscleRepository;
	private final ImageRepository imageRepository;
	private final ExerciseRepository exerciseRepository;
	private final ExerciseHasMuscleRepository exerciseHasMuscleRepository;
	private final ExerciseHasImageRepository exerciseHasImageRepository;
	private final boolean loadImages;
	private final boolean downloadImages;
	private final HttpClient httpClient = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(10))
			.build();

	public ExerciseCatalogSeeder(
			ObjectMapper objectMapper,
			EquipmentRepository equipmentRepository,
			MuscleRepository muscleRepository,
			ImageRepository imageRepository,
			ExerciseRepository exerciseRepository,
			ExerciseHasMuscleRepository exerciseHasMuscleRepository,
			ExerciseHasImageRepository exerciseHasImageRepository,
			@Value("${fittrack.seed.load-images:true}") boolean loadImages,
			@Value("${fittrack.seed.download-images:true}") boolean downloadImages
	) {
		this.objectMapper = objectMapper;
		this.equipmentRepository = equipmentRepository;
		this.muscleRepository = muscleRepository;
		this.imageRepository = imageRepository;
		this.exerciseRepository = exerciseRepository;
		this.exerciseHasMuscleRepository = exerciseHasMuscleRepository;
		this.exerciseHasImageRepository = exerciseHasImageRepository;
		this.loadImages = loadImages;
		this.downloadImages = downloadImages;
	}

	@Override
	@Transactional
	public void run(ApplicationArguments args) throws Exception {
		if (exerciseRepository.count() > 0) {
			log.info("Exercise table already has data; skipping catalog seed");
			return;
		}
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
		int imagesLoaded = 0;
		int imagesMissing = 0;
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
			exercise.setTrackedParameters(
					seed.trackedParameters() != null
							? seed.trackedParameters()
							: trackedForCategory(seed.category()));
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
						img.setAltText(seed.name());
						if (loadImages) {
							LoadedImage loaded = loadImageBytes(p);
							if (loaded != null) {
								img.setContentBase64(Base64.getEncoder().encodeToString(loaded.bytes()));
								img.setContentType(loaded.contentType());
							}
						}
						return imageRepository.save(img);
					});
					if (image.getContentBase64() != null) {
						imagesLoaded++;
					}
					else if (loadImages) {
						imagesMissing++;
					}
					ExerciseHasImage link = new ExerciseHasImage();
					link.setExercise(exercise);
					link.setImage(image);
					link.setSortOrder(order++);
					exerciseHasImageRepository.save(link);
				}
			}
			upserted++;
		}
		log.info(
				"Seeded/updated {} catalog exercises from {} (images loaded={}, missing={})",
				upserted,
				RESOURCE,
				imagesLoaded,
				imagesMissing
		);
	}

	private LoadedImage loadImageBytes(String relativePath) {
		ClassPathResource local = new ClassPathResource(IMAGE_CLASSPATH_PREFIX + relativePath);
		if (local.exists()) {
			try (InputStream in = local.getInputStream()) {
				return new LoadedImage(in.readAllBytes(), contentTypeFor(relativePath));
			}
			catch (IOException e) {
				log.warn("Failed to read classpath image {}: {}", relativePath, e.getMessage());
			}
		}
		if (!downloadImages) {
			return null;
		}
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(IMAGE_DOWNLOAD_BASE + relativePath))
					.timeout(Duration.ofSeconds(30))
					.GET()
					.build();
			HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
			if (response.statusCode() >= 200 && response.statusCode() < 300 && response.body() != null && response.body().length > 0) {
				String contentType = response.headers().firstValue("content-type").orElse(contentTypeFor(relativePath));
				return new LoadedImage(response.body(), contentType);
			}
			log.warn("Image download for {} returned HTTP {}", relativePath, response.statusCode());
		}
		catch (Exception e) {
			log.warn("Failed to download image {}: {}", relativePath, e.getMessage());
		}
		return null;
	}

	private static String contentTypeFor(String path) {
		String lower = path.toLowerCase(Locale.ROOT);
		if (lower.endsWith(".png")) {
			return "image/png";
		}
		if (lower.endsWith(".webp")) {
			return "image/webp";
		}
		if (lower.endsWith(".gif")) {
			return "image/gif";
		}
		return "image/jpeg";
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

	/** Maps a Ryot exercise {@code lot} string to FitTrack bitmask, or {@code null} if unknown. */
	static Integer trackedFromRyotLot(String lot) {
		if (!StringUtils.hasText(lot)) {
			return null;
		}
		return switch (lot.trim().toLowerCase(Locale.ROOT)) {
			case "reps_and_weight" -> TrackedParameters.REPS | TrackedParameters.WEIGHT;
			case "duration" -> TrackedParameters.DURATION;
			case "distance_and_duration" -> TrackedParameters.DURATION | TrackedParameters.DISTANCE;
			case "reps" -> TrackedParameters.REPS;
			default -> null;
		};
	}

	private record LoadedImage(byte[] bytes, String contentType) {
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
			List<String> images,
			Integer trackedParameters
	) {
	}
}
