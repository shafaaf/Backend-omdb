package com.example.movielist.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Join row: one movie's membership in one list (the "many-to-many" link between
 * FavoriteList and Movie). The unique constraint below stops the same movie being
 * added to the same list twice at the database level.
 */
@Entity
@Table(name = "favorite_list_items", uniqueConstraints = @UniqueConstraint(columnNames = {"list_id", "movie_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) // required by @Builder; not for direct use
@Builder
public class FavoriteListItem extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "list_id", nullable = false)
	private FavoriteList list;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "movie_id", nullable = false)
	private Movie movie;

	@Column(nullable = false)
	private Instant addedAt;
}
