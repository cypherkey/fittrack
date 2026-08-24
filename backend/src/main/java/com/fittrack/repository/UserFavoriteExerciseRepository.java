package com.fittrack.repository;

import com.fittrack.domain.UserFavoriteExercise;
import com.fittrack.domain.UserFavoriteExercise.UserFavoriteExerciseId;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFavoriteExerciseRepository extends JpaRepository<UserFavoriteExercise, UserFavoriteExerciseId> {

	boolean existsByUser_IdAndExercise_Id(String userId, String exerciseId);

	List<UserFavoriteExercise> findByUser_IdAndExercise_IdIn(String userId, Collection<String> exerciseIds);

	void deleteByUser_IdAndExercise_Id(String userId, String exerciseId);

	void deleteByExercise_Id(String exerciseId);
}