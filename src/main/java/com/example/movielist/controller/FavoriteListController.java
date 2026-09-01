package com.example.movielist.controller;

import com.example.movielist.dto.request.CreateFavoriteListRequest;
import com.example.movielist.dto.response.FavoriteListResponse;
import com.example.movielist.security.CustomUserDetails;
import com.example.movielist.service.FavoriteListService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CRUD for a user's own favorite lists (not the movies inside them — see FavoriteListItemController). Requires auth. */
@RestController
@RequestMapping("/api/lists")
@RequiredArgsConstructor
public class FavoriteListController {

	private final FavoriteListService favoriteListService;

	/** Creates a new, empty list owned by the current user. */
	@PostMapping
	public ResponseEntity<FavoriteListResponse> create(
			@AuthenticationPrincipal CustomUserDetails principal,
			@Valid @RequestBody CreateFavoriteListRequest request) {
		FavoriteListResponse created = favoriteListService.create(principal.getId(), request);
		return ResponseEntity.created(URI.create("/api/lists/" + created.id())).body(created);
	}

	/** Returns all lists owned by the current user. */
	@GetMapping
	public ResponseEntity<List<FavoriteListResponse>> findAll(@AuthenticationPrincipal CustomUserDetails principal) {
		return ResponseEntity.ok(favoriteListService.findAllForOwner(principal.getId()));
	}

	/** Returns one list, if owned by the current user (404 otherwise). */
	@GetMapping("/{listId}")
	public ResponseEntity<FavoriteListResponse> findOne(
			@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long listId) {
		return ResponseEntity.ok(favoriteListService.findOneForOwner(principal.getId(), listId));
	}

	/** Deletes one list (and its items), if owned by the current user. */
	@DeleteMapping("/{listId}")
	public ResponseEntity<Void> delete(
			@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long listId) {
		favoriteListService.delete(principal.getId(), listId);
		return ResponseEntity.noContent().build();
	}
}
