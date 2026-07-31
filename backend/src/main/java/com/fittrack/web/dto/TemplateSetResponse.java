package com.fittrack.web.dto;

public record TemplateSetResponse(
		String id,
		String exerciseId,
		String exerciseName,
		int setNumber,
		Integer reps,
		Double weightKg,
		Integer durationSeconds,
		Double distanceMeters,
		String notes
) {
}