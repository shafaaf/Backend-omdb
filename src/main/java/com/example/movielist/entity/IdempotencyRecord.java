package com.example.movielist.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Saves the result of one "Idempotency-Key" protected request, so if the same
 * request comes in again with the same key, we can return the saved result
 * instead of doing the work twice. See FavoriteListItemServiceImpl.addMovie.
 */
@Entity
@Table(
		name = "idempotency_records",
		uniqueConstraints = @UniqueConstraint(columnNames = {"idempotency_key", "user_id", "endpoint_path"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyRecord extends BaseEntity {

	@Column(name = "idempotency_key", nullable = false)
	private String idempotencyKey;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "endpoint_path", nullable = false)
	private String endpointPath;

	/** SHA-256 fingerprint of the request's logically-significant fields. */
	@Column(name = "request_hash", nullable = false)
	private String requestHash;

	@Column(name = "response_status", nullable = false)
	private int responseStatus;

	@Lob
	@Column(name = "response_body")
	private String responseBody;

	/** Records the outcome of one idempotency-key-guarded request for later replay. */
	public IdempotencyRecord(
			String idempotencyKey, Long userId, String endpointPath,
			String requestHash, int responseStatus, String responseBody) {
		this.idempotencyKey = idempotencyKey;
		this.userId = userId;
		this.endpointPath = endpointPath;
		this.requestHash = requestHash;
		this.responseStatus = responseStatus;
		this.responseBody = responseBody;
	}
}
