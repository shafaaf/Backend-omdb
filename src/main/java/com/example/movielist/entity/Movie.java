package com.example.movielist.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A movie fetched from OMDb and saved locally so we don't call the API again.
 * Keyed by externalId (the IMDb id, e.g. "tt0111161"). Shared by all users.
 * Built with Lombok's @Builder — e.g. Movie.builder().title("Inception").build().
 */
@Entity
@Table(name = "movies", uniqueConstraints = @UniqueConstraint(columnNames = "external_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE) // required by @Builder; not for direct use
@Builder
public class Movie extends BaseEntity {

	@Column(name = "external_id", nullable = false, unique = true)
	private String externalId;

	@Column(nullable = false)
	private String title;

	// "year" is a reserved word in H2/ANSI SQL (interval literals); mapped to a
	// non-reserved column name to avoid a DDL syntax error.
	@Column(name = "release_year")
	private Integer releaseYear;

	private String posterUrl;

	@Column(length = 2000)
	private String plot;

	private String genre;

	private String director;

	private String imdbRating;

	private Instant lastRefreshedAt;

	/** Applies fresh OMDb data to this already-persisted row (upsert-on-refresh). */
	public void refreshFrom(Movie fetched) {
		this.title = fetched.title;
		this.releaseYear = fetched.releaseYear;
		this.posterUrl = fetched.posterUrl;
		this.plot = fetched.plot;
		this.genre = fetched.genre;
		this.director = fetched.director;
		this.imdbRating = fetched.imdbRating;
		this.lastRefreshedAt = Instant.now();
	}
}
