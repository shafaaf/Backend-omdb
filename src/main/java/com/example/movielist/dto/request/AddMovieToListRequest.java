package com.example.movielist.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Request body for POST /api/lists/{listId}/movies. */
public record AddMovieToListRequest(

		@NotBlank
		String imdbId
) {
}
