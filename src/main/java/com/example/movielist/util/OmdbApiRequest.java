package com.example.movielist.util;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Hand-rolled Builder — contrast with entity.Movie / entity.FavoriteListItem,
 * which use Lombok's @Builder. Lombok's generated builder is the right tool for
 * "construct an object with several optional fields"; this one exists because
 * the real-world case here is different: exactly one of three mutually exclusive
 * query modes (title search, imdbId lookup, free-text search term) must be set,
 * plus an always-required apiKey, and that invariant needs to be checked at
 * build() time — Lombok's @Builder has no hook for that, it just assembles
 * whatever fields were set. A hand-rolled builder can validate before the object
 * ever exists.
 */
public final class OmdbApiRequest {

	private final String apiKey;
	private final String imdbId;
	private final String searchTerm;

	private OmdbApiRequest(Builder builder) {
		this.apiKey = builder.apiKey;
		this.imdbId = builder.imdbId;
		this.searchTerm = builder.searchTerm;
	}

	public String apiKey() {
		return apiKey;
	}

	public String imdbId() {
		return imdbId;
	}

	public String searchTerm() {
		return searchTerm;
	}

	public boolean isImdbIdLookup() {
		return imdbId != null;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private String apiKey;
		private String imdbId;
		private String searchTerm;

		public Builder apiKey(String apiKey) {
			this.apiKey = apiKey;
			return this;
		}

		public Builder imdbId(String imdbId) {
			this.imdbId = imdbId;
			return this;
		}

		public Builder searchTerm(String searchTerm) {
			this.searchTerm = searchTerm;
			return this;
		}

		/** Validates required fields and constructs the request; throws if the invariant above is violated. */
		public OmdbApiRequest build() {
			if (apiKey == null || apiKey.isBlank()) {
				throw new IllegalStateException("apiKey is required");
			}
			long modesSet = Stream.of(imdbId, searchTerm).filter(Objects::nonNull).count();
			if (modesSet != 1) {
				throw new IllegalStateException("Exactly one of imdbId or searchTerm must be set");
			}
			return new OmdbApiRequest(this);
		}
	}
}
