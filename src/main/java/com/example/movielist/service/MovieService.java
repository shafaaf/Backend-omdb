package com.example.movielist.service;

import com.example.movielist.dto.response.MovieResponse;
import com.example.movielist.dto.response.MovieSearchResultResponse;
import com.example.movielist.entity.Movie;
import java.util.List;

/** Movie search and cache-or-fetch lookups — the business logic behind MovieController. */
public interface MovieService {

	/** Searches OMDb by title. */
	List<MovieSearchResultResponse> search(String title);

	/**
	 * Returns the cached Movie entity, fetching and upserting from OMDb on a cache
	 * miss. Returns the entity, not a DTO — this is a service-to-service call
	 * (FavoriteListItemServiceImpl uses it too), not a controller boundary, and
	 * DTOs exist specifically to guard the wire boundary, not every method call.
	 * getOrFetch(String) below is the DTO-returning wrapper for MovieController.
	 */
	Movie getOrFetchEntity(String imdbId);

	/** DTO-returning wrapper around getOrFetchEntity, for MovieController. */
	MovieResponse getOrFetch(String imdbId);
}
