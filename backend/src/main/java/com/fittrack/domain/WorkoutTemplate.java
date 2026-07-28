package com.fittrack.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "template")
public class WorkoutTemplate {

	@Id
	@Column(length = 36, nullable = false)
	private String id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	private String name;

	@Column(name = "duration_seconds")
	private Integer durationSeconds;

	@Column(name = "total_weight_lifted")
	private Double totalWeightLifted;

	@Enumerated(EnumType.STRING)
	private WorkoutDifficulty difficulty;

	private String notes;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TemplateVisibility visibility = TemplateVisibility.PRIVATE;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("setNumber ASC")
	private List<TemplateSet> sets = new ArrayList<>();

	@PrePersist
	void onCreate() {
		if (id == null) {
			id = UUID.randomUUID().toString();
		}
		Instant now = Instant.now();
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getDurationSeconds() {
		return durationSeconds;
	}

	public void setDurationSeconds(Integer durationSeconds) {
		this.durationSeconds = durationSeconds;
	}

	public Double getTotalWeightLifted() {
		return totalWeightLifted;
	}

	public void setTotalWeightLifted(Double totalWeightLifted) {
		this.totalWeightLifted = totalWeightLifted;
	}

	public WorkoutDifficulty getDifficulty() {
		return difficulty;
	}

	public void setDifficulty(WorkoutDifficulty difficulty) {
		this.difficulty = difficulty;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public TemplateVisibility getVisibility() {
		return visibility;
	}

	public void setVisibility(TemplateVisibility visibility) {
		this.visibility = visibility;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public List<TemplateSet> getSets() {
		return sets;
	}

	public void setSets(List<TemplateSet> sets) {
		this.sets = sets;
	}
}