package com.fittrack.repository;

import com.fittrack.domain.Image;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImageRepository extends JpaRepository<Image, String> {

	Optional<Image> findByPath(String path);
}
