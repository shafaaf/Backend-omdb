# lib/

Framework-agnostic helpers — no React here.

| File | What it does |
|---|---|
| `api.ts` | The only place that calls the backend. `apiRequest<T>(path, options)` — sends `credentials: 'include'` (so httpOnly auth cookies are sent automatically, never handled in JS) and the `X-Requested-With` header the backend's CSRF filter requires on mutations. On a `401`, transparently calls `/auth/refresh` once and retries the original request before giving up. Throws `ApiError` (with `.status` and `.fieldErrors`) on any non-2xx response. |
| `idempotency.ts` | `newIdempotencyKey()` — one `crypto.randomUUID()` per logical action (see `../components/AddToListButton.tsx` for how its lifecycle is tied to a single "add" attempt, not to every HTTP request). |
