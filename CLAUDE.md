# movielist — design & study reference

Purpose of this project: a small, readable, production-style codebase built
to demonstrate current backend engineering practices to someone studying for
backend interviews (troubleshoot/fix-a-bug-in-an-existing-codebase style
rounds, AI-assist allowed). Feature scope is intentionally small — sign up /
sign in / sign out, create favorite lists, add/remove movies — so the depth
budget goes into engineering practices, not feature count.

Explicitly **out of scope**: observability/metrics/tracing (no Actuator or
monitoring stack). Explicitly **in scope**: logging (SLF4J), global exception
handling, unit testing (JUnit5 + Mockito), and full
controller → service → repository → DTO layering.

## Confirmed decisions

- **Movie data**: fetched from the OMDb API, cached/persisted locally into a
  `Movie` table — the frontend never calls OMDb directly. Requires a free key
  from omdbapi.com (see README setup).
- **Database**: H2. File-based for dev (`jdbc:h2:file:./data/movielist`, data
  survives restarts so the app can be demoed without reseeding). In-memory
  for tests (`create-drop`), for clean repeatable runs.
- **Frontend**: React (Vite + TS), plain CSS, no state library — minimal UI:
  login/signup, search/browse, create/view lists, add/remove movies.

## Repo layout

```
backend-practise/
├── pom.xml, mvnw, mvnw.cmd, .mvn/
├── src/main/java/com/example/movielist/
├── src/main/resources/application.yml
├── src/test/java/com/example/movielist/
├── .env.example                # JWT_SECRET=, OMDB_API_KEY= (tracked); .env is gitignored
├── README.md, CLAUDE.md
└── frontend/                   # Vite + React + TS, plain CSS, react-router-dom
```

Two independent dev servers: backend on 8080, frontend on 5173 (CORS
configured, `allowCredentials(true)`).

## Domain model

| Entity | Key fields | Relationships |
|---|---|---|
| `BaseEntity` (`@MappedSuperclass`) | id, createdAt, updatedAt (JPA Auditing) | extended by Movie, FavoriteList, FavoriteListItem |
| `User` | id, email (unique), passwordHash, displayName | 1─* FavoriteList, 1─* RefreshToken |
| `Movie` | id, externalId (OMDb imdbID, unique), title, year, posterUrl, plot, genre, director, imdbRating, lastRefreshedAt | 1─* FavoriteListItem (shared cache, not user-owned) |
| `FavoriteList` | id, name, owner (FK User); unique(owner_id, name) | *─1 User, 1─* FavoriteListItem |
| `FavoriteListItem` | id, list (FK), movie (FK), addedAt; unique(list_id, movie_id) | *─1 FavoriteList, *─1 Movie |
| `RefreshToken` | id, tokenHash (SHA-256), user (FK), issuedAt, expiresAt, revoked | *─1 User |
| `TokenBlacklistEntry` | jti (PK), blacklistedAt, expiresAt | standalone, keyed by JWT `jti` claim |
| `IdempotencyRecord` | id, idempotencyKey, userId, endpointPath, requestHash, responseStatus, responseBody; unique(key, userId, endpointPath) | logical FK to User |

`FavoriteList.addItem(item)` enforces "no duplicate movie" as a domain
invariant, backed by (not replacing) the DB unique constraint — plus the
idempotency key at the transport layer. **Three mechanisms, each guarding a
different failure mode**: client bug (domain invariant), race condition (DB
constraint), network retry (idempotency key).

## Package layout (`com.example.movielist`)

