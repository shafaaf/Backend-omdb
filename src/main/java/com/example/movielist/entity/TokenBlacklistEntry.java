package com.example.movielist.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * A logged-out access token. On logout we save its id (jti) here, and
 * JwtAuthenticationFilter rejects any token whose jti shows up in this table —
 * that's what makes logout actually work even though JWTs can't be "deleted".
 */
@Entity
@Table(name = "token_blacklist_entries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TokenBlacklistEntry {

	@Id
	private String jti;

	@Column(nullable = false)
	private Instant blacklistedAt;

	@Column(nullable = false)
	private Instant expiresAt;

	/** Blacklists the given jti, timestamped now, until the given original expiry. */
	public TokenBlacklistEntry(String jti, Instant expiresAt) {
		this.jti = jti;
		this.blacklistedAt = Instant.now();
		this.expiresAt = expiresAt;
	}
}
