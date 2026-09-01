package com.example.movielist.dto.response;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;

/** The one JSON error shape the whole API returns, e.g. `{"status":404,"message":"..."}`. */
public record ErrorResponse(
		Instant timestamp,
		int status,
		String error,
		String message,
		String path,
		Map<String, String> fieldErrors
) {

	/** Builds an error response with no per-field detail. */
	public static ErrorResponse of(HttpStatus status, String message, String path) {
		return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, path, null);
	}

	/** Builds an error response carrying per-field validation errors. */
	public static ErrorResponse ofValidation(
			HttpStatus status, String message, String path, Map<String, String> fieldErrors) {
		return new ErrorResponse(Instant.now(), status.value(), status.getReasonPhrase(), message, path, fieldErrors);
	}
}
