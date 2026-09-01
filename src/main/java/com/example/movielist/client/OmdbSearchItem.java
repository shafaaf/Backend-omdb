package com.example.movielist.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** One entry from OMDb's `s=` search endpoint — a lighter shape than the full
 *  detail response (OmdbMovieResponse): no plot/genre/director/rating. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OmdbSearchItem(
		@JsonProperty("imdbID") String imdbId,
		@JsonProperty("Title") String title,
		@JsonProperty("Year") String year,
		@JsonProperty("Poster") String poster
) {
}
