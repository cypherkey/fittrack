package com.fittrack.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
		@NotBlank @Size(max = 100) String username,
		@NotBlank @Size(min = 4, max = 200) String password,
		@NotBlank @Size(max = 200) String displayName,
		@Size(max = 320) String email,
		Boolean admin
) {
}
