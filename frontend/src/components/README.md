# components/

Reusable pieces shared across pages.

| File | What it does |
|---|---|
| `NavBar.tsx` | Top nav — shows "Search" always, "My Lists" + username + logout when signed in, otherwise a "Log in" link |
| `ProtectedRoute.tsx` | A `react-router-dom` layout route: renders `<Outlet />` if `useAuth().user` is set, otherwise redirects to `/login`. Wraps `/lists` and `/lists/:listId` in `App.tsx`. |
| `MovieCard.tsx` | One search result: poster, title, year, and either `AddToListButton` (if logged in) or a "log in to add" link |
| `FavoriteListCard.tsx` | One list summary on `ListsPage`: name (links to detail), item count, delete button |
| `AddToListButton.tsx` | List picker + "Add" button. Generates one `Idempotency-Key` per click (via `../lib/idempotency.ts`) and **reuses the same key if the request errors and the user retries** — only cleared on success. This is the frontend half of the idempotency showcase; see the backend's `service/impl/FavoriteListItemServiceImpl`. |
