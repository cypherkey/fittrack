package com.fittrack.web.dto;

import com.fittrack.domain.RpeLevel;

public record WorkoutSetResponse(
		String id,
		String exerciseId,
		String exerciseName,
		int trackedParameters,
		int setNumber,
		Integer reps,
		Double weightKg,
		Integer durationSeconds,
		Double distanceMeters,
		boolean completed,
		RpeLevel rpe,
		String notes
) {
}
