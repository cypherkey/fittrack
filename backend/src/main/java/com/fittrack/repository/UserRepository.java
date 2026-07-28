package com.fittrack.repository;

import com.fittrack.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {

	Optional<User> findByUsername(String username);

	Optional<User> findByGoogleSubject(String googleSubject);

	boolean existsByUsername(String username);

	boolean existsByEmail(String email);

	long countByAdminTrue();
}
