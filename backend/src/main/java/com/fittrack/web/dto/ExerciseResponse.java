package com.fittrack.web.dto;

import com.fittrack.domain.ExerciseLevel;
import com.fittrack.domain.ExerciseMechanic;
import java.util.List;

public record ExerciseResponse(
		String id,
		String name,
		String force,
		ExerciseLevel level,
		ExerciseMechanic mechanic,
		String equipmentId,
		String equipmentName,
		String instructions,
		String videoUrl,
		String category,
		int trackedParameters,
		boolean custom,
		String addedById,
		List<ExerciseMuscleResponse> muscles,
		List<ExerciseImageResponse> images
) {
}
