package com.example.movielist.repository;

import com.example.movielist.entity.TokenBlacklistEntry;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for TokenBlacklistEntry — logged-out access tokens, keyed by their jti. */
public interface TokenBlacklistRepository extends JpaRepository<TokenBlacklistEntry, String> {
	// existsById(jti) from JpaRepository is exactly what JwtAuthenticationFilter needs;
	// no custom query method required since jti is already the primary key.
}
