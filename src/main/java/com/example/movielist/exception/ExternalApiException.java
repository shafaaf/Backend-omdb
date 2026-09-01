package com.example.movielist.exception;

/** Mapped to 502 by GlobalExceptionHandler — the upstream (OMDb) failed or was
 *  unreachable; this is not the client's fault. */
public class ExternalApiException extends RuntimeException {

	public ExternalApiException(String message) {
		super(message);
	}

	public ExternalApiException(String message, Throwable cause) {
		super(message, cause);
	}
}
