# controller/

Thin HTTP layer — no business logic here, just request/response wiring:
`@Valid` on the body, pull the current user off `@AuthenticationPrincipal
CustomUserDetails`, delegate to a `service/`, map the result to a status
code.

| File | Routes | Auth |
|---|---|---|
| `AuthController.java` | `POST /api/auth/signup`, `/login`, `/refresh`, `/logout`, `GET /api/auth/me` | signup/login/refresh are `permitAll`; sets/clears the httpOnly cookies via `security/AuthCookies` |
| `FavoriteListController.java` | `POST/GET /api/lists`, `GET/DELETE /api/lists/{listId}` | requires auth — every method takes the current user's id from the principal |
| `FavoriteListItemController.java` | `POST/GET /api/lists/{listId}/movies`, `DELETE /api/lists/{listId}/movies/{imdbId}` | requires auth. `POST` reads the optional `Idempotency-Key` header and passes it straight through to `FavoriteListItemService.addMovie` |
| `MovieController.java` | `GET /api/movies/search?title=`, `GET /api/movies/{imdbId}` | `permitAll` — browsing doesn't require an account, mirrors real IMDb (see `SecurityConfig`) |

All error responses (validation, not-found, duplicate, etc.) are handled
centrally by `exception/GlobalExceptionHandler` — no controller here has its
own try/catch.
