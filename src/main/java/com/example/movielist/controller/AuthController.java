package com.example.movielist.controller;

import com.example.movielist.dto.request.LoginRequest;
import com.example.movielist.dto.request.SignupRequest;
import com.example.movielist.dto.response.AuthResponse;
import com.example.movielist.dto.response.UserResponse;
import com.example.movielist.exception.InvalidCredentialsException;
import com.example.movielist.security.AuthCookies;
import com.example.movielist.security.CustomUserDetails;
import com.example.movielist.service.AuthResult;
import com.example.movielist.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Signup, login, token refresh, logout, and "who am I" — the whole auth surface. */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final AuthCookies authCookies;

	/** Creates a new account and logs the user in (sets auth cookies). */
	@PostMapping("/signup")
	public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
		return withTokenCookies(authService.signup(request), HttpStatus.CREATED);
	}

	/** Logs an existing user in and sets auth cookies. */
	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
		return withTokenCookies(authService.login(request), HttpStatus.OK);
	}

	/** Exchanges the refresh-token cookie for a new access/refresh pair. */
	@PostMapping("/refresh")
	public ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
		String rawRefreshToken = extractCookie(request, AuthCookies.REFRESH_TOKEN)
				.orElseThrow(() -> new InvalidCredentialsException("No refresh token presented"));
		return withTokenCookies(authService.refresh(rawRefreshToken), HttpStatus.OK);
	}

	/** Invalidates the current session (blacklists the access token, revokes the refresh token) and clears cookies. */
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(HttpServletRequest request) {
		String rawAccessToken = extractCookie(request, AuthCookies.ACCESS_TOKEN).orElse(null);
		String rawRefreshToken = extractCookie(request, AuthCookies.REFRESH_TOKEN).orElse(null);
		authService.logout(rawAccessToken, rawRefreshToken);

		return ResponseEntity.noContent()
				.header(HttpHeaders.SET_COOKIE, authCookies.clearAccess().toString())
				.header(HttpHeaders.SET_COOKIE, authCookies.clearRefresh().toString())
				.build();
	}

	/** Returns the currently authenticated user, or 401 if there isn't one. */
	@GetMapping("/me")
	public ResponseEntity<UserResponse> me(@AuthenticationPrincipal CustomUserDetails principal) {
		return ResponseEntity.ok(authService.getCurrentUser(principal.getId()));
	}

	/** Builds the response body and sets both auth cookies — shared by signup/login/refresh. */
	private ResponseEntity<AuthResponse> withTokenCookies(AuthResult result, HttpStatus status) {
		AuthResponse body = AuthResponse.builder()
				.user(result.user())
				.accessTokenExpiresAt(result.accessTokenExpiresAt())
				.build();

		return ResponseEntity.status(status)
				.header(HttpHeaders.SET_COOKIE, authCookies.access(result.accessToken(), result.accessTokenExpiresAt()).toString())
				.header(HttpHeaders.SET_COOKIE, authCookies.refresh(result.refreshToken(), result.refreshTokenExpiresAt()).toString())
				.body(body);
	}

	/** Pulls a named cookie's value out of the request, if present. */
	private Optional<String> extractCookie(HttpServletRequest request, String name) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return Optional.empty();
		}
		return Arrays.stream(cookies)
				.filter(cookie -> name.equals(cookie.getName()))
				.map(Cookie::getValue)
				.findFirst();
	}
}
