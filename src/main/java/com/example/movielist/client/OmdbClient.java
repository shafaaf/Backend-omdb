package com.example.movielist.client;

import java.util.List;
import java.util.Optional;

/** Thin wrapper around the OMDb HTTP API — no caching or business logic, just the raw calls. */
public interface OmdbClient {

	/** Searches OMDb by free-text title; returns an empty list if nothing matches. */
	List<OmdbSearchItem> search(String searchTerm);

	/** Looks up one movie's full detail by its IMDb id; empty if OMDb has no such movie. */
	Optional<OmdbMovieResponse> fetchByImdbId(String imdbId);
}
