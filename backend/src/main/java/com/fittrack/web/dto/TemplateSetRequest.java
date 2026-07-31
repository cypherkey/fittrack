package com.fittrack.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TemplateSetRequest(
		@NotBlank String exerciseId,
		@NotNull @Min(1) Integer setNumber,
		Integer reps,
		Double weightKg,
		Integer durationSeconds,
		Double distanceMeters,
		String notes
) {
}