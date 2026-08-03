package com.fittrack.web;

import com.fittrack.domain.User;
import com.fittrack.service.ExerciseService;
import com.fittrack.web.dto.ExerciseHistoryEntryResponse;
import com.fittrack.web.dto.ExerciseRequest;
import com.fittrack.web.dto.ExerciseResponse;
import com.fittrack.web.dto.PageResponse;
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
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/exercise")
@Tag(name = "Exercises", description = "Catalog and custom exercises")
public class ExerciseController {

	private final ExerciseService exerciseService;
	private final CurrentUserResolver currentUserResolver;

	public ExerciseController(ExerciseService exerciseService, CurrentUserResolver currentUserResolver) {
		this.exerciseService = exerciseService;
		this.currentUserResolver = currentUserResolver;
	}

	@GetMapping
	public PageResponse<ExerciseResponse> list(
			@AuthenticationPrincipal Jwt jwt,
			@RequestParam(required = false) String q,
			@RequestParam(required = false) String muscle,
			@RequestParam(required = false) String equipment,
			@RequestParam(required = false) String category,
			@RequestParam(defaultValue = "false") boolean customOnly,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "50") int size
	) {
		User user = currentUserResolver.requireUser(jwt);
		return exerciseService.list(user, q, muscle, equipment, category, customOnly, page, size);
	}

	@GetMapping("/{id}")
	public ExerciseResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
		return exerciseService.get(currentUserResolver.requireUser(jwt), id);
	}

	@GetMapping("/{id}/history")
	public List<ExerciseHistoryEntryResponse> history(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
		return exerciseService.history(currentUserResolver.requireUser(jwt), id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ExerciseResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ExerciseRequest request) {
		return exerciseService.create(currentUserResolver.requireUser(jwt), request);
	}

	@PutMapping("/{id}")
	public ExerciseResponse update(
			@AuthenticationPrincipal Jwt jwt,
			@PathVariable String id,
			@Valid @RequestBody ExerciseRequest request
	) {
		return exerciseService.update(currentUserResolver.requireUser(jwt), id, request);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
		exerciseService.delete(currentUserResolver.requireUser(jwt), id);
	}
}
