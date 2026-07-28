package com.fittrack.web;

import com.fittrack.web.dto.ErrorResponse;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(this::formatFieldError)
				.collect(Collectors.joining("; "));
		if (message.isBlank()) {
			message = "Validation failed";
		}
		return ResponseEntity.badRequest().body(new ErrorResponse(message, "VALIDATION_ERROR"));
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ErrorResponse> handleStatus(ResponseStatusException ex) {
		HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
		if (status == null) {
			status = HttpStatus.INTERNAL_SERVER_ERROR;
		}
		String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
		return ResponseEntity.status(status).body(new ErrorResponse(message, status.name()));
	}

	private String formatFieldError(FieldError error) {
		return error.getField() + ": " + error.getDefaultMessage();
	}
}
