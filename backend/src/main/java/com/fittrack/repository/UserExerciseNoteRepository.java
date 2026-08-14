package com.fittrack.repository;

import com.fittrack.domain.UserExerciseNote;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserExerciseNoteRepository extends JpaRepository<UserExerciseNote, String> {

	Optional<UserExerciseNote> findByUser_IdAndExercise_Id(String userId, String exerciseId);

	List<UserExerciseNote> findByUser_IdAndExercise_IdIn(String userId, Collection<String> exerciseIds);
}