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

	@EntityGraph(attributePaths = { "user" })
	@Query("""
			SELECT w FROM Workout w
			WHERE w.user.id = :userId
			  AND (:from IS NULL OR w.startedAt >= :from)
			  AND (:to IS NULL OR w.startedAt <= :to)
			  AND (:exerciseId IS NULL OR EXISTS (
			        SELECT 1 FROM WorkoutSet s
			        WHERE s.workout = w AND s.exercise.id = :exerciseId
			      ))
			ORDER BY CASE WHEN w.startedAt IS NULL THEN 1 ELSE 0 END, w.startedAt DESC
			""")
	List<Workout> findByUserIdAndStartedAtRange(
			@Param("userId") String userId,
			@Param("from") Instant from,
			@Param("to") Instant to,
			@Param("exerciseId") String exerciseId
	);

	@EntityGraph(attributePaths = { "user" })
	@Query("""
			SELECT w FROM Workout w
			WHERE (:from IS NULL OR w.startedAt >= :from)
			  AND (:to IS NULL OR w.startedAt <= :to)
			  AND (:exerciseId IS NULL OR EXISTS (
			        SELECT 1 FROM WorkoutSet s
			        WHERE s.workout = w AND s.exercise.id = :exerciseId
			      ))
			ORDER BY CASE WHEN w.startedAt IS NULL THEN 1 ELSE 0 END, w.startedAt DESC
			""")
	List<Workout> findAllByStartedAtRange(
			@Param("from") Instant from,
			@Param("to") Instant to,
			@Param("exerciseId") String exerciseId
	);

	@EntityGraph(attributePaths = { "sets", "sets.exercise", "sourceTemplate", "user" })
	@Query("SELECT w FROM Workout w WHERE w.id = :id")
	Optional<Workout> findWithSetsById(@Param("id") String id);

	boolean existsByUser_IdAndName(String userId, String name);

	boolean existsByUser_IdAndNameAndIdNot(String userId, String name, String id);
}
