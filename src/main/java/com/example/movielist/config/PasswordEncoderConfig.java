package com.example.movielist.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * A @Bean method with the default (singleton) scope is Spring's container-managed
 * take on the Singleton pattern: the ApplicationContext creates this
 * PasswordEncoder exactly once and hands the same instance to every class that
 * declares it as a constructor dependency (AuthServiceImpl, CustomUserDetailsService
 * via the auto-configured DaoAuthenticationProvider, etc). Contrast with
 * util.OmdbRateLimiterSingleton, which implements the classic Gang-of-Four
 * Singleton by hand, entirely outside the DI container.
 */
@Configuration
public class PasswordEncoderConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
