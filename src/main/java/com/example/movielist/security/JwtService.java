package com.example.movielist.security;

import com.example.movielist.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/**
 * Hand-rolled JWT signing/parsing via the jjwt library, used instead of
 * spring-boot-starter-oauth2-resource-server specifically so this logic stays
 * visible and readable rather than hidden behind framework auto-configuration —
 * the whole point of this project is to show the mechanism, not just use it.
 *
 * A Spring @Service bean is a container-managed singleton by default scope: one
 * instance per ApplicationContext, shared by every caller. Contrast with
 * util.OmdbRateLimiterSingleton, a classic GoF enum singleton built entirely
 * outside the container.
 */
@Service
public class JwtService {

	private final SecretKey signingKey;
	private final JwtProperties properties;

	/** Derives the HMAC signing key from the configured JWT secret. */
	public JwtService(JwtProperties properties) {
		this.properties = properties;
		this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
	}

	/** Signs and returns a new short-lived access token for the given user. */
	public AccessToken generateAccessToken(Long userId, String email) {
		Instant now = Instant.now();
		Instant expiresAt = now.plusSeconds(properties.accessTokenTtlSeconds());
		String jti = UUID.randomUUID().toString();

		String token = Jwts.builder()
				.subject(userId.toString())
				.claim("email", email)
				.id(jti)
				.issuedAt(Date.from(now))
				.expiration(Date.from(expiresAt))
				.signWith(signingKey)
				.compact();

		return new AccessToken(token, jti, expiresAt);
	}

	/** Returns empty if the token is malformed, expired, or has a bad signature. */
	public Optional<Claims> tryParse(String token) {
		try {
			return Optional.of(Jwts.parser()
					.verifyWith(signingKey)
					.build()
					.parseSignedClaims(token)
					.getPayload());
		} catch (JwtException | IllegalArgumentException e) {
			return Optional.empty();
		}
	}

	/** Reads the user id back out of a token's subject claim. */
	public Long extractUserId(Claims claims) {
		return Long.valueOf(claims.getSubject());
	}
}
