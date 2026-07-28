package com.fittrack.web.dto;

import jakarta.validation.constraints.NotBlank;

public record MuscleLinkRequest(
		@NotBlank String muscleId,
		boolean primary
) {
}
