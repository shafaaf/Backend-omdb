package com.example.movielist.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body for POST /api/auth/signup. */
public record SignupRequest(

		@NotBlank @Email
		String email,

		@NotBlank @Size(min = 8, max = 100, message = "password must be at least 8 characters")
		String password,

		@NotBlank @Size(max = 100)
		String displayName
) {
}
