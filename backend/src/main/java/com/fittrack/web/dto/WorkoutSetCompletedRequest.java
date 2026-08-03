package com.fittrack.web.dto;

import jakarta.validation.constraints.NotNull;

public record WorkoutSetCompletedRequest(
		@NotNull Boolean completed
) {
}