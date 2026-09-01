package com.example.movielist.dto.response;

import lombok.Builder;

/** Full movie details sent to the client, e.g. for a movie's detail page. */
@Builder
public record MovieResponse(
		Long id,
		String externalId,
		String title,
		Integer releaseYear,
		String posterUrl,
		String plot,
		String genre,
		String director,
		String imdbRating
) {
}
