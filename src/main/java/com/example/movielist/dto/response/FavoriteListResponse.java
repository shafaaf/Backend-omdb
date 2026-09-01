package com.example.movielist.dto.response;

import java.time.Instant;

/** Summary of one favorite list — used both for the list-of-lists view and one list's own detail. */
public record FavoriteListResponse(
		Long id,
		String name,
		long itemCount,
		Instant createdAt
) {
}
