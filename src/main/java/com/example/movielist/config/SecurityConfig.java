package com.example.movielist.config;

import com.example.movielist.dto.response.ErrorResponse;
import com.example.movielist.security.CsrfHeaderFilter;
import com.example.movielist.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Ties together the pieces built across this study project's "auth" step:
 * stateless sessions (the server holds no session state — see JwtAuthenticationFilter
 * for how each request re-establishes identity from its own JWT), a permitAll list
 * limited to signup/login/refresh and read-only movie browsing (mirrors real IMDb —
 * browsing doesn't require an account), and everything else authenticated.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final CsrfHeaderFilter csrfHeaderFilter;
	private final ObjectMapper objectMapper;

	/** Defines the app's HTTP security rules: which routes are public, session policy, filters, error handling. */
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.cors(cors -> {}) // picks up the CorsConfigurationSource bean from CorsConfig
				// Spring's built-in CSRF protection is designed around server-rendered forms
				// carrying a hidden token; this is a stateless JSON API consumed by an SPA.
				// Disabled here in favor of the lighter SameSite=Lax cookie attribute (AuthCookies)
				// + strict single-origin CORS with credentials (CorsConfig) + a required custom
				// header on state-changing requests (CsrfHeaderFilter) — a forged cross-site
				// request can't attach that header, so it never reaches business logic.
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(
								"/api/auth/signup", "/api/auth/login", "/api/auth/refresh")
						.permitAll()
						.requestMatchers(HttpMethod.GET, "/api/movies/**").permitAll()
						.requestMatchers("/h2-console/**").permitAll()
						.anyRequest().authenticated())
				.headers(headers -> headers.frameOptions(frame -> frame.sameOrigin())) // needed for the H2 console (dev only)
				.exceptionHandling(handling -> handling
						.authenticationEntryPoint((request, response, ex) ->
								writeError(response, HttpStatus.UNAUTHORIZED, "Authentication required", request.getRequestURI()))
						.accessDeniedHandler((request, response, ex) ->
								writeError(response, HttpStatus.FORBIDDEN, "Access denied", request.getRequestURI())))
				.addFilterBefore(csrfHeaderFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	/** Exposes Spring Security's AuthenticationManager as a bean so AuthServiceImpl.login can use it directly. */
	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	/** Writes a JSON ErrorResponse directly to the response — used by the entry point/access-denied handlers above. */
	private void writeError(HttpServletResponse response, HttpStatus status, String message, String path)
			throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getWriter(), ErrorResponse.of(status, message, path));
	}
}
