package com.example.movielist.service;

import com.example.movielist.dto.request.CreateFavoriteListRequest;
import com.example.movielist.dto.response.FavoriteListResponse;
import java.util.List;

/**
 * Declared as an interface even though FavoriteListServiceImpl is its only
 * implementation — somewhat old-school layered-architecture (modern Spring
 * guidance often skips the interface for a single impl). Kept deliberately: it's
 * a common interview probe ("why interface+impl here?"), and it demonstrates DI
 * against an abstraction plus mock-based unit testing (see
 * FavoriteListServiceImplTest, which mocks the repositories, not this interface —
 * the interface's payoff shows up in FavoriteListController's tests, which can
 * mock FavoriteListService itself without touching persistence at all).
 */
public interface FavoriteListService {

	/** Creates a new, empty list owned by the given user. */
	FavoriteListResponse create(Long ownerId, CreateFavoriteListRequest request);

	/** Returns all lists owned by the given user. */
	List<FavoriteListResponse> findAllForOwner(Long ownerId);

	/** Returns one list, if owned by the given user (404 otherwise). */
	FavoriteListResponse findOneForOwner(Long ownerId, Long listId);

	/** Deletes one list (and its items), if owned by the given user. */
	void delete(Long ownerId, Long listId);
}
