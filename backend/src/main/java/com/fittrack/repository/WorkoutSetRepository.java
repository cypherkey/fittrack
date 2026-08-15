package com.fittrack.repository;

import com.fittrack.domain.WorkoutSet;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkoutSetRepository extends JpaRepository<WorkoutSet, String> {

	List<WorkoutSet> findByWorkout_IdOrderBySetNumberAsc(String workoutId);

	boolean existsByExercise_Id(String exerciseId);

	@Query("""
			SELECT ws FROM WorkoutSet ws
			JOIN FETCH ws.workout w
			WHERE ws.exercise.id = :exerciseId AND w.user.id = :userId
			  AND w.completed = true AND ws.completed = true
			""")
	List<WorkoutSet> findByExerciseIdAndUserId(
			@Param("exerciseId") String exerciseId,
			@Param("userId") String userId
	);
}
