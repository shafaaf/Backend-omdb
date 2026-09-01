package com.example.movielist.repository;

import com.example.movielist.entity.IdempotencyRecord;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for IdempotencyRecord — stored responses for replaying retried requests. */
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, Long> {

	/** Looks up the stored response (if any) for one idempotency key + user + endpoint. */
	Optional<IdempotencyRecord> findByIdempotencyKeyAndUserIdAndEndpointPath(
			String idempotencyKey, Long userId, String endpointPath);
}
