# config/

`@Configuration`/`@ConfigurationProperties` classes — application wiring, no
business logic.

| File | What it configures |
|---|---|
| `SecurityConfig.java` | The `SecurityFilterChain` bean: stateless sessions, CSRF disabled (with the reasoning in a comment — see `security/CsrfHeaderFilter`), `permitAll` on signup/login/refresh + movie browsing, everything else authenticated. Also the JSON `AuthenticationEntryPoint`/`AccessDeniedHandler` and the `AuthenticationManager` bean. |
| `CorsConfig.java` | `CorsConfigurationSource` scoped to the Vite dev origin (`http://localhost:5173`) with `allowCredentials(true)`, so the browser sends/receives the auth cookies cross-origin. |
| `JpaAuditingConfig.java` | Just `@EnableJpaAuditing`. Kept in its own tiny class rather than on `MovielistApplication` — otherwise narrow test slices like `@WebMvcTest` (which don't load JPA at all) fail with "JPA metamodel must not be empty". |
| `PasswordEncoderConfig.java` | The `PasswordEncoder` (`BCryptPasswordEncoder`) bean — a plain example of Spring's container-managed Singleton (contrast with `util/OmdbRateLimiterSingleton`, a hand-rolled one). |
| `RestClientConfig.java` | The `RestClient` bean used by `client/OmdbClientImpl`, pre-configured with OMDb's base URL. |
| `JwtProperties.java` | `@ConfigurationProperties(prefix = "movielist.jwt")` — `secret`, `accessTokenTtlSeconds`, `refreshTokenTtlSeconds`. `secret` is `@NotBlank` with no default, so a missing `JWT_SECRET` env var fails startup immediately. |
| `OmdbProperties.java` | `@ConfigurationProperties(prefix = "omdb.api")` — `key`, `baseUrl`. Same fail-fast pattern as `JwtProperties` for a missing `OMDB_API_KEY`. |
