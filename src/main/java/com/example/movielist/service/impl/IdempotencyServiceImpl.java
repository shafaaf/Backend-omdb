package com.example.movielist.service.impl;

import com.example.movielist.entity.IdempotencyRecord;
import com.example.movielist.exception.IdempotencyConflictException;
import com.example.movielist.repository.IdempotencyRecordRepository;
import com.example.movielist.service.IdempotencyService;
import com.example.movielist.service.IdempotentReplay;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** See IdempotencyService for what each method does. */
@Service
@RequiredArgsConstructor
@Transactional
public class IdempotencyServiceImpl implements IdempotencyService {

	private final IdempotencyRecordRepository idempotencyRecordRepository;

	@Override
	@Transactional(readOnly = true)
	public Optional<IdempotentReplay> checkForReplay(
			String idempotencyKey, Long userId, String endpointPath, String requestFingerprint) {
		return idempotencyRecordRepository
				.findByIdempotencyKeyAndUserIdAndEndpointPath(idempotencyKey, userId, endpointPath)
				.map(existing -> {
					if (!existing.getRequestHash().equals(requestFingerprint)) {
						throw new IdempotencyConflictException(
								"Idempotency-Key '%s' was already used for a different request".formatted(idempotencyKey));
					}
					return new IdempotentReplay(existing.getResponseStatus(), existing.getResponseBody());
				});
	}

	@Override
	public void record(
			String idempotencyKey, Long userId, String endpointPath, String requestFingerprint,
			int responseStatus, String responseBodyJson) {
		idempotencyRecordRepository.save(new IdempotencyRecord(
				idempotencyKey, userId, endpointPath, requestFingerprint, responseStatus, responseBodyJson));
	}
}
