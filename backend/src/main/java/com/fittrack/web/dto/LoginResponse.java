package com.fittrack.web.dto;

public record LoginResponse(
		String token,
		UserResponse user
) {
}
