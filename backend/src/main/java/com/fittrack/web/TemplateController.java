package com.fittrack.web;

import com.fittrack.domain.TemplateVisibility;
import com.fittrack.domain.User;
import com.fittrack.service.TemplateService;
import com.fittrack.web.dto.CloneTemplateRequest;
import com.fittrack.web.dto.TemplateRequest;
import com.fittrack.web.dto.TemplateResponse;
import com.fittrack.web.dto.WorkoutResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/templates")
public class TemplateController {

	private final TemplateService templateService;
	private final CurrentUserResolver currentUserResolver;

	public TemplateController(TemplateService templateService, CurrentUserResolver currentUserResolver) {
		this.templateService = templateService;
		this.currentUserResolver = currentUserResolver;
	}

	@GetMapping
	public List<TemplateResponse> list(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) TemplateVisibility visibility
	) {
		User user = currentUserResolver.requireUser(jwt);
		return templateService.list(user, visibility);
	}

	@GetMapping("/{id}")
	public TemplateResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
		return templateService.get(currentUserResolver.requireUser(jwt), id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TemplateResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TemplateRequest request) {
		return templateService.create(currentUserResolver.requireUser(jwt), request);
	}

	@PutMapping("/{id}")
	public TemplateResponse update(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable String id,
			@Valid @RequestBody TemplateRequest request
	) {
		return templateService.update(currentUserResolver.requireUser(jwt), id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
		templateService.delete(currentUserResolver.requireUser(jwt), id);
	}

	@PostMapping("/{id}/clone")
	@ResponseStatus(HttpStatus.CREATED)
	public WorkoutResponse clone(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable String id,
			@Valid @RequestBody CloneTemplateRequest request
	) {
		return templateService.cloneToWorkout(currentUserResolver.requireUser(jwt), id, request);
	}
}
