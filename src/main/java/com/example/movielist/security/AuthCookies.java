package com.example.movielist.security;

import java.time.Duration;
import java.time.Instant;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Builds the httpOnly cookies that carry the access/refresh tokens. Centralized
 * here so AuthController (writes them) and JwtAuthenticationFilter (reads the
 * access one) agree on names, and so the cookie attributes — the actual security
 * decisions — live in exactly one place.
 *
 * secure(true) requires HTTPS in general, but browsers special-case "localhost" as
 * a secure context, so local dev over plain http still works without weakening
 * this for anywhere else.
 */
@Component
public class AuthCookies {

	public static final String ACCESS_TOKEN = "access_token";
	public static final String REFRESH_TOKEN = "refresh_token";

	/** Builds the access-token cookie, expiring at the token's own expiry. */
	public ResponseCookie access(String value, Instant expiresAt) {
		return build(ACCESS_TOKEN, value, Duration.between(Instant.now(), expiresAt));
	}

	/** Builds the refresh-token cookie, expiring at the token's own expiry. */
	public ResponseCookie refresh(String value, Instant expiresAt) {
		return build(REFRESH_TOKEN, value, Duration.between(Instant.now(), expiresAt));
	}

	/** An empty, immediately-expiring cookie that clears the access token on logout. */
	public ResponseCookie clearAccess() {
		return build(ACCESS_TOKEN, "", Duration.ZERO);
	}

	/** An empty, immediately-expiring cookie that clears the refresh token on logout. */
	public ResponseCookie clearRefresh() {
		return build(REFRESH_TOKEN, "", Duration.ZERO);
	}

	/** Shared cookie-attribute builder used by all four methods above. */
	private ResponseCookie build(String name, String value, Duration maxAge) {
		return ResponseCookie.from(name, value)
				.httpOnly(true)
				.secure(true)
				.sameSite("Lax")
				.path("/")
				.maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge)
				.build();
	}
}
