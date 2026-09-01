package com.example.movielist.mapper;

import com.example.movielist.dto.response.UserResponse;
import com.example.movielist.entity.User;

/** Turns a User entity into the DTO we send to the client. Hand-written, not MapStruct, so it's easy to read line by line. */
public final class UserMapper {

	private UserMapper() {
	}

	/** Converts a User entity to its outward-facing response DTO. */
	public static UserResponse toResponse(User user) {
		return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName());
	}
}
