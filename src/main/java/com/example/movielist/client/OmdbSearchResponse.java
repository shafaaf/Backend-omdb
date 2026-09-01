package com.example.movielist.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** OMDb's `s=` search endpoint response envelope — a list of results plus a status flag. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OmdbSearchResponse(
		@JsonProperty("Search") List<OmdbSearchItem> search,
		@JsonProperty("Response") String response,
		@JsonProperty("Error") String error
) {

	/** OMDb returns Response="False" (with an Error message) for "no results" — not an HTTP error. */
	public boolean isSuccess() {
		return "True".equalsIgnoreCase(response);
	}
}
