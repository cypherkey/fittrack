package com.fittrack.repository;

import com.fittrack.domain.ExerciseHasImage;
import com.fittrack.domain.ExerciseHasImage.ExerciseHasImageId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExerciseHasImageRepository extends JpaRepository<ExerciseHasImage, ExerciseHasImageId> {

	List<ExerciseHasImage> findByExercise_IdOrderBySortOrderAsc(String exerciseId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("DELETE FROM ExerciseHasImage ehi WHERE ehi.exercise.id = :exerciseId")
	void deleteByExerciseId(@Param("exerciseId") String exerciseId);
}
