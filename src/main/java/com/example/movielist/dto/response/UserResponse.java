package com.example.movielist.dto.response;

/** Never includes passwordHash — the mapper controls exactly what leaves the service layer. */
public record UserResponse(
		Long id,
		String email,
		String displayName
) {
}
