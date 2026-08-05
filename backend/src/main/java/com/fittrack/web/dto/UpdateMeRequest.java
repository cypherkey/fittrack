package com.fittrack.web.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateMeRequest(
		@NotNull Boolean useMetric
) {
}