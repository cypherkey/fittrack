package com.fittrack.web;

import com.fittrack.domain.User;
import com.fittrack.service.UserAdminService;
import com.fittrack.web.dto.CreateUserRequest;
import com.fittrack.web.dto.UpdateUserRequest;
import com.fittrack.web.dto.UserResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Admin user management (ROLE_ADMIN)")
public class UserController {

	private final UserAdminService userAdminService;
	private final CurrentUserResolver currentUserResolver;

	public UserController(UserAdminService userAdminService, CurrentUserResolver currentUserResolver) {
		this.userAdminService = userAdminService;
		this.currentUserResolver = currentUserResolver;
	}

	@GetMapping
	public List<UserResponse> list(@AuthenticationPrincipal Jwt jwt) {
		currentUserResolver.requireUser(jwt);
		return userAdminService.list();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public UserResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateUserRequest request) {
		currentUserResolver.requireUser(jwt);
		return userAdminService.create(request);
	}

	@PutMapping("/{id}")
	public UserResponse update(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable String id,
			@Valid @RequestBody UpdateUserRequest request
	) {
		currentUserResolver.requireUser(jwt);
		return userAdminService.update(id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
		User actor = currentUserResolver.requireUser(jwt);
		userAdminService.delete(id, actor);
	}
}
