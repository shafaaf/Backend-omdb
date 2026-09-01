package com.example.movielist.service;

import java.util.Optional;

/** Generic idempotency-key bookkeeping — doesn't know anything about favorite lists specifically. */
public interface IdempotencyService {

	/**
	 * Looks up a prior use of this idempotency key for this user+endpoint.
	 *
	 * @throws com.example.movielist.exception.IdempotencyConflictException if the key
	 *         was already used with a different request fingerprint — reusing a key
	 *         for a logically different request is treated as a client bug, not a replay.
	 * @return the stored response to replay, or empty if this key hasn't been seen
	 *         before (the caller should proceed with the operation).
	 */
	Optional<IdempotentReplay> checkForReplay(String idempotencyKey, Long userId, String endpointPath, String requestFingerprint);

	/** Stores the outcome of one idempotency-key-guarded request so a retry can replay it. */
	void record(String idempotencyKey, Long userId, String endpointPath, String requestFingerprint,
			int responseStatus, String responseBodyJson);
}
