package com.fittrack.service;

import com.fittrack.domain.User;
import com.fittrack.repository.UserRepository;
import com.fittrack.web.dto.CreateUserRequest;
import com.fittrack.web.dto.UpdateUserRequest;
import com.fittrack.web.dto.UserResponse;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserAdminService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserAdminService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional(readOnly = true)
	public List<UserResponse> list() {
		return userRepository.findAll().stream()
				.sorted(Comparator.comparing(User::getDisplayName, Comparator.nullsLast(String::compareToIgnoreCase)))
				.map(UserAdminService::toResponse)
				.toList();
	}

	@Transactional
	public UserResponse create(CreateUserRequest request) {
		String username = request.username().trim();
		if (userRepository.existsByUsername(username)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
		}
		String email = blankToNull(request.email());
		if (email != null && userRepository.existsByEmail(email)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
		}

		User user = new User();
		user.setUsername(username);
		user.setPasswordHash(passwordEncoder.encode(request.password()));
		user.setDisplayName(request.displayName().trim());
		user.setEmail(email);
		user.setAdmin(Boolean.TRUE.equals(request.admin()));
		return toResponse(userRepository.save(user));
	}

	@Transactional
	public UserResponse update(String id, UpdateUserRequest request) {
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

		if (request.displayName() != null && !request.displayName().isBlank()) {
			user.setDisplayName(request.displayName().trim());
		}
		if (request.email() != null) {
			String email = blankToNull(request.email());
			if (email != null) {
				userRepository.findAll().stream()
						.filter(u -> email.equalsIgnoreCase(u.getEmail()) && !u.getId().equals(id))
						.findAny()
						.ifPresent(u -> {
							throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
						});
			}
			user.setEmail(email);
		}
		if (request.password() != null && !request.password().isBlank()) {
			if (user.getUsername() == null) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot set password on Google-only user without username");
			}
			user.setPasswordHash(passwordEncoder.encode(request.password()));
		}
		if (request.admin() != null) {
			if (!request.admin() && user.isAdmin() && userRepository.countByAdminTrue() <= 1) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove the last admin");
			}
			user.setAdmin(request.admin());
		}
		return toResponse(userRepository.save(user));
	}

	@Transactional
	public void delete(String id, User actor) {
		if (actor.getId().equals(id)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete your own account");
		}
		User user = userRepository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
		if (user.isAdmin() && userRepository.countByAdminTrue() <= 1) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot delete the last admin");
		}
		userRepository.delete(user);
	}

	public static UserResponse toResponse(User user) {
		return new UserResponse(
				user.getId(),
				user.getUsername(),
				user.getEmail(),
				user.getDisplayName(),
				user.getAvatarUrl(),
				user.isAdmin()
		);
	}

	private static String blankToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
