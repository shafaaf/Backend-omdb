package com.example.movielist.mapper;

import com.example.movielist.dto.response.FavoriteListItemResponse;
import com.example.movielist.dto.response.FavoriteListResponse;
import com.example.movielist.entity.FavoriteList;
import com.example.movielist.entity.FavoriteListItem;

/** Converts FavoriteList/FavoriteListItem entities to their response DTOs. */
public final class FavoriteListMapper {

	private FavoriteListMapper() {
	}

	/** Converts one list item, including its nested movie, to a response DTO. */
	public static FavoriteListItemResponse toItemResponse(FavoriteListItem item) {
		return new FavoriteListItemResponse(
				item.getId(),
				MovieMapper.toResponse(item.getMovie()),
				item.getAddedAt()
		);
	}

	/** Converts a list to a response DTO. itemCount is passed in (from a separate COUNT query) instead of loading every item just to count them. */
	public static FavoriteListResponse toResponse(FavoriteList list, long itemCount) {
		return new FavoriteListResponse(
				list.getId(),
				list.getName(),
				itemCount,
				list.getCreatedAt()
		);
	}
}
