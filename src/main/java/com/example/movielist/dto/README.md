# dto/

Data Transfer Objects — the only shapes that ever cross the controller
boundary. Entities are never serialized directly to JSON; `mapper/` converts
between the two. All are Java `record`s (immutable, `@NotBlank`/`@Email`/etc.
validation annotations go directly on the record components).

## request/ — validated input

| File | Fields |
|---|---|
| `SignupRequest.java` | `email` (`@Email`), `password` (`@Size(min=8)`), `displayName` |
| `LoginRequest.java` | `email`, `password` |
| `CreateFavoriteListRequest.java` | `name` |
| `AddMovieToListRequest.java` | `imdbId` |

## response/ — output shapes

| File | What it carries |
|---|---|
| `UserResponse.java` | `id`, `email`, `displayName` — never `passwordHash` |
| `AuthResponse.java` | `user` + `accessTokenExpiresAt`. Built with Lombok `@Builder`. Never carries the actual tokens — those go out as httpOnly cookies (see `security/AuthCookies`), not in the JSON body. |
| `FavoriteListResponse.java` | `id`, `name`, `itemCount`, `createdAt` |
| `FavoriteListItemResponse.java` | `id`, nested `MovieResponse`, `addedAt` |
| `MovieResponse.java` | Full cached movie detail. Built with Lombok `@Builder` (many nullable OMDb fields). |
| `MovieSearchResultResponse.java` | Lightweight search-result shape (no plot/genre/director/rating — OMDb's search endpoint doesn't return those) |
| `ErrorResponse.java` | The one error shape the whole API returns: `timestamp`, `status`, `error`, `message`, `path`, `fieldErrors`. Built by `exception/GlobalExceptionHandler` and also directly by `security/SecurityConfig` / `security/CsrfHeaderFilter` for failures that happen before a controller is reached. |
