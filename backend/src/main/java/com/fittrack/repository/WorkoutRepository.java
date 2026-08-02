package com.fittrack.repository;

import com.fittrack.domain.Workout;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WorkoutRepository extends JpaRepository<Workout, String> {

	@Query("""
			SELECT w FROM Workout w
			WHERE w.user.id = :userId
			  AND (:from IS NULL OR w.startedAt >= :from)
			  AND (:to IS NULL OR w.startedAt <= :to)
			ORDER BY CASE WHEN w.startedAt IS NULL THEN 1 ELSE 0 END, w.startedAt DESC
			""")
	List<Workout> findByUserIdAndStartedAtRange(
			@Param("userId") String userId,
			@Param("from") Instant from,
			@Param("to") Instant to
	);

	@EntityGraph(attributePaths = { "sets", "sets.exercise", "sourceTemplate" })
	@Query("SELECT w FROM Workout w WHERE w.id = :id")
	Optional<Workout> findWithSetsById(@Param("id") String id);
}
