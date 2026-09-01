package com.example.movielist.service;

import com.example.movielist.dto.response.UserResponse;
import java.time.Instant;

/**
 * Internal carrier between AuthServiceImpl and AuthController — deliberately not
 * a dto/response type. It holds the raw access/refresh token values, which the
 * controller must turn into httpOnly cookies and never let leak into a JSON
 * response body. Keeping this out of the dto package makes that boundary
 * structural rather than just a convention someone has to remember.
 */
public record AuthResult(
		UserResponse user,
		String accessToken,
		Instant accessTokenExpiresAt,
		String refreshToken,
		Instant refreshTokenExpiresAt
) {
}
