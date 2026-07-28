package com.fittrack.repository;

import com.fittrack.domain.ExerciseHasMuscle;
import com.fittrack.domain.ExerciseHasMuscle.ExerciseHasMuscleId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExerciseHasMuscleRepository extends JpaRepository<ExerciseHasMuscle, ExerciseHasMuscleId> {

	List<ExerciseHasMuscle> findByExercise_Id(String exerciseId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("DELETE FROM ExerciseHasMuscle ehm WHERE ehm.exercise.id = :exerciseId")
	void deleteByExerciseId(@Param("exerciseId") String exerciseId);
}
