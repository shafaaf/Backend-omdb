package com.example.movielist.exception;

/** Mapped to 409 by GlobalExceptionHandler. */
public class DuplicateResourceException extends RuntimeException {

	public DuplicateResourceException(String message) {
		super(message);
	}
}
