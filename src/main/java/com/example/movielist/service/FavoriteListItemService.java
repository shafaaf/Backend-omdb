package com.example.movielist.service;

import com.example.movielist.dto.request.AddMovieToListRequest;
import com.example.movielist.dto.response.FavoriteListItemResponse;
import java.util.List;

/** Business logic for the movies inside one favorite list — add, list, remove. */
public interface FavoriteListItemService {

	/** idempotencyKey may be null — the header is optional; see impl for the full flow. */
	FavoriteListItemResponse addMovie(Long ownerId, Long listId, AddMovieToListRequest request, String idempotencyKey);

	/** Returns every movie in the given list, if owned by ownerId. */
	List<FavoriteListItemResponse> findAllForList(Long ownerId, Long listId);

	/** Removes one movie from the given list, if owned by ownerId. */
	void removeMovie(Long ownerId, Long listId, String imdbId);
}
