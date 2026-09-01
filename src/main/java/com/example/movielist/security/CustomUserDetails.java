package com.example.movielist.security;

import com.example.movielist.entity.User;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Adapts our User entity to Spring Security's UserDetails. Exposes getId() so
 * controllers can pull the authenticated user's id straight off the principal
 * (@AuthenticationPrincipal CustomUserDetails) instead of re-querying by email.
 */
@Getter
public class CustomUserDetails implements UserDetails {

	private final Long id;
	private final String email;
	private final String displayName;
	private final String passwordHash;

	/** Copies the fields Spring Security's UserDetails needs out of a User entity. */
	public CustomUserDetails(User user) {
		this.id = user.getId();
		this.email = user.getEmail();
		this.displayName = user.getDisplayName();
		this.passwordHash = user.getPasswordHash();
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(); // no role/authority model in this showcase — every authenticated user has equal access
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return email;
	}
}
