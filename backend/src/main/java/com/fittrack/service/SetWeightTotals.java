package com.fittrack.service;

import java.util.Collection;

public final class SetWeightTotals {

	private SetWeightTotals() {
	}

	public static Double compute(Collection<? extends WeightedSet> sets) {
		double total = 0;
		boolean any = false;
		for (WeightedSet set : sets) {
			if (set.reps() != null && set.weightKg() != null) {
				total += set.reps() * set.weightKg();
				any = true;
			}
		}
		return any ? total : null;
	}

	public interface WeightedSet {
		Integer reps();

		Double weightKg();
	}
}
