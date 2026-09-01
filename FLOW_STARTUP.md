# From "run the commands" to "home page is on screen"

This doc is a timeline, not an explanation of mechanisms. It answers "what
happens, in what order, in which file" from the moment you run the startup
commands to the moment the home page (the Featured Movies grid on `/search`)
is visible in your browser with real posters on it.

For *how* Spring actually constructs all these objects and hands config
values around, read [`DEPENDENCY_INJECTION.md`](DEPENDENCY_INJECTION.md) —
this doc links to it at the relevant step instead of re-explaining it.

Two halves: the backend boots first, then the frontend boots and calls the
backend to paint the page.

## Backend: cold boot to ready

**1. You run `./mvnw spring-boot:run`** (or with `-Dspring-boot.run.profiles=dev`
to also turn on the H2 database console). Maven compiles the code if needed,
starts a JVM, and runs the `main` method of `src/main/java/com/example/movielist/MovielistApplication.java`.

**2. `MovielistApplication.main`** calls `SpringApplication.run(...)`. This
one line is what kicks off everything below — Spring Boot's own startup
sequence. The class is marked `@SpringBootApplication` (turns on
autoconfiguration and component scanning) and `@ConfigurationPropertiesScan`
(needed for step 3).

**3. Config values get resolved.** If you ran `set -a; source .env; set +a`
first, `JWT_SECRET` and `OMDB_API_KEY` are now OS environment variables.
`src/main/resources/application.yml` references them as
`${JWT_SECRET:}` and `${OMDB_API_KEY:}` — Spring fills those placeholders in
right now, and binds the results onto two small records:
`config/JwtProperties.java` and `config/OmdbProperties.java`. Both fields are
`@NotBlank` with no default value, so if either env var was never set,
validation fails immediately and the app refuses to start — you get a clear
error here, not a confusing failure later while handling a real request. See
`DEPENDENCY_INJECTION.md` for exactly how a YAML value ends up bound onto a
Java record.

**4. Every other class gets built.** Spring scans the `com.example.movielist`
package for `@Service`, `@Repository`, `@Controller`, `@Component`, and
`@Configuration` classes and constructs one instance of each — the
repositories, the mappers' owning services, `security/JwtService.java`,
`security/JwtAuthenticationFilter.java`, and so on. Again,
`DEPENDENCY_INJECTION.md` covers *how* constructor wiring works; here it's
just: this is the moment it happens.

**5. The database gets set up.** Spring finds every class under `entity/`
(`User`, `Movie`, `FavoriteList`, `FavoriteListItem`, `RefreshToken`,
`TokenBlacklistEntry`, `IdempotencyRecord`), and `config/JpaAuditingConfig.java`
(`@EnableJpaAuditing`) is picked up so `createdAt`/`updatedAt` fields fill
themselves in later. Hibernate connects to the datasource named in
`application.yml` — normally a file at `./data/movielist` (an H2 database,
created if it doesn't exist yet), an in-memory one when running tests — and
creates or updates one table per entity (`ddl-auto: update`).

**6. The security filter chain gets assembled.** `config/SecurityConfig.java`'s
`securityFilterChain` bean runs: it inserts `security/CsrfHeaderFilter.java`
and `security/JwtAuthenticationFilter.java` into the request pipeline,
attaches the CORS rules from `config/CorsConfig.java` (only
`http://localhost:5173` is allowed to call this API with cookies), and
registers the JSON error responses used when a request is unauthenticated or
forbidden.

**7. The web server starts.** Embedded Tomcat binds port 8080 (`server.port`
in `application.yml`). Spring Boot logs `Started MovielistApplication in
X seconds`. The backend is now sitting there, ready to answer HTTP requests
— nothing has been requested yet, this is just "ready." (With the `dev`
profile, the H2 console is also reachable at `/h2-console` from this point
on, useful for poking at the actual database tables.)

## Frontend: page load to visible home page

**1. You run `npm install`** (once) **then `npm run dev`** inside `frontend/`.
Vite starts a dev server on port 5173. Unlike a production build, it doesn't
bundle anything up front — it serves your TypeScript/React files straight to
the browser and compiles each one on the fly as it's requested.

**2. Your browser requests `http://localhost:5173/`.** Vite serves
`frontend/index.html`, whose only real content is `<div id="root"></div>` and
a `<script type="module" src="/src/main.tsx">` tag. That script tag is what
pulls in the actual app.

**3. `frontend/src/main.tsx` runs.** One call:
`createRoot(document.getElementById('root')!).render(<App />)`. React now
owns that `<div id="root">` and everything inside it.

**4. `frontend/src/App.tsx` renders.** `BrowserRouter` wraps `AuthProvider`,
which wraps `NavBar` plus a `Routes` table. The current URL (`/`, on a fresh
visit) is matched against that table — the `/` route immediately redirects
to `/search`, which is this app's actual home page.

**5. `frontend/src/context/AuthContext.tsx`'s `AuthProvider` mounts.** Its
effect fires right away: `GET /api/auth/me` via `lib/api.ts`'s `apiRequest`.
On a brand-new visit there's no auth cookie yet, so this comes back
unauthenticated, `user` stays `null`, and `loading` becomes `false`. This is
why the home page can render fully with no login — browsing was designed to
never require an account (mirrored on the backend: `MovieController`'s
routes are `permitAll` in `SecurityConfig`).

**6. `frontend/src/pages/SearchPage.tsx` renders** — this is the component
behind `/search`, i.e. the actual home page. A second effect fires straight
away: it reads the fixed list of IMDb ids from
`frontend/src/lib/featuredMovies.ts` (`FEATURED_IMDB_IDS` — 12 well-known
titles) and calls `GET /api/movies/{imdbId}` once per id, all in parallel via
`apiRequest`.

**7. Each of those 12 requests hits the backend.** `controller/MovieController.java`'s
`getOne` receives it and calls `service/impl/MovieServiceImpl.java`'s
`getOrFetch`, which calls `getOrFetchEntity`. That method first asks
`repository/MovieRepository.findByExternalId(imdbId)` — on the very first
run of the app this is a cache miss for every title, so it falls through to
`client/OmdbClientImpl.java`, which calls the real OMDb API and gets back
raw JSON. `mapper/MovieMapper.fromOmdb(...)` turns that into a `Movie`
entity, which gets saved — caching it, so the *next* time anyone asks for
that same `imdbId` (a second visit to this page, for instance) it's served
straight from the local database instead of calling OMDb again. Either way,
`MovieMapper.toResponse(...)` turns the `Movie` entity into a `MovieResponse`
DTO, which comes back to the browser as JSON.

**8. Back in the browser, the grid actually appears.** `SearchPage.tsx`
collects the 12 responses with `Promise.allSettled` (so if one title happens
to fail, the other 11 still show up instead of the whole grid going blank),
converts each `MovieResponse` into the lighter shape `MovieCard` expects, and
renders one `components/MovieCard.tsx` per movie inside a `<div className="movie-grid">`
— that CSS class, defined in `frontend/src/index.css`, is what lays them out
as a responsive grid of poster cards. This is the moment the home page goes
from "loading…" to a wall of real movie posters.

## Why does `/search` show a grid instead of a login form?

Because nothing on this page — searching, browsing, viewing a movie's full
detail page — requires an account. That's a deliberate choice, made on the
backend first (`SecurityConfig` marks movie-browsing `permitAll`) and
followed through on the frontend (`AuthContext` treats "not logged in" as a
normal, fully-functional state, not an error to redirect away from). The
*only* thing that ever asks you to log in is trying to add a movie to a
list — you'll see that gate on `AddToListButton`/`MovieCard`, not here on
the home page.
