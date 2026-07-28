package com.fittrack.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReorderSetItem(
		@NotBlank String setId,
		@NotNull Integer setNumber
) {
}
