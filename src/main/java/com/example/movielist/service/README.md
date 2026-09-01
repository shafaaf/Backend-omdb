# service/ (+ service/impl/)

Business logic, one interface per feature area plus a single `*Impl` in
`impl/`. Declaring the interface even with only one implementation is
somewhat old-school (modern Spring guidance often skips it) — kept
deliberately here because "why interface+impl?" is a common interview
question, and it's what lets `FavoriteListItemServiceImplTest` mock the
repositories directly while `FavoriteListControllerTest` mocks the service
interface itself, with neither test needing the other layer.

Every `*Impl` is `@Service` + `@RequiredArgsConstructor` (constructor DI,
Spring auto-detects the single constructor — no `@Autowired` needed) +
`@Transactional`.

| Interface | Impl methods | What it does |
|---|---|---|
| `AuthService.java` | `signup`, `login`, `refresh`, `logout`, `getCurrentUser` | Signup/login issue an access JWT + opaque refresh token. `login` delegates credential-checking to Spring Security's `AuthenticationManager` (no manual password comparison). `refresh` rotates the refresh token (old one revoked). `logout` blacklists the current access token's `jti` and revokes the refresh token — see `security/JwtAuthenticationFilter` for how the blacklist is enforced. |
| `FavoriteListService.java` | `create`, `findAllForOwner`, `findOneForOwner`, `delete` | Plain ownership-scoped CRUD for lists. Every lookup goes through `findByIdAndOwnerId` so a non-owner's request 404s instead of 403. |
| `FavoriteListItemService.java` | `addMovie`, `findAllForList`, `removeMovie` | **The idempotency showcase.** `addMovie` checks for a replayable prior response (via `IdempotencyService`), verifies list ownership, fetches-or-caches the movie (via `MovieService`), and saves — with the idempotency record written in the same transaction as the list-item insert. Full write-up in `CLAUDE.md`. |
| `MovieService.java` | `search`, `getOrFetchEntity`, `getOrFetchEntity` | `search` proxies to OMDb. `getOrFetchEntity` returns the cached `Movie` entity, fetching-and-upserting from OMDb on a cache miss — this returns an *entity*, not a DTO, because it's a service-to-service call (used by `FavoriteListItemServiceImpl` too), not a controller boundary. `getOrFetch` is the DTO-returning wrapper for `MovieController`. |
| `IdempotencyService.java` | `checkForReplay`, `record` | Generic idempotency-key bookkeeping against `IdempotencyRecord` — doesn't know anything about favorite lists specifically. |

Two small internal carrier records live directly in `service/` (not
`dto/response/`) because they're never meant to leave the service layer:

- `AuthResult.java` — holds the *raw* access/refresh token values between
  `AuthServiceImpl` and `AuthController`, which turns them into cookies.
  Kept out of `dto/` so that boundary is structural, not just a convention.
- `IdempotentReplay.java` — the stored `(status, responseBodyJson)` pair
  returned by `IdempotencyService.checkForReplay`.
