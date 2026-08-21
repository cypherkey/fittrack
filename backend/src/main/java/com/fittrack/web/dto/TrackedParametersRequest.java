package com.fittrack.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/**
 * Admin-only patch for tracked parameters; optional {@code videoUrl} (explicit null clears).
 */
public class TrackedParametersRequest {

	@NotNull
	private Integer trackedParameters;

	private String videoUrl;

	private boolean videoUrlPresent;

	public Integer getTrackedParameters() {
		return trackedParameters;
	}

	public void setTrackedParameters(Integer trackedParameters) {
		this.trackedParameters = trackedParameters;
	}

	public String getVideoUrl() {
		return videoUrl;
	}

	@JsonProperty("videoUrl")
	public void setVideoUrl(String videoUrl) {
		this.videoUrl = videoUrl;
		this.videoUrlPresent = true;
	}

	@JsonIgnore
	public boolean isVideoUrlPresent() {
		return videoUrlPresent;
	}
}