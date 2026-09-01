package com.example.movielist.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** OMDb's full detail response (from `i=` lookup by imdbID) — the raw external
 *  shape, deliberately kept separate from the internal Movie entity and the
 *  outward-facing MovieResponse DTO (see mapper.MovieMapper for the translation). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OmdbMovieResponse(
		@JsonProperty("imdbID") String imdbId,
		@JsonProperty("Title") String title,
		@JsonProperty("Year") String year,
		@JsonProperty("Poster") String poster,
		@JsonProperty("Plot") String plot,
		@JsonProperty("Genre") String genre,
		@JsonProperty("Director") String director,
		@JsonProperty("imdbRating") String imdbRating,
		@JsonProperty("Response") String response,
		@JsonProperty("Error") String error
) {

	public boolean isFound() {
		return "True".equalsIgnoreCase(response);
	}
}
