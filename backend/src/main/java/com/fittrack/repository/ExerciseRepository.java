package com.fittrack.repository;

import com.fittrack.domain.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ExerciseRepository extends JpaRepository<Exercise, String>, JpaSpecificationExecutor<Exercise> {
}
