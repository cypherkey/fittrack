package com.fittrack.web.dto;

import jakarta.validation.constraints.NotNull;

public record TrackedParametersRequest(
		@NotNull Integer trackedParameters
) {
}