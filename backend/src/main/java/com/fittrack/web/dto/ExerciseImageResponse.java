package com.fittrack.web.dto;

public record ExerciseImageResponse(
		String imageId,
		String path,
		String altText,
		int sortOrder,
		String contentType,
		String contentBase64
) {
}
