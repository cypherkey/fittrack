package com.fittrack.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.fittrack.domain.ExerciseLevel;
import com.fittrack.domain.ExerciseMechanic;
import com.fittrack.domain.TrackedParameters;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExerciseCatalogSeederTest {

	@Test
	void mapsLevelAndMechanic() {
		assertThat(ExerciseCatalogSeeder.mapLevel("beginner")).isEqualTo(ExerciseLevel.BEGINNER);
		assertThat(ExerciseCatalogSeeder.mapLevel("INTERMEDIATE")).isEqualTo(ExerciseLevel.INTERMEDIATE);
		assertThat(ExerciseCatalogSeeder.mapMechanic("compound")).isEqualTo(ExerciseMechanic.COMPOUND);
		assertThat(ExerciseCatalogSeeder.mapMechanic(null)).isNull();
	}

	@Test
	void instructionsBecomeMarkdownList() {
		assertThat(ExerciseCatalogSeeder.toMarkdown(List.of("Step one", "Step two")))
				.isEqualTo("1. Step one\n2. Step two");
	}

	@Test
	void trackedParametersFollowCategoryHeuristic() {
		assertThat(ExerciseCatalogSeeder.trackedForCategory("cardio"))
				.isEqualTo(TrackedParameters.DURATION | TrackedParameters.DISTANCE);
		assertThat(ExerciseCatalogSeeder.trackedForCategory("stretching"))
				.isEqualTo(TrackedParameters.DURATION | TrackedParameters.REPS);
		assertThat(ExerciseCatalogSeeder.trackedForCategory("strength"))
				.isEqualTo(TrackedParameters.REPS | TrackedParameters.WEIGHT);
	}
}
