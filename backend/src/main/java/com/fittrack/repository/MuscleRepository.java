package com.fittrack.repository;

import com.fittrack.domain.Muscle;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MuscleRepository extends JpaRepository<Muscle, String> {

	Optional<Muscle> findByNameIgnoreCase(String name);
}
