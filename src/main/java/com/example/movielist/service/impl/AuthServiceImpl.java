package com.example.movielist.service.impl;

import com.example.movielist.config.JwtProperties;
import com.example.movielist.dto.request.LoginRequest;
import com.example.movielist.dto.request.SignupRequest;
import com.example.movielist.dto.response.UserResponse;
import com.example.movielist.entity.RefreshToken;
import com.example.movielist.entity.TokenBlacklistEntry;
import com.example.movielist.entity.User;
import com.example.movielist.exception.DuplicateResourceException;
import com.example.movielist.exception.InvalidCredentialsException;
import com.example.movielist.exception.ResourceNotFoundException;
import com.example.movielist.mapper.UserMapper;
import com.example.movielist.repository.RefreshTokenRepository;
import com.example.movielist.repository.TokenBlacklistRepository;
import com.example.movielist.repository.UserRepository;
import com.example.movielist.security.AccessToken;
import com.example.movielist.security.JwtService;
import com.example.movielist.security.TokenHasher;
import com.example.movielist.service.AuthResult;
import com.example.movielist.service.AuthService;
import io.jsonwebtoken.Claims;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** See AuthService for what each method does; the interesting logic (credential
 *  checking, token rotation, blacklisting) is documented inline below. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

	private final UserRepository userRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final TokenBlacklistRepository tokenBlacklistRepository;
	private final PasswordEncoder passwordEncoder;
	private final AuthenticationManager authenticationManager;
	private final JwtService jwtService;
	private final JwtProperties jwtProperties;

	@Override
	public AuthResult signup(SignupRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			log.warn("signup attempted with already-registered email");
			throw new DuplicateResourceException("An account with this email already exists");
		}

		User user = new User(request.email(), passwordEncoder.encode(request.password()), request.displayName());
		userRepository.save(user);
		log.info("user signed up id={}", user.getId());

		return issueTokens(user);
	}

	@Override
	public AuthResult login(LoginRequest request) {
		try {
			// Delegates credential checking to Spring Security's standard machinery:
			// CustomUserDetailsService loads the user, the auto-configured
			// DaoAuthenticationProvider compares the password via the PasswordEncoder bean.
			// No manual passwordEncoder.matches(...) call needed here.
			authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(request.email(), request.password()));
		} catch (AuthenticationException e) {
			// Same message whether the email doesn't exist or the password is wrong —
			// distinguishing the two would tell an attacker which emails are registered.
			log.warn("login failed for supplied email");
			throw new InvalidCredentialsException("Invalid email or password");
		}

		User user = userRepository.findByEmail(request.email())
				.orElseThrow(() -> new IllegalStateException("Authenticated user vanished mid-request"));
		log.info("user logged in id={}", user.getId());

		return issueTokens(user);
	}

	@Override
	public AuthResult refresh(String rawRefreshToken) {
		RefreshToken existing = refreshTokenRepository.findByTokenHash(TokenHasher.sha256Hex(rawRefreshToken))
				.orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));

		if (!existing.isUsable()) {
			throw new InvalidCredentialsException("Refresh token is expired or has been revoked");
		}

		// Rotation: the presented refresh token is single-use. Revoking it here means a
		// stolen-and-replayed old token fails on its second use even if the legitimate
		// client already rotated past it.
		existing.revoke();
		refreshTokenRepository.save(existing);

		return issueTokens(existing.getUser());
	}

	@Override
	public void logout(String rawAccessToken, String rawRefreshToken) {
		if (rawAccessToken != null && !rawAccessToken.isBlank()) {
			jwtService.tryParse(rawAccessToken).ifPresent(this::blacklist);
		}
		if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
			refreshTokenRepository.findByTokenHash(TokenHasher.sha256Hex(rawRefreshToken))
					.ifPresent(token -> {
						token.revoke();
						refreshTokenRepository.save(token);
					});
		}
		log.info("logout processed");
	}

	@Override
	@Transactional(readOnly = true)
	public UserResponse getCurrentUser(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		return UserMapper.toResponse(user);
	}

	/** Adds the given access token's jti to the blacklist, mirroring its own expiry. */
	private void blacklist(Claims accessTokenClaims) {
		String jti = accessTokenClaims.getId();
		Instant expiresAt = accessTokenClaims.getExpiration().toInstant();
		tokenBlacklistRepository.save(new TokenBlacklistEntry(jti, expiresAt));
	}

	/** Generates and persists a fresh access/refresh token pair for the given user. */
	private AuthResult issueTokens(User user) {
		AccessToken access = jwtService.generateAccessToken(user.getId(), user.getEmail());

		String rawRefresh = TokenHasher.generateOpaqueToken();
		Instant refreshExpiresAt = Instant.now().plusSeconds(jwtProperties.refreshTokenTtlSeconds());
		refreshTokenRepository.save(new RefreshToken(
				TokenHasher.sha256Hex(rawRefresh), user, Instant.now(), refreshExpiresAt));

		return new AuthResult(
				UserMapper.toResponse(user),
				access.value(), access.expiresAt(),
				rawRefresh, refreshExpiresAt);
	}
}
