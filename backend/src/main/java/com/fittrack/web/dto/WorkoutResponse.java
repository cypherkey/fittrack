package com.fittrack.web.dto;

import com.fittrack.domain.WorkoutDifficulty;
import java.time.Instant;
import java.util.List;

public record WorkoutResponse(
		String id,
		String userId,
		Instant performedAt,
		String name,
		Integer durationSeconds,
		Double totalWeightLifted,
		WorkoutDifficulty difficulty,
		String notes,
		String sourceTemplateId,
		Instant createdAt,
		Instant updatedAt,
		List<WorkoutSetResponse> sets
) {
}
