package com.example.movielist.exception;

/**
 * Thrown when an Idempotency-Key is reused for a request whose logically
 * significant fields don't match the original request that key was first used
 * for — e.g. the same key sent once for "add movie A" and again for "add movie
 * B". That's a client bug worth surfacing loudly (409) rather than silently
 * replaying the wrong stored response. See
 * service.impl.FavoriteListItemServiceImpl for where this is thrown.
 */
public class IdempotencyConflictException extends RuntimeException {

	public IdempotencyConflictException(String message) {
		super(message);
	}
}
