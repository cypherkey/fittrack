package com.fittrack.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "exercise")
public class Exercise {

	@Id
	@Column(nullable = false)
	private String id;

	@Column(nullable = false)
	private String name;

	private String force;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ExerciseLevel level;

	@Enumerated(EnumType.STRING)
	private ExerciseMechanic mechanic;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "equipment_id")
	private Equipment equipment;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String instructions = "";

	@Column(name = "video_url", columnDefinition = "TEXT")
	private String videoUrl;

	private String category;

	@Column(name = "tracked_parameters", nullable = false)
	private int trackedParameters;

	@Column(name = "is_custom", nullable = false)
	private boolean custom;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "added_by")
	private User addedBy;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getForce() {
		return force;
	}

	public void setForce(String force) {
		this.force = force;
	}

	public ExerciseLevel getLevel() {
		return level;
	}

	public void setLevel(ExerciseLevel level) {
		this.level = level;
	}

	public ExerciseMechanic getMechanic() {
		return mechanic;
	}

	public void setMechanic(ExerciseMechanic mechanic) {
		this.mechanic = mechanic;
	}

	public Equipment getEquipment() {
		return equipment;
	}

	public void setEquipment(Equipment equipment) {
		this.equipment = equipment;
	}

	public String getInstructions() {
		return instructions;
	}

	public void setInstructions(String instructions) {
		this.instructions = instructions;
	}

	public String getVideoUrl() {
		return videoUrl;
	}

	public void setVideoUrl(String videoUrl) {
		this.videoUrl = videoUrl;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public int getTrackedParameters() {
		return trackedParameters;
	}

	public void setTrackedParameters(int trackedParameters) {
		this.trackedParameters = trackedParameters;
	}

	public boolean isCustom() {
		return custom;
	}

	public void setCustom(boolean custom) {
		this.custom = custom;
	}

	public User getAddedBy() {
		return addedBy;
	}

	public void setAddedBy(User addedBy) {
		this.addedBy = addedBy;
	}
}
