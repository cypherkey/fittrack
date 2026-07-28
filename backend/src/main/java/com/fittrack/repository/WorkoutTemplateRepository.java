package com.fittrack.repository;

import com.fittrack.domain.TemplateVisibility;
import com.fittrack.domain.WorkoutTemplate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkoutTemplateRepository extends JpaRepository<WorkoutTemplate, String> {

	List<WorkoutTemplate> findByUser_IdOrderByUpdatedAtDesc(String userId);

	List<WorkoutTemplate> findByVisibilityOrderByUpdatedAtDesc(TemplateVisibility visibility);

	@EntityGraph(attributePaths = { "sets", "sets.exercise" })
	@Query("SELECT t FROM WorkoutTemplate t WHERE t.id = :id")
	Optional<WorkoutTemplate> findWithSetsById(@Param("id") String id);
}
