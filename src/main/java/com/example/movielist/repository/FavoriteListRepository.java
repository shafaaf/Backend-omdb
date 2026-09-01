package com.example.movielist.repository;

import com.example.movielist.entity.FavoriteList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for FavoriteList — a user's named list of movies. */
public interface FavoriteListRepository extends JpaRepository<FavoriteList, Long> {

	List<FavoriteList> findByOwnerId(Long ownerId);

	/** Finds a list only if it belongs to that owner — a different user's list just isn't found. */
	Optional<FavoriteList> findByIdAndOwnerId(Long id, Long ownerId);

	boolean existsByOwnerIdAndName(Long ownerId, String name);
}
