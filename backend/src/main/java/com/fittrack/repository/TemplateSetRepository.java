package com.fittrack.repository;

import com.fittrack.domain.TemplateSet;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TemplateSetRepository extends JpaRepository<TemplateSet, String> {

	List<TemplateSet> findByTemplate_IdOrderBySetNumberAsc(String templateId);

	boolean existsByExercise_Id(String exerciseId);
}
