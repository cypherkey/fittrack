package com.fittrack.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fittrack.domain.RpeLevel;

/**
 * Partial update for a workout set. Omitted fields are left unchanged.
 * Explicit JSON null clears nullable fields (metrics / rpe).
 */
public class WorkoutSetPatchRequest {

	private Boolean completed;
	private Integer reps;
	private Double weightKg;
	private Integer durationSeconds;
	private Double distanceMeters;
	private RpeLevel rpe;

	private boolean completedPresent;
	private boolean repsPresent;
	private boolean weightKgPresent;
	private boolean durationSecondsPresent;
	private boolean distanceMetersPresent;
	private boolean rpePresent;

	public Boolean getCompleted() {
		return completed;
	}

	@JsonProperty("completed")
	public void setCompleted(Boolean completed) {
		this.completed = completed;
		this.completedPresent = true;
	}

	public Integer getReps() {
		return reps;
	}

	@JsonProperty("reps")
	public void setReps(Integer reps) {
		this.reps = reps;
		this.repsPresent = true;
	}

	public Double getWeightKg() {
		return weightKg;
	}

	@JsonProperty("weightKg")
	public void setWeightKg(Double weightKg) {
		this.weightKg = weightKg;
		this.weightKgPresent = true;
	}

	public Integer getDurationSeconds() {
		return durationSeconds;
	}

	@JsonProperty("durationSeconds")
	public void setDurationSeconds(Integer durationSeconds) {
		this.durationSeconds = durationSeconds;
		this.durationSecondsPresent = true;
	}

	public Double getDistanceMeters() {
		return distanceMeters;
	}

	@JsonProperty("distanceMeters")
	public void setDistanceMeters(Double distanceMeters) {
		this.distanceMeters = distanceMeters;
		this.distanceMetersPresent = true;
	}

	public RpeLevel getRpe() {
		return rpe;
	}

	@JsonProperty("rpe")
	public void setRpe(RpeLevel rpe) {
		this.rpe = rpe;
		this.rpePresent = true;
	}

	@JsonIgnore
	public boolean isCompletedPresent() {
		return completedPresent;
	}

	@JsonIgnore
	public boolean isRepsPresent() {
		return repsPresent;
	}

	@JsonIgnore
	public boolean isWeightKgPresent() {
		return weightKgPresent;
	}

	@JsonIgnore
	public boolean isDurationSecondsPresent() {
		return durationSecondsPresent;
	}

	@JsonIgnore
	public boolean isDistanceMetersPresent() {
		return distanceMetersPresent;
	}

	@JsonIgnore
	public boolean isRpePresent() {
		return rpePresent;
	}
}
