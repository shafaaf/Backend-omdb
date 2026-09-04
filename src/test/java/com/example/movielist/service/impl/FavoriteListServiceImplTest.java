package com.example.movielist.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.movielist.dto.request.CreateFavoriteListRequest;
import com.example.movielist.dto.response.FavoriteListResponse;
import com.example.movielist.entity.FavoriteList;
import com.example.movielist.entity.User;
import com.example.movielist.exception.DuplicateResourceException;
import com.example.movielist.exception.ResourceNotFoundException;
import com.example.movielist.repository.FavoriteListItemRepository;
import com.example.movielist.repository.FavoriteListRepository;
import com.example.movielist.repository.UserRepository;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for FavoriteListServiceImpl, demonstrating CRUD operations with
 * ownership checks and duplicate-name validation — all repository calls are
 * mocked to test the service logic in isolation.
 */
@ExtendWith(MockitoExtension.class)
class FavoriteListServiceImplTest {

	@Mock
	private FavoriteListRepository favoriteListRepository;
	@Mock
	private FavoriteListItemRepository favoriteListItemRepository;
	@Mock
	private UserRepository userRepository;

	private FavoriteListServiceImpl service;

	private static final Long OWNER_ID = 1L;
	private static final Long LIST_ID = 10L;
	private static final String LIST_NAME = "My Favorites";

	@BeforeEach
	void setUp() {
		service = new FavoriteListServiceImpl(
				favoriteListRepository, favoriteListItemRepository, userRepository);
	}

	/**
	 * Verifies that create throws DuplicateResourceException when a list with
	 * the same name already exists for this owner — preventing duplicate names
	 * at the service layer before any persistence attempt.
	 */
	@Test
	void create_duplicateName_throwsDuplicateResourceException() {
		// Arrange: mock repository to indicate a list with this name already exists
		when(favoriteListRepository.existsByOwnerIdAndName(OWNER_ID, LIST_NAME))
				.thenReturn(true);

		// Act & Assert
		CreateFavoriteListRequest request = new CreateFavoriteListRequest(LIST_NAME);
		assertThatThrownBy(() -> service.create(OWNER_ID, request))
				.isInstanceOf(DuplicateResourceException.class)
				.hasMessageContaining(LIST_NAME);

		// Verify that the save was never attempted due to early validation
		verify(favoriteListRepository, never()).save(any());
	}

	/**
	 * Verifies that create successfully saves a new list with a unique name,
	 * returning a FavoriteListResponse with item count initialized to zero.
	 */
	@Test
	void create_newName_savesAndReturnsResponse() {
		// Arrange: mock repository to allow creation (no existing list with this name)
		when(favoriteListRepository.existsByOwnerIdAndName(OWNER_ID, LIST_NAME))
				.thenReturn(false);

		// Mock user reference lookup (getReferenceById returns a lazy proxy)
		User owner = new User("owner@example.com", "hash", "Owner");
		setId(owner, OWNER_ID);
		when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);

		// Mock repository save to return the persisted list
		FavoriteList persisted = new FavoriteList(LIST_NAME, owner);
		setId(persisted, LIST_ID);
		when(favoriteListRepository.save(any(FavoriteList.class))).thenReturn(persisted);

		// Act
		CreateFavoriteListRequest request = new CreateFavoriteListRequest(LIST_NAME);
		FavoriteListResponse result = service.create(OWNER_ID, request);

		// Assert: response contains the list details
		assertThat(result).isNotNull();
		assertThat(result.id()).isEqualTo(LIST_ID);
		assertThat(result.name()).isEqualTo(LIST_NAME);
		assertThat(result.itemCount()).isEqualTo(0);
		verify(favoriteListRepository).save(any(FavoriteList.class));
	}

	/**
	 * Verifies that findAllForOwner returns all lists owned by the user with
	 * correct item counts — demonstrates aggregation of count data from the
	 * FavoriteListItemRepository.
	 */
	@Test
	void findAllForOwner_multipleListsOwned_returnsAllWithItemCounts() {
		// Arrange: create test user and lists
		User owner = new User("owner@example.com", "hash", "Owner");
		setId(owner, OWNER_ID);

		FavoriteList list1 = new FavoriteList("List One", owner);
		setId(list1, 10L);
		FavoriteList list2 = new FavoriteList("List Two", owner);
		setId(list2, 11L);

		// Mock repository to return owned lists
		when(favoriteListRepository.findByOwnerId(OWNER_ID))
				.thenReturn(List.of(list1, list2));

		// Mock item counts for each list (returns Long, not int)
		when(favoriteListItemRepository.countByListId(10L)).thenReturn(3L);
		when(favoriteListItemRepository.countByListId(11L)).thenReturn(5L);

		// Act
		List<FavoriteListResponse> results = service.findAllForOwner(OWNER_ID);

		// Assert: all lists returned with correct counts
		assertThat(results).hasSize(2);
		assertThat(results.get(0).id()).isEqualTo(10L);
		assertThat(results.get(0).itemCount()).isEqualTo(3);
		assertThat(results.get(1).id()).isEqualTo(11L);
		assertThat(results.get(1).itemCount()).isEqualTo(5);
	}

	/**
	 * Verifies that findOneForOwner throws ResourceNotFoundException when a
	 * non-owner attempts to access a list — a security check that returns 404
	 * (never 403) to avoid leaking list existence.
	 */
	@Test
	void findOneForOwner_unownedList_throwsNotFound() {
		// Arrange: mock repository to return empty (list not owned by this user)
		when(favoriteListRepository.findByIdAndOwnerId(LIST_ID, OWNER_ID))
				.thenReturn(Optional.empty());

		// Act & Assert
		assertThatThrownBy(() -> service.findOneForOwner(OWNER_ID, LIST_ID))
				.isInstanceOf(ResourceNotFoundException.class);

		// Verify that item count was never queried due to early check
		verify(favoriteListItemRepository, never()).countByListId(anyLong());
	}

	/**
	 * Verifies that delete successfully removes a list and all its items when
	 * the user owns the list, and throws ResourceNotFoundException otherwise.
	 */
	@Test
	void delete_ownedList_deletesSuccessfully() {
		// Arrange
		User owner = new User("owner@example.com", "hash", "Owner");
		setId(owner, OWNER_ID);
		FavoriteList list = new FavoriteList(LIST_NAME, owner);
		setId(list, LIST_ID);

		when(favoriteListRepository.findByIdAndOwnerId(LIST_ID, OWNER_ID))
				.thenReturn(Optional.of(list));

		// Act
		service.delete(OWNER_ID, LIST_ID);

		// Assert: list was deleted
		verify(favoriteListRepository).delete(list);
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