```
config/       SecurityConfig, CorsConfig, RestClientConfig, JpaAuditingConfig, PasswordEncoderConfig
controller/   AuthController, MovieController, FavoriteListController, FavoriteListItemController
service/      interfaces: AuthService, MovieService, FavoriteListService, FavoriteListItemService, IdempotencyService
service/impl/ *Impl, constructor-injected via @RequiredArgsConstructor + private final fields
repository/   Spring Data JPA repos for each entity
entity/       User, Movie, FavoriteList, FavoriteListItem, RefreshToken, TokenBlacklistEntry, IdempotencyRecord, BaseEntity
dto/request/  SignupRequest, LoginRequest, CreateFavoriteListRequest, AddMovieToListRequest — jakarta.validation annotations
dto/response/ UserResponse, AuthResponse, MovieResponse, FavoriteListResponse, FavoriteListItemResponse, ErrorResponse
mapper/       UserMapper, MovieMapper, FavoriteListMapper — hand-written static methods, not MapStruct
security/     JwtService, JwtAuthenticationFilter, CustomUserDetailsService
exception/    GlobalExceptionHandler (@RestControllerAdvice), ResourceNotFoundException, DuplicateResourceException, InvalidCredentialsException, IdempotencyConflictException, ExternalApiException
client/       OmdbClient (interface) + OmdbClientImpl, OmdbMovieResponse (raw external DTO)
util/         OmdbRateLimiterSingleton (enum-based GoF singleton, outside DI), OmdbApiRequest (hand-rolled Builder)
```

### Deliberate teaching tradeoffs (each documented inline in code too)

- **Mappers are hand-written, not MapStruct.** MapStruct is more "production"
  (less boilerplate, compile-time safety) but hides the field-by-field
  mapping behind annotation processing — worse for a codebase meant to be
  *read*. Manual mapping chosen for legibility; MapStruct noted as the real
  alternative.
- **Service interfaces + `*Impl` even with one implementation.** Somewhat
  old-school (modern Spring guidance often skips the interface when there's
  a single implementation — YAGNI). Kept deliberately because "why
  interface+impl here?" is a common interview probe, and it demonstrates DI
  against an abstraction plus mock-based testing.
- **Two Singleton examples, contrasted directly**: Spring-managed beans
  (container singleton by default scope, e.g. `PasswordEncoder`) vs.
  `OmdbRateLimiterSingleton`, a classic enum-based GoF singleton living
  entirely outside the DI container, used by `OmdbClientImpl` to throttle
  calls to OMDb's free-tier rate limit. A real team would probably still
  make the latter a Spring bean — it's implemented the GoF way here
  specifically so it can be explained by hand.
- **Two Builder examples, contrasted directly**: Lombok `@Builder` on
  `Movie`/DTOs (many optional/nullable OMDb fields) vs. a hand-rolled
  `OmdbApiRequest.Builder` with `build()`-time required-field validation — a
  genuine real-world use case (building an outbound query string).

## Request lifecycle — two illustrative endpoints

### `POST /auth/signup`

`@Valid SignupRequest` (`@Email`, `@NotBlank`, `@Size(min=8)` on password) →
validation failures caught by `GlobalExceptionHandler` (400, per-field
errors). `AuthServiceImpl`: check `existsByEmail` → `DuplicateResourceException`
(409) if taken; hash password via the `PasswordEncoder` singleton bean
(BCrypt); build `User`, save. Issue access JWT (short TTL, embeds `jti`) +
opaque refresh token (hashed, persisted as `RefreshToken`). Map to
`UserResponse`/`AuthResponse` (Lombok builder) — never expose `passwordHash`.
Set httpOnly cookies, return 201. Log at INFO on success (never log
password/token values); duplicate-signup attempts logged at WARN.

### `POST /lists/{listId}/movies` (idempotency showcase)

`@RequestHeader(value="Idempotency-Key", required=false) String key`. Inside
one `@Transactional` service method:

1. If `key` present: look up `IdempotencyRecord` by `(key, userId, endpointPath)`.
   - Found, matching request-fingerprint hash → **replay the stored response,
     skip business logic entirely.**
   - Found, different fingerprint for the same key → `IdempotencyConflictException`
     (409) — same key reused for a logically different request, a bug worth
     surfacing loudly.
   - Not found → proceed.
2. Verify list ownership: mismatch → `ResourceNotFoundException` (**404, not
   403** — avoids leaking existence of other users' lists; deliberate
   security choice).
