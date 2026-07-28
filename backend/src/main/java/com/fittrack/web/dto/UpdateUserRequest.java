package com.fittrack.web.dto;

import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
		@Size(max = 200) String displayName,
		@Size(max = 320) String email,
		@Size(min = 4, max = 200) String password,
		Boolean admin
) {
}
