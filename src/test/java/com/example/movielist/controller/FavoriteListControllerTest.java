package com.example.movielist.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.movielist.dto.response.FavoriteListResponse;
import com.example.movielist.entity.User;
import com.example.movielist.security.CsrfHeaderFilter;
import com.example.movielist.security.CustomUserDetails;
import com.example.movielist.security.JwtAuthenticationFilter;
import com.example.movielist.service.FavoriteListService;
import java.lang.reflect.Field;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * A slice test: only the web layer boots (no real database), FavoriteListService
 * is mocked. This is the payoff of FavoriteListService being an interface: the
 * controller can be tested completely independent of persistence.
 *
 * Two Spring Security wrinkles specific to slice-testing a controller that uses
 * @AuthenticationPrincipal with a custom type:
 *  1. excludeFilters keeps the real JwtAuthenticationFilter/CsrfHeaderFilter out
 *     of this slice entirely — they're @Component-scanned Filter beans that would
 *     otherwise get pulled in (needing their real, not-part-of-this-slice
 *     dependencies) or, if merely mocked, would silently swallow every request
 *     (a Mockito-mocked Filter's doFilter is a no-op that never calls the chain).
 *  2. A minimal local @EnableWebSecurity config with the real (non-mocked)
 *     filter chain running is required so SecurityContextHolderFilter actually
 *     applies the authentication that SecurityMockMvcRequestPostProcessors sets
 *     up — with the whole filter chain disabled, nothing ever populates
 *     SecurityContextHolder and @AuthenticationPrincipal resolves to null.
 */
@WebMvcTest(
		controllers = FavoriteListController.class,
		excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {JwtAuthenticationFilter.class, CsrfHeaderFilter.class}))
class FavoriteListControllerTest {

	@TestConfiguration
	@EnableWebSecurity
	static class MinimalSecurityConfig {
		@Bean
		SecurityFilterChain permitAllChain(HttpSecurity http) throws Exception {
			http.csrf(AbstractHttpConfigurer::disable) // out of scope for this slice — see SecurityConfig for the real policy
					.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
			return http.build();
		}
	}

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private FavoriteListService favoriteListService;

	@Test
	void create_validRequest_returns201WithLocationHeader() throws Exception {
		FavoriteListResponse response = new FavoriteListResponse(1L, "My Favorites", 0, Instant.now());
		when(favoriteListService.create(eq(1L), any())).thenReturn(response);

		mockMvc.perform(post("/api/lists")
						.with(authentication(authenticationToken()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"My Favorites\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("My Favorites"));
	}

	@Test
	void create_blankName_returns400WithFieldError() throws Exception {
		mockMvc.perform(post("/api/lists")
						.with(authentication(authenticationToken()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.name").exists());
	}

	private UsernamePasswordAuthenticationToken authenticationToken() {
		User user = new User("owner@example.com", "hash", "Owner");
		setId(user, 1L);
		CustomUserDetails principal = new CustomUserDetails(user);
		return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
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
