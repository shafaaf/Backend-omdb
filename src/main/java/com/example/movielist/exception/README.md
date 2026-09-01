# exception/

Custom exceptions plus the one place that turns any exception into a
response: `GlobalExceptionHandler`.

| File | Mapped to | Thrown when |
|---|---|---|
| `ResourceNotFoundException.java` | 404 | A list/movie doesn't exist, or exists but isn't owned by the requester (deliberately 404, not 403 — never confirms something exists to a non-owner) |
| `DuplicateResourceException.java` | 409 | Signup with an already-registered email; creating a list with a name you already have; adding a movie already in the list |
| `InvalidCredentialsException.java` | 401 | Bad login, or an invalid/expired/revoked refresh token — always the same generic message regardless of which is true |
| `IdempotencyConflictException.java` | 409 | Same `Idempotency-Key` reused for a request with different logically-significant fields |
| `ExternalApiException.java` | 502 | OMDb is unreachable, errors, or rejects the request (e.g. bad API key) |
| `GlobalExceptionHandler.java` | — | `@RestControllerAdvice` — one `@ExceptionHandler` method per exception type above, plus `MethodArgumentNotValidException`/`ConstraintViolationException`/`MissingServletRequestParameterException` (→ 400 with field errors), `DataIntegrityViolationException` (→ 409, the backstop for the idempotency-key race — see `service/README.md`), and a catch-all `Exception` → 500 with a generic message (full stack trace logged server-side only) |

Every handler returns the same `dto/response/ErrorResponse` shape.
