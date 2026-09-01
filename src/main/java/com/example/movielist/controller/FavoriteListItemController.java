package com.example.movielist.controller;

import com.example.movielist.dto.request.AddMovieToListRequest;
import com.example.movielist.dto.response.FavoriteListItemResponse;
import com.example.movielist.security.CustomUserDetails;
import com.example.movielist.service.FavoriteListItemService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Add/list/remove movies within one favorite list. Requires auth. The idempotency-key showcase — see CLAUDE.md. */
@RestController
@RequestMapping("/api/lists/{listId}/movies")
@RequiredArgsConstructor
public class FavoriteListItemController {

	private final FavoriteListItemService favoriteListItemService;

	/** Adds a movie (by IMDb id) to the list; the optional Idempotency-Key header makes a retry safe. */
	@PostMapping
	public ResponseEntity<FavoriteListItemResponse> addMovie(
			@AuthenticationPrincipal CustomUserDetails principal,
			@PathVariable Long listId,
			@Valid @RequestBody AddMovieToListRequest request,
			@RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
		FavoriteListItemResponse response =
				favoriteListItemService.addMovie(principal.getId(), listId, request, idempotencyKey);
		return ResponseEntity.status(HttpStatus.CREATED)
				.header(HttpHeaders.LOCATION, "/api/lists/%d/movies/%s".formatted(listId, request.imdbId()))
				.body(response);
	}

	/** Returns every movie currently in the list. */
	@GetMapping
	public ResponseEntity<List<FavoriteListItemResponse>> findAll(
			@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long listId) {
		return ResponseEntity.ok(favoriteListItemService.findAllForList(principal.getId(), listId));
	}

	/** Removes one movie from the list, by its IMDb id. */
	@DeleteMapping("/{imdbId}")
	public ResponseEntity<Void> removeMovie(
			@AuthenticationPrincipal CustomUserDetails principal,
			@PathVariable Long listId, @PathVariable String imdbId) {
		favoriteListItemService.removeMovie(principal.getId(), listId, imdbId);
		return ResponseEntity.noContent().build();
	}
}
