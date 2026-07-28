package com.fittrack.web.dto;

import com.fittrack.domain.RpeLevel;
import com.fittrack.domain.TemplateVisibility;
import com.fittrack.domain.WorkoutDifficulty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record TemplateRequest(
		String name,
		Integer durationSeconds,
		WorkoutDifficulty difficulty,
		String notes,
		@NotNull TemplateVisibility visibility,
		@Valid List<TemplateSetRequest> sets
) {
}
