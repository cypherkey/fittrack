package com.fittrack.web.dto;

import com.fittrack.domain.RpeLevel;
import com.fittrack.domain.WorkoutDifficulty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

public record WorkoutRequest(
		@NotNull Instant performedAt,
		String name,
		Integer durationSeconds,
		WorkoutDifficulty difficulty,
		String notes,
		String sourceTemplateId,
		List<@Valid WorkoutSetRequest> sets
) {
}
