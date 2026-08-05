package com.fittrack.web.dto;

import com.fittrack.domain.WorkoutDifficulty;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;

public record WorkoutRequest(
		Instant startedAt,
		Instant endedAt,
		String name,
		Boolean completed,
		Boolean useMetric,
		WorkoutDifficulty difficulty,
		String notes,
		String sourceTemplateId,
		List<@Valid WorkoutSetRequest> sets
) {
}
