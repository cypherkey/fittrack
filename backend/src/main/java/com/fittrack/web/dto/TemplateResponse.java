package com.fittrack.web.dto;

import com.fittrack.domain.TemplateVisibility;
import com.fittrack.domain.WorkoutDifficulty;
import java.time.Instant;
import java.util.List;

public record TemplateResponse(
		String id,
		String userId,
		String name,
		WorkoutDifficulty difficulty,
		String notes,
		TemplateVisibility visibility,
		Instant createdAt,
		Instant updatedAt,
		List<TemplateSetResponse> sets
) {
}
