package com.example.movielist.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for POST /api/lists. */
public record CreateFavoriteListRequest(

		@NotBlank @Size(max = 100)
		String name
) {
}
