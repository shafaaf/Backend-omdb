package com.example.movielist.exception;

/** Mapped to 404 by GlobalExceptionHandler. */
public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String message) {
		super(message);
	}
}
