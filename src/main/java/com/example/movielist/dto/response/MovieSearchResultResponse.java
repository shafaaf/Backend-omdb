package com.example.movielist.dto.response;

/** One row in a search-results list — just enough to show a poster and title. */
public record MovieSearchResultResponse(
		String externalId,
		String title,
		Integer releaseYear,
		String posterUrl
) {
}
