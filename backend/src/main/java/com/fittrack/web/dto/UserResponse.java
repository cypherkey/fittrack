package com.fittrack.web.dto;

public record UserResponse(
		String id,
		String username,
		String email,
		String displayName,
		String avatarUrl,
		boolean admin
) {
}
