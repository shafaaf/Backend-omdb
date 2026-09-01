package com.example.movielist.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.movielist.entity.FavoriteList;
import com.example.movielist.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies the DB-level unique constraint on (owner_id, name) — the layer that
 * actually enforces "no duplicate list name per user" even if application code
 * has a bug, or two requests race each other. See entity.FavoriteList and
 * entity.IdempotencyRecord for the other unique constraints in this app that
 * follow the same pattern (verified once here rather than duplicated per table).
 */
@DataJpaTest
@ActiveProfiles("test")
class FavoriteListRepositoryTest {

	@Autowired
	private TestEntityManager entityManager;

	@Autowired
	private FavoriteListRepository favoriteListRepository;

	@Test
	void duplicateNameForSameOwner_violatesUniqueConstraint() {
		User owner = entityManager.persistAndFlush(new User("owner@example.com", "hash", "Owner"));
		favoriteListRepository.saveAndFlush(new FavoriteList("Favorites", owner));

		FavoriteList duplicate = new FavoriteList("Favorites", owner);

		assertThatThrownBy(() -> favoriteListRepository.saveAndFlush(duplicate))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void sameNameForDifferentOwners_isAllowed() {
		User ownerA = entityManager.persistAndFlush(new User("a@example.com", "hash", "A"));
		User ownerB = entityManager.persistAndFlush(new User("b@example.com", "hash", "B"));

		favoriteListRepository.saveAndFlush(new FavoriteList("Favorites", ownerA));
		favoriteListRepository.saveAndFlush(new FavoriteList("Favorites", ownerB));

		assertThat(favoriteListRepository.findByOwnerId(ownerA.getId())).hasSize(1);
		assertThat(favoriteListRepository.findByOwnerId(ownerB.getId())).hasSize(1);
	}
}
