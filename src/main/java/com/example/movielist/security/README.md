# security/

Everything JWT- and auth-mechanism-specific. `config/SecurityConfig` wires
these together; this package is where the actual logic lives.

| File | What it does |
|---|---|
| `JwtService.java` | Signs and parses access JWTs directly via the `jjwt` library (not `spring-boot-starter-oauth2-resource-server`), so the mechanism stays visible instead of framework-hidden. `generateAccessToken(userId, email)` → `AccessToken`; `tryParse(token)` → `Optional<Claims>` (empty on anything invalid/expired/malformed). |
| `AccessToken.java` | Small record: the compact JWT string + its own `jti` + `expiresAt`, so callers don't have to re-parse a token they just created. |
| `TokenHasher.java` | `generateOpaqueToken()` — a random string used as the *refresh* token (not a JWT). `sha256Hex(value)` — hashes it for storage (delegates to `util/HashUtil`); only the hash is ever persisted (`entity/RefreshToken`). |
| `CustomUserDetails.java` | Adapts our `User` entity to Spring Security's `UserDetails`. Exposes `getId()` so controllers can read `@AuthenticationPrincipal CustomUserDetails principal` and call `principal.getId()` directly, no extra query. |
| `CustomUserDetailsService.java` | `loadUserByUsername(email)` — plugs into Spring Security's standard `AuthenticationManager`/`DaoAuthenticationProvider` machinery, used by `AuthServiceImpl.login`. |
| `JwtAuthenticationFilter.java` | Runs on every request. Reads the `access_token` cookie → parses it → checks the `jti` isn't blacklisted (`TokenBlacklistRepository` — this check is what makes logout actually work) → re-fetches the user by id (catches a deleted/deactivated account) → populates `SecurityContextHolder`. Never rejects a request itself; just leaves the context empty on any failure and lets `SecurityConfig`'s authorization rules decide what that means. |
| `CsrfHeaderFilter.java` | Rejects any non-GET/HEAD/OPTIONS request missing an `X-Requested-With` header with a `403` — the CSRF mitigation used instead of Spring's form-oriented CSRF-token machinery (a cross-site request can't attach a custom header). |
| `AuthCookies.java` | Builds the httpOnly/Secure/SameSite=Lax cookies for both tokens (`access(...)`, `refresh(...)`) and their logout-time clearing versions (`clearAccess()`, `clearRefresh()`). The one place cookie attributes are decided. |
