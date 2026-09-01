package com.example.movielist.mapper;

import com.example.movielist.client.OmdbMovieResponse;
import com.example.movielist.client.OmdbSearchItem;
import com.example.movielist.dto.response.MovieResponse;
import com.example.movielist.dto.response.MovieSearchResultResponse;
import com.example.movielist.entity.Movie;
import java.time.Instant;

/** Converts between OMDb's raw response shapes, the Movie entity, and its response DTOs. */
public final class MovieMapper {

	private MovieMapper() {
	}

	/** Builds a not-yet-persisted Movie from a freshly-fetched OMDb detail response —
	 *  the "upsert" write itself happens in service.impl.MovieServiceImpl. */
	public static Movie fromOmdb(OmdbMovieResponse omdb) {
		return Movie.builder()
				.externalId(omdb.imdbId())
				.title(omdb.title())
				.releaseYear(parseYear(omdb.year()))
				.posterUrl(nullIfNotAvailable(omdb.poster()))
				.plot(nullIfNotAvailable(omdb.plot()))
				.genre(nullIfNotAvailable(omdb.genre()))
				.director(nullIfNotAvailable(omdb.director()))
				.imdbRating(nullIfNotAvailable(omdb.imdbRating()))
				.lastRefreshedAt(Instant.now())
				.build();
	}

	/** Converts a cached Movie entity to its outward-facing response DTO. */
	public static MovieResponse toResponse(Movie movie) {
		return MovieResponse.builder()
				.id(movie.getId())
				.externalId(movie.getExternalId())
				.title(movie.getTitle())
				.releaseYear(movie.getReleaseYear())
				.posterUrl(movie.getPosterUrl())
				.plot(movie.getPlot())
				.genre(movie.getGenre())
				.director(movie.getDirector())
				.imdbRating(movie.getImdbRating())
				.build();
	}

	/** Converts one OMDb search result item to the lightweight search-result DTO. */
	public static MovieSearchResultResponse toSearchResult(OmdbSearchItem item) {
		return new MovieSearchResultResponse(
				item.imdbId(), item.title(), parseYear(item.year()), nullIfNotAvailable(item.poster()));
	}

	/** OMDb years can be ranges for series ("2015–2019") — the start year is good enough here. */
	private static Integer parseYear(String rawYear) {
		if (rawYear == null || rawYear.isBlank()) {
			return null;
		}
		try {
			return Integer.parseInt(rawYear.substring(0, Math.min(4, rawYear.length())));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	/** OMDb uses the literal string "N/A" for a missing field; normalized to null. */
	private static String nullIfNotAvailable(String value) {
		return (value == null || value.isBlank() || "N/A".equals(value)) ? null : value;
	}
}
