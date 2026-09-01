package com.example.movielist.dto.response;

import java.time.Instant;
import lombok.Builder;

/**
 * Sent back after login/signup. No tokens in here — those go out as httpOnly
 * cookies instead (see AuthController), never visible to JavaScript.
 */
@Builder
public record AuthResponse(
		UserResponse user,
		Instant accessTokenExpiresAt
) {
}
