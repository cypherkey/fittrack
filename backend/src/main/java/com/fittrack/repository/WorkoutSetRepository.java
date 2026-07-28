package com.fittrack.repository;

import com.fittrack.domain.WorkoutSet;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkoutSetRepository extends JpaRepository<WorkoutSet, String> {

	List<WorkoutSet> findByWorkout_IdOrderBySetNumberAsc(String workoutId);

	boolean existsByExercise_Id(String exerciseId);
}
