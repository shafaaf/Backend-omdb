package com.example.movielist.service.impl;

import com.example.movielist.dto.request.AddMovieToListRequest;
import com.example.movielist.dto.response.FavoriteListItemResponse;
import com.example.movielist.entity.FavoriteList;
import com.example.movielist.entity.FavoriteListItem;
import com.example.movielist.entity.Movie;
import com.example.movielist.exception.DuplicateResourceException;
import com.example.movielist.exception.ResourceNotFoundException;
import com.example.movielist.mapper.FavoriteListMapper;
import com.example.movielist.repository.FavoriteListItemRepository;
import com.example.movielist.repository.FavoriteListRepository;
import com.example.movielist.repository.MovieRepository;
import com.example.movielist.service.FavoriteListItemService;
import com.example.movielist.service.IdempotencyService;
import com.example.movielist.service.IdempotentReplay;
import com.example.movielist.service.MovieService;
import com.example.movielist.util.HashUtil;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * addMovie is the idempotency showcase for this project — see CLAUDE.md for the
 * full request-lifecycle writeup. Short version: an Idempotency-Key header lets a
 * client safely retry this call (e.g. after a timed-out response whose outcome
 * is unknown) without risking a duplicate list entry, and without the client
 * having to first check whether its earlier attempt actually succeeded.
 *
 * Concurrency note: two requests carrying the same brand-new key can both pass
 * checkForReplay's "not found yet" branch before either commits — the read and
 * the eventual write aren't atomic with respect to each other. What actually
 * closes that race is the DB unique constraint on
 * (idempotency_key, user_id, endpoint_path) declared on IdempotencyRecord: the
 * loser's insert throws DataIntegrityViolationException, which
 * GlobalExceptionHandler turns into a 409 telling the client its retry landed in
 * a genuine race and it should re-read rather than assume failure. A stricter
 * implementation would catch that exception here and re-fetch+replay the
 * winner's now-committed record instead; a 409 is the simpler, still-correct
 * choice for a study project.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FavoriteListItemServiceImpl implements FavoriteListItemService {

	private final FavoriteListRepository favoriteListRepository;
	private final FavoriteListItemRepository favoriteListItemRepository;
	private final MovieRepository movieRepository;
	private final MovieService movieService;
	private final IdempotencyService idempotencyService;
	private final ObjectMapper objectMapper;

	/**
	 * Adds a movie to a list: replays a prior idempotent response if one matches,
	 * otherwise verifies ownership, fetches-or-caches the movie, and saves.
	 */
	@Override
	public FavoriteListItemResponse addMovie(
			Long ownerId, Long listId, AddMovieToListRequest request, String idempotencyKey) {

		String endpointPath = "/api/lists/%d/movies".formatted(listId);
		String requestFingerprint = HashUtil.sha256Hex(listId + ":" + request.imdbId());
		boolean usingIdempotencyKey = idempotencyKey != null && !idempotencyKey.isBlank();

		if (usingIdempotencyKey) {
			Optional<IdempotentReplay> replay =
					idempotencyService.checkForReplay(idempotencyKey, ownerId, endpointPath, requestFingerprint);
			if (replay.isPresent()) {
				log.info("replaying idempotent response key={} listId={}", idempotencyKey, listId);
				return deserialize(replay.get().responseBodyJson());
			}
		}

		// Scoped by (id, ownerId) so a non-owner's request 404s instead of 403 — never
		// confirms the list exists to someone who doesn't own it.
		FavoriteList list = favoriteListRepository.findByIdAndOwnerId(listId, ownerId)
				.orElseThrow(() -> new ResourceNotFoundException("Favorite list %d not found".formatted(listId)));

		// Cache-or-fetch: a hit is a local read, a miss calls out to OMDb and upserts —
		// see MovieServiceImpl.
		Movie movie = movieService.getOrFetchEntity(request.imdbId());

		// Defense-in-depth: this check protects a caller with no idempotency key too.
		if (favoriteListItemRepository.existsByListIdAndMovieId(listId, movie.getId())) {
			throw new DuplicateResourceException("Movie is already in this list");
		}

		FavoriteListItem item = list.addItem(movie); // domain-level duplicate guard, see FavoriteList
		favoriteListItemRepository.save(item);
		log.info("movie added to list listId={} movieId={} ownerId={}", listId, movie.getId(), ownerId);

		FavoriteListItemResponse response = FavoriteListMapper.toItemResponse(item);

		if (usingIdempotencyKey) {
			// Recorded in the same transaction as the write above: if this insert fails,
			// the whole operation rolls back together, so a committed side effect can never
			// exist without its replay record.
			idempotencyService.record(
					idempotencyKey, ownerId, endpointPath, requestFingerprint,
					HttpStatus.CREATED.value(), serialize(response));
		}

		return response;
	}

	@Override
	@Transactional(readOnly = true)
	public List<FavoriteListItemResponse> findAllForList(Long ownerId, Long listId) {
		ensureOwnership(ownerId, listId);
		return favoriteListItemRepository.findByListIdWithMovie(listId).stream()
				.map(FavoriteListMapper::toItemResponse)
				.toList();
	}

	@Override
	public void removeMovie(Long ownerId, Long listId, String imdbId) {
		ensureOwnership(ownerId, listId);

		Movie movie = movieRepository.findByExternalId(imdbId)
				.orElseThrow(() -> new ResourceNotFoundException("Movie is not in this list"));
		FavoriteListItem item = favoriteListItemRepository.findByListIdAndMovieId(listId, movie.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Movie is not in this list"));

		favoriteListItemRepository.delete(item);
		log.info("movie removed from list listId={} movieId={} ownerId={}", listId, movie.getId(), ownerId);
	}

	/** Throws ResourceNotFoundException unless the given list is owned by ownerId. */
	private void ensureOwnership(Long ownerId, Long listId) {
		favoriteListRepository.findByIdAndOwnerId(listId, ownerId)
				.orElseThrow(() -> new ResourceNotFoundException("Favorite list %d not found".formatted(listId)));
	}

	/** Serializes a response DTO to JSON for storage in an IdempotencyRecord. */
	private String serialize(FavoriteListItemResponse response) {
		try {
			return objectMapper.writeValueAsString(response);
		} catch (RuntimeException e) {
			throw new IllegalStateException("Failed to serialize idempotent response", e);
		}
	}

	/** Deserializes a stored IdempotencyRecord's JSON back into a response DTO for replay. */
	private FavoriteListItemResponse deserialize(String json) {
		try {
			return objectMapper.readValue(json, FavoriteListItemResponse.class);
		} catch (RuntimeException e) {
			throw new IllegalStateException("Failed to deserialize stored idempotent response", e);
		}
	}
}
