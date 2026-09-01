package com.example.movielist.exception;

import com.example.movielist.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * The single place every exception in this app funnels through to become the
 * one JSON error shape (dto.response.ErrorResponse) the API ever returns —
 * mirrored by SecurityConfig's authenticationEntryPoint/accessDeniedHandler and
 * CsrfHeaderFilter for the failures that happen before a controller is even
 * reached. Handlers are ordered roughly by how "expected" the failure is: client
 * mistakes (validation, duplicates) first, infrastructure/unexpected last.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/** Bean Validation failure on a @RequestBody (e.g. a blank required field) → 400 with per-field messages. */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		for (FieldError error : ex.getBindingResult().getFieldErrors()) {
			fieldErrors.put(error.getField(), error.getDefaultMessage());
		}
		return build(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
	}

	/** Bean Validation failure on a @RequestParam/@PathVariable (e.g. @Validated on a controller) → 400. */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
		Map<String, String> fieldErrors = new LinkedHashMap<>();
		ex.getConstraintViolations().forEach(violation ->
				fieldErrors.put(violation.getPropertyPath().toString(), violation.getMessage()));
		return build(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
	}

	/** Required query param missing (e.g. ?title= on movie search) → 400. */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request, null);
	}

	/** Requested resource doesn't exist, or exists but isn't owned by the caller → 404. */
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage(), request, null);
	}

	/** E.g. signup with an already-registered email, or a duplicate list name → 409. */
	@ExceptionHandler(DuplicateResourceException.class)
	public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
		return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
	}

	/** Idempotency-Key reused for a logically different request → 409. */
	@ExceptionHandler(IdempotencyConflictException.class)
	public ResponseEntity<ErrorResponse> handleIdempotencyConflict(IdempotencyConflictException ex, HttpServletRequest request) {
		return build(HttpStatus.CONFLICT, ex.getMessage(), request, null);
	}

	/**
	 * Backstop for the race described in FavoriteListItemServiceImpl: two
	 * concurrent requests can both pass an application-level "does this exist"
	 * check before either commits. The DB unique constraint is what actually
	 * prevents the duplicate row; the loser's insert surfaces here as this
	 * exception rather than as an unhandled 500.
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
		log.warn("data integrity violation on {}: {}", request.getRequestURI(), ex.getMessage());
		return build(HttpStatus.CONFLICT, "The request conflicts with existing data", request, null);
	}

	/** Bad login, or an invalid/expired/revoked refresh token → 401. */
	@ExceptionHandler(InvalidCredentialsException.class)
	public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
		return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request, null);
	}

	/** OMDb was unreachable, errored, or rejected the request → 502 (not the caller's fault). */
	@ExceptionHandler(ExternalApiException.class)
	public ResponseEntity<ErrorResponse> handleExternalApi(ExternalApiException ex, HttpServletRequest request) {
		log.error("external API failure on {}", request.getRequestURI(), ex);
		return build(HttpStatus.BAD_GATEWAY, ex.getMessage(), request, null);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
		// Full stack trace goes to the server log only — the client gets a generic
		// message so internal details (class names, SQL, file paths) never leak.
		log.error("unhandled exception on {}", request.getRequestURI(), ex);
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, null);
	}

	/** Wraps a status/message (and optional field errors) into the response entity every handler returns. */
	private ResponseEntity<ErrorResponse> build(
			HttpStatus status, String message, HttpServletRequest request, Map<String, String> fieldErrors) {
		ErrorResponse body = fieldErrors == null
				? ErrorResponse.of(status, message, request.getRequestURI())
				: ErrorResponse.ofValidation(status, message, request.getRequestURI(), fieldErrors);
		return ResponseEntity.status(status).body(body);
	}
}
