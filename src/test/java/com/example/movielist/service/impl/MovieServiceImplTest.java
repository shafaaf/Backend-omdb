package com.example.movielist.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.movielist.client.OmdbClient;
import com.example.movielist.client.OmdbMovieResponse;
import com.example.movielist.client.OmdbSearchItem;
import com.example.movielist.dto.response.MovieResponse;
import com.example.movielist.dto.response.MovieSearchResultResponse;
import com.example.movielist.entity.Movie;
import com.example.movielist.exception.ResourceNotFoundException;
import com.example.movielist.repository.MovieRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for MovieServiceImpl, demonstrating cache-hit, cache-miss, and search
 * flows without touching the real OMDb API or database — all dependencies are
 * mocked to isolate the service's business logic.
 */
@ExtendWith(MockitoExtension.class)
class MovieServiceImplTest {

	@Mock
	private MovieRepository movieRepository;
	@Mock
	private OmdbClient omdbClient;

	private MovieServiceImpl service;

	private static final String IMDB_ID = "tt0111161";
	private static final String TITLE = "The Shawshank Redemption";
	private static final Integer YEAR = 1994;

	@BeforeEach
	void setUp() {
		service = new MovieServiceImpl(movieRepository, omdbClient);
	}

	/**
	 * Verifies that search delegates to the OMDb client and maps results to
	 * MovieSearchResultResponse DTOs without touching the database.
	 */
	@Test
	void search_validTitle_returnsMappedSearchResults() {
		// Arrange: mock the OMDb client to return two search results
		OmdbSearchItem item1 = new OmdbSearchItem(IMDB_ID, TITLE, "1994", "http://poster1.jpg");
		OmdbSearchItem item2 = new OmdbSearchItem("tt0068646", "The Godfather", "1972", "http://poster2.jpg");
		when(omdbClient.search("Shawshank")).thenReturn(List.of(item1, item2));

		// Act
		List<MovieSearchResultResponse> results = service.search("Shawshank");

		// Assert: results are mapped correctly
		assertThat(results).hasSize(2);
		assertThat(results.get(0).externalId()).isEqualTo(IMDB_ID);
		assertThat(results.get(0).title()).isEqualTo(TITLE);
		assertThat(results.get(0).releaseYear()).isEqualTo(1994);
		assertThat(results.get(1).externalId()).isEqualTo("tt0068646");
		verify(movieRepository, never()).findByExternalId(anyString());
	}

	/**
	 * Verifies that getOrFetchEntity returns a cached movie from the repository
	 * on a cache hit, without calling the OMDb API.
	 */
	@Test
	void getOrFetchEntity_cachedMovie_returnsFromRepository() {
		// Arrange: mock repository to return an existing movie
		Movie cached = Movie.builder()
				.externalId(IMDB_ID)
				.title(TITLE)
				.releaseYear(YEAR)
				.posterUrl("http://poster.jpg")
				.build();
		setId(cached, 1L);
		when(movieRepository.findByExternalId(IMDB_ID)).thenReturn(Optional.of(cached));

		// Act
		Movie result = service.getOrFetchEntity(IMDB_ID);

		// Assert: returned movie matches the cached version
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getExternalId()).isEqualTo(IMDB_ID);
		assertThat(result.getTitle()).isEqualTo(TITLE);
		// OmdbClient should never be called on a cache hit
		verify(omdbClient, never()).fetchByImdbId(anyString());
	}

	/**
	 * Verifies that getOrFetchEntity fetches from OMDb and persists to the
	 * repository on a cache miss, then returns the saved entity.
	 */
	@Test
	void getOrFetchEntity_notCached_fetchesFromOmdbAndPersists() {
		// Arrange: mock repository to return empty (cache miss)
		when(movieRepository.findByExternalId(IMDB_ID)).thenReturn(Optional.empty());

		// Mock OMDb client to return fresh movie data
		OmdbMovieResponse omdbResponse = new OmdbMovieResponse(
				IMDB_ID, TITLE, "1994", "http://poster.jpg", "A prison drama",
				"Drama", "Frank Darabont", "9.3", "True", null);
		when(omdbClient.fetchByImdbId(IMDB_ID)).thenReturn(Optional.of(omdbResponse));

		// Mock repository save to return the persisted movie
		Movie persisted = Movie.builder()
				.externalId(IMDB_ID)
				.title(TITLE)
				.releaseYear(YEAR)
				.build();
		setId(persisted, 2L);
		when(movieRepository.save(any(Movie.class))).thenReturn(persisted);

		// Act
		Movie result = service.getOrFetchEntity(IMDB_ID);

		// Assert: the movie was fetched and saved
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(2L);
		assertThat(result.getExternalId()).isEqualTo(IMDB_ID);
		verify(omdbClient).fetchByImdbId(IMDB_ID);
		verify(movieRepository).save(any(Movie.class));
	}

	/**
	 * Verifies that getOrFetchEntity throws ResourceNotFoundException when the
	 * movie is not cached and OMDb has no matching movie.
	 */
	@Test
	void getOrFetchEntity_notFoundInOmdb_throwsResourceNotFoundException() {
		// Arrange: mock repository to return empty (cache miss)
		when(movieRepository.findByExternalId(IMDB_ID)).thenReturn(Optional.empty());

		// Mock OMDb client to return empty (movie not found)
		when(omdbClient.fetchByImdbId(IMDB_ID)).thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> service.getOrFetchEntity(IMDB_ID))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining(IMDB_ID);

		verify(movieRepository, never()).save(any());
	}

	/**
	 * Verifies that getOrFetch returns a DTO by delegating to getOrFetchEntity
	 * and mapping the result — a thin wrapper layer.
	 */
	@Test
	void getOrFetch_cachedMovie_returnsMappedDto() {
		// Arrange
		Movie cached = Movie.builder()
				.externalId(IMDB_ID)
				.title(TITLE)
				.releaseYear(YEAR)
				.genre("Drama")
				.imdbRating("9.3")
				.build();
		setId(cached, 3L);
		when(movieRepository.findByExternalId(IMDB_ID)).thenReturn(Optional.of(cached));

		// Act
		MovieResponse result = service.getOrFetch(IMDB_ID);

		// Assert: DTO contains the correct data
		assertThat(result).isNotNull();
		assertThat(result.id()).isEqualTo(3L);
		assertThat(result.externalId()).isEqualTo(IMDB_ID);
		assertThat(result.title()).isEqualTo(TITLE);
	}

	/** Test-only reflection hack to set id on entities without a public setter. */
	private void setId(Object entity, Long id) {
		try {
			Field field = entity.getClass().getSuperclass().getDeclaredField("id");
			field.setAccessible(true);
			field.set(entity, id);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}
}
