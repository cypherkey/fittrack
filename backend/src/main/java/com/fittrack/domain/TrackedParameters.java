package com.fittrack.domain;

/**
 * Bit flags for exercise tracked parameters.
 */
public final class TrackedParameters {

	public static final int REPS = 1;
	public static final int WEIGHT = 2;
	public static final int DURATION = 4;
	public static final int DISTANCE = 8;

	private TrackedParameters() {
	}

	public static boolean has(int flags, int flag) {
		return (flags & flag) != 0;
	}
}
