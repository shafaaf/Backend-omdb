package com.example.movielist.service;

import com.example.movielist.dto.request.LoginRequest;
import com.example.movielist.dto.request.SignupRequest;
import com.example.movielist.dto.response.UserResponse;

/** The auth business logic behind AuthController — signup, login, token refresh, logout. */
public interface AuthService {

	/** Creates a new account and issues a fresh access/refresh token pair. */
	AuthResult signup(SignupRequest request);

	/** Verifies credentials and issues a fresh access/refresh token pair. */
	AuthResult login(LoginRequest request);

	/** Rotates the refresh token: the old one is revoked, a new pair is issued. */
	AuthResult refresh(String rawRefreshToken);

	/**
	 * Blacklists the current access token's jti and revokes the refresh token.
	 * Either argument may be null/blank (e.g. a client that already lost its
	 * cookies calling logout again) — this is idempotent and never throws for that.
	 */
	void logout(String rawAccessToken, String rawRefreshToken);

	/** Returns the profile of the given user id, for GET /api/auth/me. */
	UserResponse getCurrentUser(Long userId);
}
