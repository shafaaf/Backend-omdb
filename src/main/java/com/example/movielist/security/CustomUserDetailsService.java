package com.example.movielist.security;

import com.example.movielist.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Plugs into Spring Security's standard authentication machinery: with this bean
 * and a PasswordEncoder bean both present, Spring Boot auto-configures a
 * DaoAuthenticationProvider that AuthenticationManager.authenticate(...) uses in
 * AuthServiceImpl.login — no manual password comparison needed there.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	/** Looks up a user by email (Spring Security calls the parameter "username"); throws if none found. */
	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		return userRepository.findByEmail(email)
				.map(CustomUserDetails::new)
				.orElseThrow(() -> new UsernameNotFoundException("No user with email " + email));
	}
}
