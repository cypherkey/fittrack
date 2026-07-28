package com.fittrack.web.dto;

import com.fittrack.domain.RpeLevel;

public record TemplateSetResponse(
		String id,
		String exerciseId,
		String exerciseName,
		int setNumber,
		Integer reps,
		Double weightKg,
		Integer durationSeconds,
		Double distanceMeters,
		RpeLevel rpe,
		String notes
) {
}
