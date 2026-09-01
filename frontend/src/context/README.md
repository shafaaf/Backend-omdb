# context/

| File | What it does |
|---|---|
| `AuthContext.tsx` | `AuthProvider` wraps the whole app (in `../App.tsx`) and loads the current user via `GET /auth/me` on mount. Exposes `user`, `loading`, `login()`, `signup()`, `logout()` through the `useAuth()` hook. No token handling here — auth state is just "did `/auth/me` return a user," since the actual credential lives in an httpOnly cookie the frontend never touches. |
