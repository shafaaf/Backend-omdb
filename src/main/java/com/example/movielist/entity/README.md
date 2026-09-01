# entity/

JPA entities — the ORM layer. Never returned directly from a controller; always
mapped to a `dto/response` type first (see `mapper/`).

| File | What it is |
|---|---|
| `BaseEntity.java` | `@MappedSuperclass` with `id`, `createdAt`, `updatedAt`. Every other entity except `TokenBlacklistEntry` extends this — audit timestamps are filled automatically by JPA Auditing (`JpaAuditingConfig`). |
| `User.java` | An account: email, password hash, display name. Owns a list of `FavoriteList`. |
| `Movie.java` | A locally cached movie fetched from OMDb, keyed by `externalId` (the IMDb id). Built via Lombok `@Builder`. `refreshFrom(Movie)` overwrites this row's fields with freshly-fetched data. |
| `FavoriteList.java` | A named list owned by one `User`. `addItem(Movie)` and `removeItem(FavoriteListItem)` manage its items; `containsMovie(Movie)` backs the duplicate check inside `addItem`. |
| `FavoriteListItem.java` | Join row: one movie's membership in one list. Built via Lombok `@Builder`. |
| `RefreshToken.java` | An issued refresh token — only its SHA-256 hash is stored, never the raw value. `revoke()` marks it dead; `isUsable()` checks not-revoked-and-not-expired. |
| `TokenBlacklistEntry.java` | One blacklisted access-token `jti`. The mechanism that makes logout actually invalidate a still-unexpired JWT. Does *not* extend `BaseEntity` — its primary key is the `jti` string itself. |
| `IdempotencyRecord.java` | A stored response for one `(idempotencyKey, userId, endpointPath)` combination, used to replay a retried request instead of re-running it. See `service/impl/FavoriteListItemServiceImpl`. |

**Three layers against duplicate list entries**, each in a different file: the
domain check in `FavoriteList.addItem`, the DB unique constraint on
`FavoriteListItem`, and the idempotency check in `IdempotencyRecord` — see
`CLAUDE.md` for why all three exist.
