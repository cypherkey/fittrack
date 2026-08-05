package com.fittrack.web.dto;

import com.fittrack.domain.WorkoutDifficulty;
import java.time.Instant;
import java.util.List;

public record WorkoutResponse(
		String id,
		String userId,
		Instant startedAt,
		Instant endedAt,
		String name,
		boolean completed,
		boolean useMetric,
		Double totalWeightLifted,
		WorkoutDifficulty difficulty,
		String notes,
		String sourceTemplateId,
		Instant createdAt,
		Instant updatedAt,
		int setCount,
		List<WorkoutSetResponse> sets
) {
}
