package com.fittrack.web.dto;

import com.fittrack.domain.RpeLevel;
import java.time.Instant;

public record ExerciseHistoryEntryResponse(
		Instant startedAt,
		int setNumber,
		Integer reps,
		Double weightKg,
		RpeLevel rpe
) {
}