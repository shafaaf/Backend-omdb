package com.example.movielist.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Bound from movielist.jwt.* in application.yml. Deliberately has no defaults on
 * `secret` — a blank/missing JWT_SECRET fails Spring Boot startup immediately via
 * JSR-303 validation rather than letting the app boot into a silently-insecure state.
 */
@ConfigurationProperties(prefix = "movielist.jwt")
@Validated
public record JwtProperties(

		@NotBlank(message = "movielist.jwt.secret (env JWT_SECRET) must be set")
		String secret,

		@Positive
		long accessTokenTtlSeconds,

		@Positive
		long refreshTokenTtlSeconds
) {
}