3. Look up `Movie` by `externalId`; if absent, fetch via `OmdbClient` and
   upsert (this is the "cache results locally" requirement made concrete).
4. `existsByListIdAndMovieId` defense-in-depth check → 409 if already present
   (works even without an idempotency key).
5. Build `FavoriteListItem` (Lombok builder), save.
6. If `key` was present, persist the `IdempotencyRecord`
   **in the same transaction** — atomicity matters: if that write fails, the
   whole operation rolls back together, so you never end up with a committed
   side effect that can't be replay-detected next time.
7. Map to `FavoriteListItemResponse`, return 201 + `Location` header.

**Concurrency note**: two near-simultaneous requests with the same
idempotency key can both pass the "not found" check in step 1 before either
commits. The DB unique constraint on `(idempotency_key, user_id,
endpoint_path)` catches the race — the loser's `DataIntegrityViolationException`
is caught and converted into "re-fetch and return the now-committed record."
This is the realistic "why idempotency is actually hard" teaching moment.

Exceptions across both endpoints bubble to `GlobalExceptionHandler`:
`ResourceNotFoundException`→404, `DuplicateResourceException`→409,
`IdempotencyConflictException`→409, `ExternalApiException` (OMDb down/not
found)→502/404, unhandled→500 with a generic client-facing message (full
stack trace logged server-side only, at ERROR).

## JWT auth design end-to-end

- **Access token**: real JWT (`jjwt` library, HS256 — chosen over
  `spring-boot-starter-oauth2-resource-server` specifically so the
  signing/parsing code is visible and hand-understood rather than
  framework-hidden), ~15 min TTL, claims `sub` (userId), `email`, `iat`,
  `exp`, `jti` (UUID — the hook that makes logout actually work).
- **Refresh token**: **opaque** random value (not a JWT), ~7 day TTL, only
  its SHA-256 hash stored server-side (`RefreshToken` row), rotated on every
  use (old row revoked, new row issued). Chosen over a second JWT because
  refresh calls are low-frequency, so the DB hit is cheap, and it sidesteps
  "how do you revoke a stateless refresh JWT" entirely — you just flag the
  row.
- **Storage — httpOnly cookies, not localStorage**:
  - localStorage is readable by any injected script → any XSS bug becomes
    full account takeover with a typically long-lived token. Rejected.
  - httpOnly cookie is invisible to JS entirely.
  - Tradeoff accepted: cookies reintroduce CSRF risk. Mitigated with
    `SameSite=Lax` + a strict CORS allow-list (`allowCredentials(true)`) +
    requiring a custom header (e.g. `X-Requested-With`) on all
    state-changing requests — a lightweight, well-known SPA defense, lighter
    than Spring's form-oriented CSRF-token machinery. Reasoning documented
    inline in `SecurityConfig`.

### Flows

