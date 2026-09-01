package com.example.movielist.exception;

/** Mapped to 401 by GlobalExceptionHandler. Message is always generic — see AuthServiceImpl. */
public class InvalidCredentialsException extends RuntimeException {

	public InvalidCredentialsException(String message) {
		super(message);
	}
}
