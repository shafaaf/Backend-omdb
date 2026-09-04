package com.example.movielist.dto.response;

/**
 * One row in a search-results list — just enough to show a poster, title, and
 * rating. imdbRating is null unless this movie is already cached locally: OMDb's
 * search endpoint (s=) doesn't return ratings at all, only its per-title detail
 * endpoint does, so this is genuinely "if available", not a bug.
 */
public record MovieSearchResultResponse(
		String externalId,
		String title,
		Integer releaseYear,
		String posterUrl,
		String imdbRating
) {
}
