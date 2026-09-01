package com.example.movielist.security;

import java.time.Instant;

/** Result of issuing an access JWT: the compact token string plus the pieces the
 *  caller needs without re-parsing it (its own jti, its own expiry). */
public record AccessToken(String value, String jti, Instant expiresAt) {
}
