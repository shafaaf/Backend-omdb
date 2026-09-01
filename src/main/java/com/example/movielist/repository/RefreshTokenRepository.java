package com.example.movielist.repository;

import com.example.movielist.entity.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for RefreshToken — issued refresh tokens, looked up by their hash. */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	Optional<RefreshToken> findByTokenHash(String tokenHash);
}
