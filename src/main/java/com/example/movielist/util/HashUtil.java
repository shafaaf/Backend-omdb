package com.example.movielist.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Generic SHA-256 hashing, used for two unrelated purposes elsewhere: hashing
 * refresh tokens for storage (security.TokenHasher) and fingerprinting a
 * request's logically-significant fields for idempotency-key replay detection
 * (service.impl.FavoriteListItemServiceImpl). Kept here rather than duplicated
 * in both places, since neither use is really "about" the other's domain.
 */
public final class HashUtil {

	private HashUtil() {
	}

	/** Hashes the given string with SHA-256 and returns it as a lowercase hex string. */
	public static String sha256Hex(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException e) {
			// SHA-256 is guaranteed available on every JDK; this can't actually happen.
			throw new IllegalStateException(e);
		}
	}
}
