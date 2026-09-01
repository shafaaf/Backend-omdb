# pages/

One component per route (wired up in `../App.tsx`). Each owns its own local
state and talks to the backend directly via `../lib/api.ts` — there's no
global state library.

| File | Route | What it does |
|---|---|---|
| `LoginPage.tsx` | `/login` | Email/password form → `useAuth().login()` → redirects to `/search` on success |
| `SignupPage.tsx` | `/signup` | Same, plus a display name field and per-field validation errors (`ApiError.fieldErrors`) surfaced under each input |
| `SearchPage.tsx` | `/search` | Movie search box → `GET /movies/search`. Public — doesn't require login (mirrors the backend's `permitAll`). If logged in, also loads the user's lists so `MovieCard` can show the "add to list" control. |
| `MovieDetailPage.tsx` | `/movies/:imdbId` | Public — full movie detail (plot/genre/director/rating) via `GET /movies/{imdbId}`. Same login-optional pattern as `SearchPage`: viewing is always allowed, the "add to list" control only appears when signed in. |
| `ListsPage.tsx` | `/lists` (protected) | Create a list, list all of the user's lists, delete one |
| `ListDetailPage.tsx` | `/lists/:listId` (protected) | Loads one list + its items in parallel, lets you remove a movie from the list |