- **Login** (`POST /auth/login`): look up by email → `InvalidCredentialsException`
  (401) on missing user *or* password mismatch (same generic message either
  way — don't leak which). Issue access+refresh, set cookies.
- **Authenticated request** — `JwtAuthenticationFilter extends OncePerRequestFilter`,
  registered `addFilterBefore(..., UsernamePasswordAuthenticationFilter.class)`:
  1. Read `access_token` cookie; absent → continue unauthenticated (let
     downstream authorization rules 401/403).
  2. Parse/validate signature+expiry; invalid/expired → short-circuit 401
     JSON (signals frontend to hit `/auth/refresh`).
  3. Extract `jti`, check `TokenBlacklistRepository.existsByJti(jti)` — **the
     actual logout enforcement point** despite JWTs being stateless.
  4. Re-fetch the user by id (cheap PK lookup) to confirm still
     active/not deleted — a small deliberate DB hit instead of trusting
     claims blindly, matching real production practice for account
     deactivation.
  5. Populate `SecurityContextHolder`; continue chain.
- **Refresh** (`POST /auth/refresh`): read `refresh_token` cookie, hash, look
  up, validate not revoked/expired, rotate (revoke old, issue new
  access+refresh), set new cookies.
- **Logout** (`POST /auth/logout`): extract `jti` from the current access
  token → insert `TokenBlacklistEntry` (expiresAt = token's original exp, so
  it can be pruned once naturally expired); revoke the matching
  `RefreshToken` row; clear both cookies (`Max-Age=0`). This is the concrete
  answer to "what actually gets invalidated": the refresh token is gone from
  the DB (no silent re-issuance), and the current access token stops working
  immediately via the denylist, even though it hasn't naturally expired.

**SecurityConfig shape**: `SecurityFilterChain` bean,
`sessionManagement(STATELESS)`, Spring's built-in CSRF disabled (reasoning
documented inline), `permitAll` on `/auth/signup`, `/auth/login`,
`/auth/refresh`, and movie search/browse (deliberate — browsing doesn't
require an account, mirrors real IMDb), `.anyRequest().authenticated()`
otherwise. `PasswordEncoder` (`BCryptPasswordEncoder`) and
`CorsConfigurationSource` (exact Vite dev origin, `allowCredentials(true)`)
as beans.

## Where each requested practice concretely lives

| Requirement | Concrete location |
|---|---|
| DI (constructor-based) | Every `@Service`/`@Controller`/`@Component` — `@RequiredArgsConstructor` + `private final` fields (Spring 4.3+ auto-detects a single constructor, no `@Autowired` needed) |
| DTOs never expose entities | `dto/request`, `dto/response`; mapping happens in `mapper/*` at the controller/service boundary |
| ORM/Hibernate | `entity/*` + `repository/*` (Spring Data JPA), `@MappedSuperclass BaseEntity` for audit fields, one hand-written `@Query` (JPQL) alongside derived-method queries to show both styles |
| Singleton (Spring) | Any default-scope bean, e.g. `PasswordEncoder`, `JwtService` |
| Singleton (non-Spring, deliberate) | `util/OmdbRateLimiterSingleton` (enum-based GoF singleton) |
| Builder (Lombok) | `Movie`, `FavoriteListItem` entities, `AuthResponse`/`MovieResponse` DTOs |
| Builder (hand-rolled) | `util/OmdbApiRequest` — static nested `Builder`, fluent setters, `build()` validates required fields |
| Idempotency | `IdempotencyRecord` entity/repo + `IdempotencyService`, wired inside `FavoriteListItemServiceImpl.addMovie` |
| Validation | `jakarta.validation` annotations on every `dto/request/*`, `@Valid` on controller params |
| Global exception handling | `exception/GlobalExceptionHandler` (`@RestControllerAdvice`), single `ErrorResponse` shape: `timestamp, status, error, message, path` (+ `fieldErrors` map for validation failures) |
| Logging | SLF4J `Logger` per class — INFO for business events, WARN for expected failures, ERROR for unhandled exceptions — never log passwords/tokens |
| Unit tests | `src/test/java/.../service/*Test.java` — JUnit 5 + Mockito, `@ExtendWith(MockitoExtension.class)` |

## Frontend structure

```
frontend/src/
├── pages/        LoginPage, SignupPage, SearchPage, ListsPage, ListDetailPage
├── components/   MovieCard, FavoriteListCard, AddToListButton, NavBar, ProtectedRoute
├── lib/
│   ├── api.ts        fetch wrapper, credentials:'include', 401 → refresh-then-retry-once → else redirect /login
│   └── idempotency.ts  crypto.randomUUID() generated once per logical "add" action, reused across retries of that action
├── context/      AuthContext.tsx (current user via GET /auth/me, login/logout functions)
└── App.tsx        react-router-dom routes
```

