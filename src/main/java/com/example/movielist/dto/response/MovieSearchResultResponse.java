package com.example.movielist.dto.response;

/**
 * One row in a search-results list — just enough to show a poster and title.
 * {@code imdbRating} is only populated when the movie is already cached locally
 * (OMDb's search endpoint doesn't return a rating; only its full-detail lookup
 * does), so it's null on a genuine cache miss rather than triggering an extra
 * OMDb call per search row.
 */
public record MovieSearchResultResponse(
		String externalId,
		String title,
		Integer releaseYear,
		String posterUrl,
		String imdbRating
) {
}
