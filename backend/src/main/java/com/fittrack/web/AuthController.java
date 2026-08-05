package com.fittrack.web;

import com.fittrack.domain.User;
import com.fittrack.repository.UserRepository;
import com.fittrack.security.AppUserDetails;
import com.fittrack.security.AppUserDetailsService;
import com.fittrack.security.JwtService;
import com.fittrack.service.UserAdminService;
import com.fittrack.web.dto.LoginRequest;
import com.fittrack.web.dto.LoginResponse;
import com.fittrack.web.dto.UpdateMeRequest;
import com.fittrack.web.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Auth", description = "Login and current user")
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final AppUserDetailsService userDetailsService;
	private final UserRepository userRepository;

	public AuthController(
			AuthenticationManager authenticationManager,
			JwtService jwtService,
			AppUserDetailsService userDetailsService,
			UserRepository userRepository
	) {
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
		this.userDetailsService = userDetailsService;
		this.userRepository = userRepository;
	}

	@PostMapping("/auth/login")
	@SecurityRequirements
	@Operation(summary = "Local username/password login", description = "Returns a JWT for Authorization: Bearer <token>")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.username(), request.password())
		);
		AppUserDetails principal = (AppUserDetails) authentication.getPrincipal();
		String token = jwtService.createToken(principal);
		return new LoginResponse(token, UserAdminService.toResponse(principal.getUser()));
	}

	@GetMapping("/me")
	@Operation(summary = "Current authenticated user")
	public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
		return UserAdminService.toResponse(requireCurrentUser(jwt));
	}

	@PatchMapping("/me")
	@Transactional
	@Operation(summary = "Update current user preferences")
	public UserResponse updateMe(
			@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody UpdateMeRequest request
	) {
		User user = requireCurrentUser(jwt);
		user.setUseMetric(request.useMetric());
		userRepository.save(user);
		return UserAdminService.toResponse(user);
	}

	private User requireCurrentUser(Jwt jwt) {
		if (jwt == null || jwt.getSubject() == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		}
		return userDetailsService.loadByUserId(jwt.getSubject()).getUser();
	}
}