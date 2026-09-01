package com.example.movielist.repository;

import com.example.movielist.entity.FavoriteListItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Data access for FavoriteListItem — one movie's membership in one list. */
public interface FavoriteListItemRepository extends JpaRepository<FavoriteListItem, Long> {

	boolean existsByListIdAndMovieId(Long listId, Long movieId);

	long countByListId(Long listId);

	Optional<FavoriteListItem> findByListIdAndMovieId(Long listId, Long movieId);

	/** Same as above but loads each item's Movie in the same query (avoids extra queries per item). */
	@Query("SELECT i FROM FavoriteListItem i JOIN FETCH i.movie WHERE i.list.id = :listId ORDER BY i.addedAt DESC")
	List<FavoriteListItem> findByListIdWithMovie(@Param("listId") Long listId);
}
