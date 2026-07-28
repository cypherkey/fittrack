package com.fittrack.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "exercise_has_image")
public class ExerciseHasImage {

	@EmbeddedId
	private ExerciseHasImageId id = new ExerciseHasImageId();

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("exerciseId")
	@JoinColumn(name = "exercise_id")
	private Exercise exercise;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("imageId")
	@JoinColumn(name = "image_id")
	private Image image;

	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	public ExerciseHasImageId getId() {
		return id;
	}

	public void setId(ExerciseHasImageId id) {
		this.id = id;
	}

	public Exercise getExercise() {
		return exercise;
	}

	public void setExercise(Exercise exercise) {
		this.exercise = exercise;
		if (exercise != null) {
			this.id.setExerciseId(exercise.getId());
		}
	}

	public Image getImage() {
		return image;
	}

	public void setImage(Image image) {
		this.image = image;
		if (image != null) {
			this.id.setImageId(image.getId());
		}
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}

	@Embeddable
	public static class ExerciseHasImageId implements Serializable {

		@Column(name = "exercise_id")
		private String exerciseId;

		@Column(name = "image_id")
		private String imageId;

		public String getExerciseId() {
			return exerciseId;
		}

		public void setExerciseId(String exerciseId) {
			this.exerciseId = exerciseId;
		}

		public String getImageId() {
			return imageId;
		}

		public void setImageId(String imageId) {
			this.imageId = imageId;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof ExerciseHasImageId that)) {
				return false;
			}
			return Objects.equals(exerciseId, that.exerciseId) && Objects.equals(imageId, that.imageId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(exerciseId, imageId);
		}
	}
}
