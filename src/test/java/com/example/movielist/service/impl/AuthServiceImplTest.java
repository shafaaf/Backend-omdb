package com.example.movielist.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.movielist.config.JwtProperties;
import com.example.movielist.dto.request.LoginRequest;
import com.example.movielist.dto.request.SignupRequest;
import com.example.movielist.entity.RefreshToken;
import com.example.movielist.entity.User;
import com.example.movielist.exception.DuplicateResourceException;
import com.example.movielist.exception.InvalidCredentialsException;
import com.example.movielist.repository.RefreshTokenRepository;
import com.example.movielist.repository.TokenBlacklistRepository;
import com.example.movielist.repository.UserRepository;
import com.example.movielist.security.AccessToken;
import com.example.movielist.security.JwtService;
import com.example.movielist.service.AuthResult;
import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private RefreshTokenRepository refreshTokenRepository;
	@Mock
	private TokenBlacklistRepository tokenBlacklistRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private AuthenticationManager authenticationManager;
	@Mock
	private JwtService jwtService;

	private AuthServiceImpl service;

	@BeforeEach
	void setUp() {
		JwtProperties jwtProperties = new JwtProperties("test-secret", 900, 604_800);
		service = new AuthServiceImpl(
				userRepository, refreshTokenRepository, tokenBlacklistRepository,
				passwordEncoder, authenticationManager, jwtService, jwtProperties);
	}

	@Test
	void signup_emailAlreadyRegistered_throwsDuplicateResourceException() {
		when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

		SignupRequest request = new SignupRequest("taken@example.com", "password123", "Someone");

		assertThatThrownBy(() -> service.signup(request)).isInstanceOf(DuplicateResourceException.class);
		verify(userRepository, org.mockito.Mockito.never()).save(any());
	}

	@Test
	void signup_newEmail_hashesPasswordAndIssuesTokens() {
		when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
		when(passwordEncoder.encode("password123")).thenReturn("bcrypt-hash");
		when(jwtService.generateAccessToken(any(), anyString()))
				.thenReturn(new AccessToken("jwt-value", "jti-1", Instant.now().plusSeconds(900)));

		AuthResult result = service.signup(new SignupRequest("new@example.com", "password123", "New Person"));

		assertThat(result.user().email()).isEqualTo("new@example.com");
		assertThat(result.accessToken()).isEqualTo("jwt-value");
		verify(refreshTokenRepository).save(any());
	}

	@Test
	void login_badCredentials_throwsGenericInvalidCredentialsException() {
		when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("nope"));

		LoginRequest request = new LoginRequest("nobody@example.com", "wrong");

		assertThatThrownBy(() -> service.login(request))
				.isInstanceOf(InvalidCredentialsException.class)
				.hasMessage("Invalid email or password");
	}

	@Test
	void refresh_revokedToken_throwsInvalidCredentialsException() {
		User user = new User("owner@example.com", "hash", "Owner");
		RefreshToken revoked = new RefreshToken("hash", user, Instant.now(), Instant.now().plusSeconds(60));
		revoked.revoke();

		when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(revoked));

		assertThatThrownBy(() -> service.refresh("raw-refresh-token"))
				.isInstanceOf(InvalidCredentialsException.class);
	}

	@Test
	void refresh_validToken_rotatesAndIssuesNewPair() {
		User user = new User("owner@example.com", "hash", "Owner");
		setId(user, 1L);
		RefreshToken usable = new RefreshToken("hash", user, Instant.now(), Instant.now().plusSeconds(600));

		when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(usable));
		when(jwtService.generateAccessToken(any(), anyString()))
				.thenReturn(new AccessToken("new-jwt", "jti-2", Instant.now().plusSeconds(900)));

		AuthResult result = service.refresh("raw-refresh-token");

		assertThat(usable.isUsable()).isFalse(); // rotation revokes the presented token
		assertThat(result.accessToken()).isEqualTo("new-jwt");
		verify(refreshTokenRepository, org.mockito.Mockito.times(2)).save(any()); // revoke old + save new
	}

	private void setId(Object entity, Long id) {
		try {
			Field field = entity.getClass().getSuperclass().getDeclaredField("id");
			field.setAccessible(true);
			field.set(entity, id);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}
}
