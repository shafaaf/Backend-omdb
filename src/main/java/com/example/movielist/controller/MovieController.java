package com.example.movielist.controller;

import com.example.movielist.dto.response.MovieResponse;
import com.example.movielist.dto.response.MovieSearchResultResponse;
import com.example.movielist.service.MovieService;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Both endpoints here are permitAll in SecurityConfig — browsing movies doesn't
 * require an account, mirroring real IMDb. Only favorite-list management
 * (FavoriteListController, FavoriteListItemController) requires authentication.
 */
@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
@Validated
public class MovieController {

	private final MovieService movieService;

	/** Searches OMDb by title; results aren't cached (see MovieServiceImpl for why). */
	@GetMapping("/search")
	public ResponseEntity<List<MovieSearchResultResponse>> search(@RequestParam @NotBlank String title) {
		return ResponseEntity.ok(movieService.search(title));
	}

	/** Returns one movie's full detail by IMDb id, fetching-and-caching from OMDb on a cache miss. */
	@GetMapping("/{imdbId}")
	public ResponseEntity<MovieResponse> getOne(@PathVariable String imdbId) {
		return ResponseEntity.ok(movieService.getOrFetch(imdbId));
	}
}
