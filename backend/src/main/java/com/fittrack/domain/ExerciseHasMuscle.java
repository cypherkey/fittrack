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
@Table(name = "exercise_has_muscle")
public class ExerciseHasMuscle {

	@EmbeddedId
	private ExerciseHasMuscleId id = new ExerciseHasMuscleId();

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("exerciseId")
	@JoinColumn(name = "exercise_id")
	private Exercise exercise;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("muscleId")
	@JoinColumn(name = "muscle_id")
	private Muscle muscle;

	@Column(name = "is_primary", nullable = false)
	private boolean primary;

	public ExerciseHasMuscleId getId() {
		return id;
	}

	public void setId(ExerciseHasMuscleId id) {
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

	public Muscle getMuscle() {
		return muscle;
	}

	public void setMuscle(Muscle muscle) {
		this.muscle = muscle;
		if (muscle != null) {
			this.id.setMuscleId(muscle.getId());
		}
	}

	public boolean isPrimary() {
		return primary;
	}

	public void setPrimary(boolean primary) {
		this.primary = primary;
	}

	@Embeddable
	public static class ExerciseHasMuscleId implements Serializable {

		@Column(name = "exercise_id")
		private String exerciseId;

		@Column(name = "muscle_id")
		private String muscleId;

		public String getExerciseId() {
			return exerciseId;
		}

		public void setExerciseId(String exerciseId) {
			this.exerciseId = exerciseId;
		}

		public String getMuscleId() {
			return muscleId;
		}

		public void setMuscleId(String muscleId) {
			this.muscleId = muscleId;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof ExerciseHasMuscleId that)) {
				return false;
			}
			return Objects.equals(exerciseId, that.exerciseId) && Objects.equals(muscleId, that.muscleId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(exerciseId, muscleId);
		}
	}
}