No token handling in JS at all — cookies are httpOnly, so the frontend just
calls endpoints and lets the browser manage auth transport. No state library
(matches sibling projects' convention) — `AuthContext` + local component
state is sufficient for this feature surface.

## Build/run setup

- **Backend**: `pom.xml` on `spring-boot-starter-parent` 4.1.0, Java 21+
  (tested with Java 26); deps: `spring-boot-starter-web`, `-validation`, 
  `-data-jpa`, `-security`, `com.h2database:h2`, `io.jsonwebtoken:jjwt-{api,impl,jackson}`, 
  `lombok` (1.18.38+), `spring-security-test` (test scope). `mvnw`/`.mvn` copied 
  from sibling project `1/`. Uses maven-compiler-plugin with Lombok annotation 
  processor for code generation (necessary for Java 21+ compatibility).
- **application.yml**: H2 file datasource; `ddl-auto: update` for dev
  (known simplification — a real prod app would use Flyway/Liquibase
  migrations instead); `test` profile uses in-memory H2 with `create-drop`;
  H2 console enabled only under the `dev` profile; `movielist.jwt.secret`,
  access/refresh TTLs, `omdb.api.key` pulled from env vars with **no
  defaults** so the app fails fast at startup if unset.
- **Secrets**: `.env.example` at repo root (`JWT_SECRET=`, `OMDB_API_KEY=`),
  matching the parent repo's `.gitignore` convention of tracking
  `.env.example` while ignoring `.env`. No dotenv library — export env vars
  before `./mvnw spring-boot:run`, or set them in the IDE run config; keeps
  the mechanism simple and visible.
- **Frontend**: Vite + React + TypeScript, plain CSS, `oxlint`, `vitest` for
  a couple of light tests. `npm run dev` on 5173.

## Study path (build order)

1. Scaffold `pom.xml`/`mvnw`/base package/`application.yml` → verify
   `./mvnw spring-boot:run` boots.
2. Core entities + repositories (`BaseEntity`, `User`, `Movie`,
   `FavoriteList`, `FavoriteListItem`) → verify schema via H2 console.
3. DTOs + mappers + a temporarily-open (no auth) `FavoriteList` CRUD slice →
   validate Controller→Service→Repository→DTO plumbing end to end before
   layering security on top.
4. Auth core: `PasswordEncoder`, `JwtService`, `SecurityConfig`,
   `JwtAuthenticationFilter`, `AuthController` (signup/login) → wire real
   ownership checks into list endpoints.
5. Logout/refresh nuance: `RefreshToken`, `TokenBlacklistEntry`,
   `/auth/refresh`, `/auth/logout` — deliberately after basic auth works,
   since it's the trickiest part.
6. External movie client + caching: `OmdbClient`, `OmdbMovieResponse`,
   `MovieMapper` upsert, `MovieController` search.
7. Add-to-list endpoint (plain, no idempotency yet) — wires movie caching +
   `FavoriteListItem` creation together.
8. Idempotency: `IdempotencyRecord`/`IdempotencyService`, wired into
   add-to-list.
9. `GlobalExceptionHandler` + custom exceptions — retrofit pass across
   everything built so far.
10. Pattern showcase pass: `OmdbRateLimiterSingleton`, hand-rolled
    `OmdbApiRequest` builder.
11. Tests: service-layer Mockito tests, one `@WebMvcTest` slice, one
    `@SpringBootTest` context-load smoke test, one `@DataJpaTest` for the
    unique-constraint behavior.
12. Frontend, in order: Login/Signup → Search/browse → Lists → List detail
    (add/remove) → wire `api.ts` refresh-retry + idempotency key generation
    last.

## Verification

- Backend boots cleanly with `./mvnw spring-boot:run` (fails fast with a
  clear error if `JWT_SECRET`/`OMDB_API_KEY` are unset).
- Manual end-to-end walkthrough (see README).
- `./mvnw test` passes: service-layer unit tests, `@WebMvcTest` slice,
  `@SpringBootTest` smoke test, `@DataJpaTest` constraint test.
- Frontend: `npm run dev`, walk the same flow through the UI;
  `npx vitest run` passes.
