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
@Table(name = "appuser_favorite_exercise")
public class UserFavoriteExercise {

	@EmbeddedId
	private UserFavoriteExerciseId id = new UserFavoriteExerciseId();

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("userId")
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@MapsId("exerciseId")
	@JoinColumn(name = "exercise_id", nullable = false)
	private Exercise exercise;

	public UserFavoriteExerciseId getId() {
		return id;
	}

	public void setId(UserFavoriteExerciseId id) {
		this.id = id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Exercise getExercise() {
		return exercise;
	}

	public void setExercise(Exercise exercise) {
		this.exercise = exercise;
	}

	@Embeddable
	public static class UserFavoriteExerciseId implements Serializable {

		@Column(name = "user_id")
		private String userId;

		@Column(name = "exercise_id")
		private String exerciseId;

		public UserFavoriteExerciseId() {
		}

		public UserFavoriteExerciseId(String userId, String exerciseId) {
			this.userId = userId;
			this.exerciseId = exerciseId;
		}

		public String getUserId() {
			return userId;
		}

		public void setUserId(String userId) {
			this.userId = userId;
		}

		public String getExerciseId() {
			return exerciseId;
		}

		public void setExerciseId(String exerciseId) {
			this.exerciseId = exerciseId;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof UserFavoriteExerciseId that)) {
				return false;
			}
			return Objects.equals(userId, that.userId) && Objects.equals(exerciseId, that.exerciseId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(userId, exerciseId);
		}
	}
}