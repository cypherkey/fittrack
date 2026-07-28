package com.fittrack.web.dto;

import com.fittrack.domain.ExerciseLevel;
import com.fittrack.domain.ExerciseMechanic;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ExerciseRequest(
		@NotBlank String name,
		String force,
		@NotNull ExerciseLevel level,
		ExerciseMechanic mechanic,
		String equipmentId,
		String instructions,
		String category,
		Integer trackedParameters,
		@Valid List<MuscleLinkRequest> muscles
) {
}
