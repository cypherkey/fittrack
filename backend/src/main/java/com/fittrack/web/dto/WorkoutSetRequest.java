package com.fittrack.web.dto;

import com.fittrack.domain.RpeLevel;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WorkoutSetRequest(
		@NotBlank String exerciseId,
		@NotNull @Min(1) Integer setNumber,
		Integer reps,
		Double weightKg,
		Integer durationSeconds,
		Double distanceMeters,
		Boolean completed,
		RpeLevel rpe,
		String notes
) {
}
