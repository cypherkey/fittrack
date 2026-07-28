package com.fittrack.web.dto;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record CloneTemplateRequest(
		@NotNull Instant performedAt,
		String name
) {
}
