package com.example.movielist.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A named collection of movies owned by one User (e.g. "Weekend Watchlist"). */
@Entity
@Table(name = "favorite_lists", uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "name"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FavoriteList extends BaseEntity {

	@Column(nullable = false)
	private String name;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "owner_id", nullable = false)
	private User owner;

	@OneToMany(mappedBy = "list", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<FavoriteListItem> items = new ArrayList<>();

	/** Creates a new, empty list with the given name and owner. */
	public FavoriteList(String name, User owner) {
		this.name = name;
		this.owner = owner;
	}

	/** True if the given movie is already in this list. */
	public boolean containsMovie(Movie movie) {
		return items.stream().anyMatch(item -> item.getMovie().getId().equals(movie.getId()));
	}

	/**
	 * Adds a movie to this list, throwing if it's already in there.
	 * One of a few checks that stop duplicates — see FavoriteListItem for the others.
	 */
	public FavoriteListItem addItem(Movie movie) {
		if (containsMovie(movie)) {
			throw new IllegalStateException(
					"Movie %d is already in list %d".formatted(movie.getId(), getId()));
		}
		FavoriteListItem item = FavoriteListItem.builder()
				.list(this)
				.movie(movie)
				.addedAt(Instant.now())
				.build();
		items.add(item);
		return item;
	}

	/** Removes the given item from this list. */
	public void removeItem(FavoriteListItem item) {
		items.remove(item);
	}
}
