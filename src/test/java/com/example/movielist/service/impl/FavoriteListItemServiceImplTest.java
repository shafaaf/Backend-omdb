package com.example.movielist.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.movielist.dto.request.AddMovieToListRequest;
import com.example.movielist.dto.response.FavoriteListItemResponse;
import com.example.movielist.entity.FavoriteList;
import com.example.movielist.entity.Movie;
import com.example.movielist.entity.User;
import com.example.movielist.exception.DuplicateResourceException;
import com.example.movielist.exception.IdempotencyConflictException;
import com.example.movielist.exception.ResourceNotFoundException;
import com.example.movielist.repository.FavoriteListItemRepository;
import com.example.movielist.repository.FavoriteListRepository;
import com.example.movielist.repository.MovieRepository;
import com.example.movielist.service.IdempotencyService;
import com.example.movielist.service.IdempotentReplay;
import com.example.movielist.service.MovieService;
import java.lang.reflect.Field;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

/**
 * Exercises the idempotency showcase directly: replay, conflict, and the plain
 * happy path, all without touching a real database or the OMDb API — everything
 * FavoriteListItemServiceImpl depends on is mocked. This is exactly the payoff
 * the plan calls out for declaring FavoriteListService/FavoriteListItemService
 * as interfaces: swapping in mocks here doesn't require Spring at all.
 */
@ExtendWith(MockitoExtension.class)
class FavoriteListItemServiceImplTest {

	@Mock
	private FavoriteListRepository favoriteListRepository;
	@Mock
	private FavoriteListItemRepository favoriteListItemRepository;
	@Mock
	private MovieRepository movieRepository;
	@Mock
	private MovieService movieService;
	@Mock
	private IdempotencyService idempotencyService;

	private FavoriteListItemServiceImpl service;

	private static final Long OWNER_ID = 1L;
	private static final Long LIST_ID = 10L;
	private static final String IMDB_ID = "tt0111161";

	@BeforeEach
	void setUp() {
		service = new FavoriteListItemServiceImpl(
				favoriteListRepository, favoriteListItemRepository, movieRepository,
				movieService, idempotencyService, JsonMapper.builder().build());
	}

	@Test
	void addMovie_happyPath_savesItemAndReturnsResponse() {
		FavoriteList list = newList();
		Movie movie = newMovie();

		when(favoriteListRepository.findByIdAndOwnerId(LIST_ID, OWNER_ID)).thenReturn(Optional.of(list));
		when(movieService.getOrFetchEntity(IMDB_ID)).thenReturn(movie);
		when(favoriteListItemRepository.existsByListIdAndMovieId(LIST_ID, movie.getId())).thenReturn(false);

		FavoriteListItemResponse response = service.addMovie(OWNER_ID, LIST_ID, new AddMovieToListRequest(IMDB_ID), null);

		assertThat(response.movie().externalId()).isEqualTo(IMDB_ID);
		verify(favoriteListItemRepository).save(any());
		verify(idempotencyService, never()).record(anyString(), any(), anyString(), anyString(), anyInt(), anyString());
	}

	@Test
	void addMovie_alreadyInList_throwsDuplicateEvenWithoutIdempotencyKey() {
		FavoriteList list = newList();
		Movie movie = newMovie();

		when(favoriteListRepository.findByIdAndOwnerId(LIST_ID, OWNER_ID)).thenReturn(Optional.of(list));
		when(movieService.getOrFetchEntity(IMDB_ID)).thenReturn(movie);
		when(favoriteListItemRepository.existsByListIdAndMovieId(LIST_ID, movie.getId())).thenReturn(true);

		assertThatThrownBy(() -> service.addMovie(OWNER_ID, LIST_ID, new AddMovieToListRequest(IMDB_ID), null))
				.isInstanceOf(DuplicateResourceException.class);

		verify(favoriteListItemRepository, never()).save(any());
	}

	@Test
	void addMovie_nonOwnedList_throwsNotFound_notForbidden() {
		when(favoriteListRepository.findByIdAndOwnerId(LIST_ID, OWNER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.addMovie(OWNER_ID, LIST_ID, new AddMovieToListRequest(IMDB_ID), null))
				.isInstanceOf(ResourceNotFoundException.class);
	}

	@Test
	void addMovie_withIdempotencyKey_matchingReplay_skipsBusinessLogicEntirely() {
		String storedJson = """
				{"id":99,"movie":{"id":5,"externalId":"tt0111161","title":"The Shawshank Redemption",\
				"releaseYear":1994,"posterUrl":null,"plot":null,"genre":null,"director":null,"imdbRating":null},\
				"addedAt":null}""";
		when(idempotencyService.checkForReplay(anyString(), any(), anyString(), anyString()))
				.thenReturn(Optional.of(new IdempotentReplay(201, storedJson)));

		FavoriteListItemResponse response =
				service.addMovie(OWNER_ID, LIST_ID, new AddMovieToListRequest(IMDB_ID), "key-123");

		assertThat(response.id()).isEqualTo(99L);
		// The whole point of a replay: none of the business logic paths get touched.
		verify(favoriteListRepository, never()).findByIdAndOwnerId(any(), any());
		verify(movieService, never()).getOrFetchEntity(any());
		verify(favoriteListItemRepository, never()).save(any());
	}

	@Test
	void addMovie_withIdempotencyKey_conflictingFingerprint_throwsConflict() {
		when(idempotencyService.checkForReplay(anyString(), any(), anyString(), anyString()))
				.thenThrow(new IdempotencyConflictException("key reused for a different request"));

		assertThatThrownBy(() -> service.addMovie(OWNER_ID, LIST_ID, new AddMovieToListRequest(IMDB_ID), "key-123"))
				.isInstanceOf(IdempotencyConflictException.class);

		verify(favoriteListRepository, never()).findByIdAndOwnerId(any(), any());
	}

	@Test
	void addMovie_withIdempotencyKey_firstUse_recordsResponseAfterSaving() {
		FavoriteList list = newList();
		Movie movie = newMovie();

		when(idempotencyService.checkForReplay(anyString(), any(), anyString(), anyString())).thenReturn(Optional.empty());
		when(favoriteListRepository.findByIdAndOwnerId(LIST_ID, OWNER_ID)).thenReturn(Optional.of(list));
		when(movieService.getOrFetchEntity(IMDB_ID)).thenReturn(movie);
		when(favoriteListItemRepository.existsByListIdAndMovieId(LIST_ID, movie.getId())).thenReturn(false);

		service.addMovie(OWNER_ID, LIST_ID, new AddMovieToListRequest(IMDB_ID), "key-123");

		verify(favoriteListItemRepository).save(any());
		verify(idempotencyService).record(
				eq("key-123"), eq(OWNER_ID), anyString(), anyString(), eq(201), anyString());
	}

	private FavoriteList newList() {
		User owner = new User("owner@example.com", "hash", "Owner");
		setId(owner, OWNER_ID);
		FavoriteList list = new FavoriteList("Favorites", owner);
		setId(list, LIST_ID);
		return list;
	}

	private Movie newMovie() {
		Movie movie = Movie.builder().externalId(IMDB_ID).title("The Shawshank Redemption").releaseYear(1994).build();
		setId(movie, 5L);
		return movie;
	}

	/** Test-only reflection hack: BaseEntity's id is JPA-managed with no public setter,
	 *  which is correct for production code but means tests need a way to give mock
	 *  domain objects an id without a real persistence round-trip. */
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
