package com.example.movielist.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One refresh token issued to one user — lets them get a new access token
 * without logging in again. We only store its hash, never the real value,
 * so a database leak can't be used as a working credential.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseEntity {

	@Column(nullable = false, unique = true)
	private String tokenHash;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false)
	private Instant issuedAt;

	@Column(nullable = false)
	private Instant expiresAt;

	@Column(nullable = false)
	private boolean revoked;

	private Instant revokedAt;

	/** Creates a new, not-yet-revoked refresh token record. */
	public RefreshToken(String tokenHash, User user, Instant issuedAt, Instant expiresAt) {
		this.tokenHash = tokenHash;
		this.user = user;
		this.issuedAt = issuedAt;
		this.expiresAt = expiresAt;
		this.revoked = false;
	}

	/** Marks this token as no longer usable (e.g. on logout or after it's rotated). */
	public void revoke() {
		this.revoked = true;
		this.revokedAt = Instant.now();
	}

	/** True if this token hasn't been revoked and hasn't expired yet. */
	public boolean isUsable() {
		return !revoked && Instant.now().isBefore(expiresAt);
	}
}
