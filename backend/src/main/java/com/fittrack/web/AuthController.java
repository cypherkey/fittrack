package com.fittrack.web;

import com.fittrack.domain.User;
import com.fittrack.security.AppUserDetails;
import com.fittrack.security.JwtService;
import com.fittrack.web.dto.LoginRequest;
import com.fittrack.web.dto.LoginResponse;
import com.fittrack.web.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.fittrack.security.AppUserDetailsService;

@RestController
@RequestMapping("/api/v1")
public class AuthController {

	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final AppUserDetailsService userDetailsService;

	public AuthController(
			AuthenticationManager authenticationManager,
			JwtService jwtService,
			AppUserDetailsService userDetailsService
	) {
		this.authenticationManager = authenticationManager;
		this.jwtService = jwtService;
		this.userDetailsService = userDetailsService;
	}

	@PostMapping("/auth/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(request.username(), request.password())
		);
		AppUserDetails principal = (AppUserDetails) authentication.getPrincipal();
		String token = jwtService.createToken(principal);
		return new LoginResponse(token, toResponse(principal.getUser()));
	}

	@GetMapping("/me")
	public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
		if (jwt == null || jwt.getSubject() == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		}
		AppUserDetails details = userDetailsService.loadByUserId(jwt.getSubject());
		return toResponse(details.getUser());
	}

	private static UserResponse toResponse(User user) {
		return new UserResponse(
				user.getId(),
				user.getUsername(),
				user.getEmail(),
				user.getDisplayName(),
				user.getAvatarUrl()
		);
	}
}
