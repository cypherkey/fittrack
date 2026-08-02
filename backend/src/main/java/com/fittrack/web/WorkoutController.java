package com.fittrack.web;

import com.fittrack.domain.User;
import com.fittrack.service.WorkoutService;
import com.fittrack.web.dto.WorkoutRequest;
import com.fittrack.web.dto.WorkoutResponse;
import com.fittrack.web.dto.ReorderSetsRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/workouts")
@Tag(name = "Workouts", description = "Logged workouts and set reorder")
public class WorkoutController {

	private final WorkoutService workoutService;
	private final CurrentUserResolver currentUserResolver;

	public WorkoutController(WorkoutService workoutService, CurrentUserResolver currentUserResolver) {
		this.workoutService = workoutService;
		this.currentUserResolver = currentUserResolver;
	}

	@GetMapping
	public List<WorkoutResponse> list(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) Instant from,
			@RequestParam(required = false) Instant to
	) {
		User user = currentUserResolver.requireUser(jwt);
		return workoutService.list(user, from, to);
	}

	@GetMapping("/{id}")
	public WorkoutResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
		return workoutService.get(currentUserResolver.requireUser(jwt), id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public WorkoutResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody WorkoutRequest request) {
		return workoutService.create(currentUserResolver.requireUser(jwt), request);
	}

	@PutMapping("/{id}")
	public WorkoutResponse update(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable String id,
			@Valid @RequestBody WorkoutRequest request
	) {
		return workoutService.update(currentUserResolver.requireUser(jwt), id, request);
	}

	@PatchMapping("/{id}/sets/reorder")
	public WorkoutResponse reorderSets(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable String id,
			@Valid @RequestBody ReorderSetsRequest request
	) {
		return workoutService.reorderSets(currentUserResolver.requireUser(jwt), id, request.items());
	}

	@PostMapping("/{id}/start")
	public WorkoutResponse start(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
		return workoutService.start(currentUserResolver.requireUser(jwt), id);
	}

	@PostMapping("/{id}/complete")
	public WorkoutResponse complete(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
		return workoutService.complete(currentUserResolver.requireUser(jwt), id);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
		workoutService.delete(currentUserResolver.requireUser(jwt), id);
	}
}
