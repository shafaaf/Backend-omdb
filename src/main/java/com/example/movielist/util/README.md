# util/

Small standalone helpers that don't belong to any one domain area, plus the
two "pattern showcase" classes called out in `CLAUDE.md`.

| File | What it is |
|---|---|
| `HashUtil.java` | `sha256Hex(value)` — generic SHA-256 hashing, used both for refresh-token storage (`security/TokenHasher`) and idempotency-key request fingerprinting (`service/impl/FavoriteListItemServiceImpl`). |
| `OmdbApiRequest.java` | **Hand-rolled Builder pattern.** A validated holder for one OMDb query: `apiKey` + exactly one of `imdbId`/`searchTerm`. `build()` throws `IllegalStateException` if that invariant isn't met — the thing Lombok's generated `@Builder` (used elsewhere, e.g. `entity/Movie`) can't do, since it has no hook for validating field combinations before construction. |
| `OmdbRateLimiterSingleton.java` | **Hand-rolled Singleton pattern** — a classic Java enum singleton (`INSTANCE`), built without Spring's DI container at all. Throttles calls to OMDb with a simple fixed-window counter; `acquire()` throws `ExternalApiException` once the window's budget is spent. Contrast with `config/PasswordEncoderConfig`'s `PasswordEncoder` bean, which is a *Spring-managed* singleton instead. |
