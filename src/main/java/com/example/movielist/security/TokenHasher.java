package com.example.movielist.security;

import com.example.movielist.util.HashUtil;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

/**
 * Generates opaque refresh-token values and hashes them for storage. Only the
 * hash is ever persisted (see entity.RefreshToken) — a DB read can't be replayed
 * as a live credential, and a raw token compromised in transit can't be
 * reconstructed from the stored row.
 */
public final class TokenHasher {

	private TokenHasher() {
	}

	/** Generates a new random, unguessable opaque refresh-token value (not a JWT). */
	public static String generateOpaqueToken() {
		byte[] randomBytes = new byte[32];
		new SecureRandom().nextBytes(randomBytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes) + "." + UUID.randomUUID();
	}

	/** Hashes a raw refresh-token value for storage/lookup. */
	public static String sha256Hex(String value) {
		return HashUtil.sha256Hex(value);
	}
}
