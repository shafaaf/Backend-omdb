package com.example.movielist.service.impl;

import com.example.movielist.dto.request.CreateFavoriteListRequest;
import com.example.movielist.dto.response.FavoriteListResponse;
import com.example.movielist.entity.FavoriteList;
import com.example.movielist.entity.User;
import com.example.movielist.exception.DuplicateResourceException;
import com.example.movielist.exception.ResourceNotFoundException;
import com.example.movielist.mapper.FavoriteListMapper;
import com.example.movielist.repository.FavoriteListItemRepository;
import com.example.movielist.repository.FavoriteListRepository;
import com.example.movielist.repository.UserRepository;
import com.example.movielist.service.FavoriteListService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** See FavoriteListService for what each method does. */
@Slf4j
@Service
@RequiredArgsConstructor // constructor DI: Spring auto-detects the single constructor, no @Autowired needed
@Transactional(readOnly = true)
public class FavoriteListServiceImpl implements FavoriteListService {

	private final FavoriteListRepository favoriteListRepository;
	private final FavoriteListItemRepository favoriteListItemRepository;
	private final UserRepository userRepository;

	@Override
	@Transactional
	public FavoriteListResponse create(Long ownerId, CreateFavoriteListRequest request) {
		if (favoriteListRepository.existsByOwnerIdAndName(ownerId, request.name())) {
			throw new DuplicateResourceException(
					"You already have a list named '%s'".formatted(request.name()));
		}

		// getReferenceById returns a lazy proxy instead of issuing a SELECT — safe here
		// because ownerId always comes from an already-authenticated principal, so the
		// row is known to exist. Saving the FavoriteList triggers the FK check anyway.
		User owner = userRepository.getReferenceById(ownerId);
		FavoriteList saved = favoriteListRepository.save(new FavoriteList(request.name(), owner));

		log.info("favorite list created id={} ownerId={}", saved.getId(), ownerId);
		return FavoriteListMapper.toResponse(saved, 0);
	}

	@Override
	public List<FavoriteListResponse> findAllForOwner(Long ownerId) {
		return favoriteListRepository.findByOwnerId(ownerId).stream()
				.map(list -> FavoriteListMapper.toResponse(list, favoriteListItemRepository.countByListId(list.getId())))
				.toList();
	}

	@Override
	public FavoriteListResponse findOneForOwner(Long ownerId, Long listId) {
		FavoriteList list = getOwnedListOrThrow(ownerId, listId);
		return FavoriteListMapper.toResponse(list, favoriteListItemRepository.countByListId(list.getId()));
	}

	@Override
	@Transactional
	public void delete(Long ownerId, Long listId) {
		FavoriteList list = getOwnedListOrThrow(ownerId, listId);
		favoriteListRepository.delete(list);
		log.info("favorite list deleted id={} ownerId={}", listId, ownerId);
	}

	/**
	 * Scoped by (id, ownerId) in the query itself (see
	 * FavoriteListRepository.findByIdAndOwnerId) so a non-owner's request finds
	 * nothing and gets a 404 — never a 403 that would confirm the list exists.
	 */
	private FavoriteList getOwnedListOrThrow(Long ownerId, Long listId) {
		return favoriteListRepository.findByIdAndOwnerId(listId, ownerId)
				.orElseThrow(() -> new ResourceNotFoundException("Favorite list %d not found".formatted(listId)));
	}
}
