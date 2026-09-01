package com.example.movielist.dto.response;

import java.time.Instant;

/** One movie's entry in a favorite list, as returned to the client. */
public record FavoriteListItemResponse(
		Long id,
		MovieResponse movie,
		Instant addedAt
) {
}
