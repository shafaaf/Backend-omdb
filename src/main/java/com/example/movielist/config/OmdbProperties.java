package com.example.movielist.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Bound from omdb.api.* in application.yml. `key` has no default — a missing
 * OMDB_API_KEY fails startup immediately (see JwtProperties for the same pattern).
 */
@ConfigurationProperties(prefix = "omdb.api")
@Validated
public record OmdbProperties(

		@NotBlank(message = "omdb.api.key (env OMDB_API_KEY) must be set")
		String key,

		@NotBlank
		String baseUrl
) {
}
