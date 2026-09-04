package com.example.movielist.service.impl;

import com.example.movielist.client.OmdbClient;
import com.example.movielist.client.OmdbMovieResponse;
import com.example.movielist.client.OmdbSearchItem;
import com.example.movielist.dto.response.MovieResponse;
import com.example.movielist.dto.response.MovieSearchResultResponse;
import com.example.movielist.entity.Movie;
import com.example.movielist.exception.ResourceNotFoundException;
import com.example.movielist.mapper.MovieMapper;
import com.example.movielist.repository.MovieRepository;
import com.example.movielist.service.MovieService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Note: getOrFetch delegates to getOrFetchEntity via a plain `this.` call, so it
 * deliberately does NOT mark itself @Transactional(readOnly = true) even though
 * it's conceptually a read: Spring's @Transactional works through a proxy, and a
 * same-class method call bypasses that proxy entirely, silently ignoring
 * whatever annotation is on the callee. Marking getOrFetch read-only here would
 * misleadingly suggest the cache-miss write path is excluded, when in fact the
 * whole call just runs inside getOrFetch's own (default, read-write) transaction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MovieServiceImpl implements MovieService {

	private final MovieRepository movieRepository;
	private final OmdbClient omdbClient;

	@Override
	@Transactional(readOnly = true)
	public List<MovieSearchResultResponse> search(String title) {
		List<OmdbSearchItem> items = omdbClient.search(title);

		// OMDb's search endpoint doesn't return ratings at all, so we backfill them
		// from whatever's already cached locally (one batch lookup, not N). Movies
		// nobody has viewed/added yet simply have no rating in the results — this is
		// expected, not a bug (see MovieSearchResultResponse's javadoc).
		Map<String, String> cachedRatingsByExternalId = movieRepository
				.findByExternalIdIn(items.stream().map(OmdbSearchItem::imdbId).toList())
				.stream()
				.collect(Collectors.toMap(Movie::getExternalId, Movie::getImdbRating));

		return items.stream()
				.map(item -> MovieMapper.toSearchResult(item, cachedRatingsByExternalId.get(item.imdbId())))
				.toList();
	}

	@Override
	public Movie getOrFetchEntity(String imdbId) {
		return movieRepository.findByExternalId(imdbId)
				.orElseGet(() -> fetchAndCache(imdbId));
	}

	@Override
	public MovieResponse getOrFetch(String imdbId) {
		return MovieMapper.toResponse(getOrFetchEntity(imdbId));
	}

	/** Fetches one movie from OMDb and saves it as a new cached row; throws if OMDb has no such movie. */
	private Movie fetchAndCache(String imdbId) {
		OmdbMovieResponse omdbResponse = omdbClient.fetchByImdbId(imdbId)
				.orElseThrow(() -> new ResourceNotFoundException("No movie found for id " + imdbId));

		Movie saved = movieRepository.save(MovieMapper.fromOmdb(omdbResponse));
		log.info("cached movie externalId={} title={}", saved.getExternalId(), saved.getTitle());
		return saved;
	}
}
