package com.example.movielist.security;

import com.example.movielist.dto.response.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Cheap, well-known CSRF mitigation for a cookie-authenticated SPA that doesn't
 * use Spring's form-oriented CSRF token machinery (disabled in SecurityConfig,
 * with the full reasoning there): a simple cross-site <form> POST or <img>-style
 * request can't attach arbitrary headers, only simple ones. Requiring this header
 * on every state-changing request means such a forged request never reaches the
 * business logic, even though the browser would still attach the SameSite=Lax
 * cookies for a top-level navigation. The frontend sets this on every mutating
 * call — see frontend/src/lib/api.ts.
 */
@Component
@RequiredArgsConstructor
public class CsrfHeaderFilter extends OncePerRequestFilter {

	private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");
	private static final String REQUIRED_HEADER = "X-Requested-With";

	private final ObjectMapper objectMapper;

	/** Rejects any non-GET/HEAD/OPTIONS request that's missing the required header; otherwise passes it through. */
	@Override
	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		boolean isStateChanging = !SAFE_METHODS.contains(request.getMethod());
		if (isStateChanging && request.getHeader(REQUIRED_HEADER) == null) {
			// Written directly rather than via response.sendError(): sendError triggers a
			// container /error dispatch that re-enters the whole security filter chain on a
			// fresh (unauthenticated-looking) request, which clobbers this 403 into a
			// misleading 401 from the authentication entry point instead.
			response.setStatus(HttpStatus.FORBIDDEN.value());
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			objectMapper.writeValue(response.getWriter(), ErrorResponse.of(
					HttpStatus.FORBIDDEN, "Missing required " + REQUIRED_HEADER + " header", request.getRequestURI()));
			return;
		}
		filterChain.doFilter(request, response);
	}
}
