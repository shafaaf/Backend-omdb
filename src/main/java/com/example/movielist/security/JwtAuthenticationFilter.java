package com.example.movielist.security;

import com.example.movielist.repository.TokenBlacklistRepository;
import com.example.movielist.repository.UserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Populates the SecurityContext from the access_token cookie on every request.
 * Deliberately never rejects a request itself on a missing/invalid/blacklisted
 * token — it just leaves the SecurityContext empty and lets
 * authorizeHttpRequests (SecurityConfig) + the configured entry point decide
 * what that means for the specific endpoint (permitAll routes still work with no
 * cookie at all; protected routes get a 401 from the entry point).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;
	private final TokenBlacklistRepository tokenBlacklistRepository;
	private final UserRepository userRepository;

	/** Runs on every request: tries to authenticate from the access-token cookie, then always continues the chain. */
	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		extractAccessTokenCookie(request)
				.flatMap(jwtService::tryParse)
				.filter(this::notBlacklisted)
				.flatMap(this::loadPrincipal)
				.ifPresent(principal -> {
					var authToken = new UsernamePasswordAuthenticationToken(
							principal, null, principal.getAuthorities());
					authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
					SecurityContextHolder.getContext().setAuthentication(authToken);
				});

		filterChain.doFilter(request, response);
	}

	/**
	 * True unless this token's jti has been logged out.
	 *
	 * This existsById check against the jti claim is the actual enforcement point for
	 * "logout" — the token itself is still cryptographically valid and unexpired, but a
	 * blacklisted jti means AuthServiceImpl.logout() has since disowned it.
	 */
	private boolean notBlacklisted(Claims claims) {
		boolean blacklisted = tokenBlacklistRepository.existsById(claims.getId());
		if (blacklisted) {
			log.debug("rejected blacklisted access token jti={}", claims.getId());
		}
		return !blacklisted;
	}

	/**
	 * Loads the CustomUserDetails principal for the token's subject (user id), if that user still exists.
	 *
	 * Re-fetching the user by id (rather than trusting the email claim alone) is a
	 * small deliberate DB hit per request: it catches an account that was deleted or
	 * deactivated since the token was issued, which the token's own claims can't know.
	 */
	private Optional<CustomUserDetails> loadPrincipal(Claims claims) {
		Long userId = jwtService.extractUserId(claims);
		return userRepository.findById(userId).map(CustomUserDetails::new);
	}

	/** Pulls the raw access_token cookie value out of the request, if present. */
	private Optional<String> extractAccessTokenCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return Optional.empty();
		}
		return Arrays.stream(cookies)
				.filter(cookie -> AuthCookies.ACCESS_TOKEN.equals(cookie.getName()))
				.map(Cookie::getValue)
				.findFirst();
	}
}
