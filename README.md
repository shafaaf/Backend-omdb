# movielist — IMDb-lite backend-practices showcase

A small full-stack app (Spring Boot + React) built specifically to demonstrate
production backend engineering practices in a codebase small enough to read
end to end: sign up / sign in / sign out, create favorite movie lists, add and
remove movies (sourced from the OMDb API and cached locally).

Feature scope is deliberately minimal — the point of this project is the
engineering underneath, not the feature count. See `CLAUDE.md` for the full
design rationale, package layout, and the specific places each practice
(DTOs, DI, ORM, design patterns, idempotency, JWT auth, validation, exception
handling, logging, tests) lives in the code.

## New here? Read in this order

Don't start at file #1 and read top to bottom — read these in order instead,
each one building on the last:

1. **This file** — what the app does, how to run it.
2. **[`FLOW_STARTUP.md`](FLOW_STARTUP.md)** — a timeline, step by step, from
   running the startup commands to the home page appearing on screen with
   real movie posters. Read this first for the big picture before diving
   into individual files.
3. **[`DEPENDENCY_INJECTION.md`](DEPENDENCY_INJECTION.md)** — the 5-minute
   version of *how* Spring boots this app and wires all the classes together
   (the mechanism behind several steps in `FLOW_STARTUP.md`).
4. **`entity/README.md`** — the "nouns": what data the app stores.
5. **`dto/README.md`** — the shapes that actually travel over the network.
6. **`repository/README.md`** — how entities get read/written to the database.
7. **`mapper/README.md`** — the glue between entity and DTO.
8. **`service/README.md`** — the business logic: "what happens when...".
9. **`controller/README.md`** — the HTTP layer that calls into `service/`.
10. **`security/README.md`** — how a request proves who it is.
11. **`config/README.md`** — the wiring pieces (ties back to
    `DEPENDENCY_INJECTION.md`).
12. **Frontend**: `pages/README.md` → `components/README.md` →
    `lib/README.md` → `context/README.md`.

Then pick one real feature — "add a movie to a list" is the richest one —
and trace it top to bottom yourself: Controller → Service → Repository/Entity
→ Mapper → DTO back out. One traced feature teaches you the architecture
faster than reading every file in isolation.

## Prerequisites

- Java 21+ (tested with Java 26)
- Node.js 18+ / npm
- A free OMDb API key: https://www.omdbapi.com/apikey.aspx

## Setup

1. Copy `.env.example` to `.env` and fill in `JWT_SECRET` (any long random
   string) and `OMDB_API_KEY` (from the link above). The app fails fast at
   startup if either is missing — this is intentional (see `CLAUDE.md`).
   
   Example `.env`:
   ```
   JWT_SECRET=your-long-random-string-here
   OMDB_API_KEY=your-omdb-api-key-here
   ```

2. Export the environment variables before running. Choose one:

   **Option A: Export in your shell**
   ```bash
   export JWT_SECRET="your-secret-here"
   export OMDB_API_KEY="your-api-key-here"
   ```

   **Option B: Source the .env file**
   ```bash
   set -a; source .env; set +a
   ```

   **Option C: Set in your IDE's run configuration** (No dotenv library is used
   on purpose — see `CLAUDE.md` for why.)

## Running the app (quick start)

**Terminal 1 — Start the backend:**
```bash
export JWT_SECRET="your-secret-here"
export OMDB_API_KEY="your-api-key-here"
./mvnw spring-boot:run
```
Backend runs on `http://localhost:8080`

**Terminal 2 — Start the frontend:**
```bash
cd frontend
npm install  # only needed first time
npm run dev
```
Frontend runs on `http://localhost:5173`

**View the app:**
- Open your browser to `http://localhost:5173`
- Or if ports are already in use, the Vite dev server will suggest an alternative
  (e.g., `http://localhost:5174` or `http://localhost:5175`)

**Note:** Data persists to `./data/movielist.mv.db` (file-based H2) so state 
survives restarts — handy for demoing without reseeding every run.

### Backend options

- Run with H2 console (dev only):
  ```bash
  ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
  ```
  Then visit `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:file:./data/movielist`)

- Wipe the local database for a fresh start:
  ```bash
  rm -rf data/
  ```

## Running tests

```bash
./mvnw test          # backend: JUnit5 + Mockito unit tests, slice tests, smoke test
cd frontend && npx vitest run   # frontend: a couple of light unit tests
```

## Troubleshooting

**Backend fails to start with `JWT_SECRET must be set` or `OMDB_API_KEY must be set`:**
- Make sure you've exported the environment variables before running `./mvnw spring-boot:run`
- Run `echo $JWT_SECRET` to verify it's set in your current shell
- Try exporting again with `export JWT_SECRET="..."`

**Frontend API calls fail with CORS errors (403 Forbidden on OPTIONS request):**
- The CORS configuration allows specific ports. If you're running on a different port
  (e.g., 5174 or 5175), update `src/main/java/com/example/movielist/config/CorsConfig.java`
  to include your port in the `setAllowedOrigins()` list.

**"Cannot find symbol: variable log" or "Cannot find symbol: method getId()" during build:**
- This is a Lombok annotation processing issue
- Make sure Java 21+ is installed: `java -version`
- Run `./mvnw clean` then try again
- If still broken, check that pom.xml has the maven-compiler-plugin configured
  with Lombok in annotationProcessorPaths

**Port already in use (Address already in use):**
- Backend (8080): `lsof -ti:8080 | xargs kill`
- Frontend (5173): `lsof -ti:5173 | xargs kill`
- Or just let Vite pick the next available port — it will show you which one

## Manual walkthrough (also see "Verification" in CLAUDE.md)

1. Sign up, then log in.
2. Search for a movie (hits OMDb on first search; second search for the same
   title comes from the local `Movie` cache — check server logs).
3. Create a favorite list, add the movie to it.
4. Retry the same "add movie" request with an `Idempotency-Key` header —
   confirm it doesn't create a duplicate and instead replays the original
   response.
5. Log out, then confirm the old access token is rejected and the refresh
   token no longer works.

## Useful commands

**Backend** (run from `backend-practise/`):

| Command | What it does |
|---|---|
| `./mvnw spring-boot:run` | Run the API on :8080 |
| `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev` | ...with the H2 console enabled at `/h2-console` |
| `./mvnw test` | Run all backend tests |
| `./mvnw test -Dtest=AuthServiceImplTest` | Run one test class |
| `./mvnw test -Dtest=AuthServiceImplTest#login_badCredentials_throwsGenericInvalidCredentialsException` | Run one test method |
| `./mvnw clean package` | Build the runnable jar (`target/movielist-0.0.1-SNAPSHOT.jar`) |
| `java -jar target/movielist-0.0.1-SNAPSHOT.jar` | Run that jar directly |
| `./mvnw dependency:tree` | Inspect the resolved dependency graph |
| `rm -rf data/` | Wipe the local H2 database (fresh state next boot) |

**Frontend** (run from `backend-practise/frontend/`):

| Command | What it does |
|---|---|
| `npm install` | Install dependencies (only needed once, or after `package.json` changes) |
| `npm run dev` | Run the dev server on :5173 with hot reload |
| `npm run build` | Type-check (`tsc -b`) and produce a production build in `dist/` |
| `npm run preview` | Serve the production build locally |
| `npm run lint` | Run oxlint |
| `npx vitest run` | Run frontend tests once |
| `npx vitest` | Run frontend tests in watch mode |

**Poking at the API directly** (backend must be running):

```bash
# Sign up (Idempotency-Key/CSRF header not needed on this one — see SecurityConfig)
curl -i -c cookies.txt -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" -H "X-Requested-With: XMLHttpRequest" \
  -d '{"email":"you@example.com","password":"password123","displayName":"You"}'

# Reuse the cookie jar for an authenticated request
curl -i -b cookies.txt http://localhost:8080/api/auth/me

# Create a list (mutating requests need the X-Requested-With header — see CsrfHeaderFilter)
curl -i -b cookies.txt -X POST http://localhost:8080/api/lists \
  -H "Content-Type: application/json" -H "X-Requested-With: XMLHttpRequest" \
  -d '{"name":"My Favorites"}'

# Add a movie idempotently — rerun the exact same command and it won't duplicate
curl -i -b cookies.txt -X POST http://localhost:8080/api/lists/1/movies \
  -H "Content-Type: application/json" -H "X-Requested-With: XMLHttpRequest" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{"imdbId":"tt0111161"}'
```

**Killing a stuck dev server**:

```bash
lsof -ti:8080 | xargs kill   # backend
lsof -ti:5173 | xargs kill   # frontend
```

## Project layout

```
backend-practise/
├── pom.xml, mvnw, mvnw.cmd, .mvn/
├── src/main/java/com/example/movielist/   # backend source, see CLAUDE.md for package map
├── src/main/resources/application.yml
├── src/test/java/com/example/movielist/
├── .env.example
├── README.md            (this file)
├── CLAUDE.md             full design doc
├── FLOW_STARTUP.md      timeline from startup command to home page on screen
├── DEPENDENCY_INJECTION.md   how Spring wires the app together, plain-language
└── frontend/              Vite + React + TS
```

Every backend package (`entity/`, `repository/`, `dto/`, `mapper/`,
`service/`, `controller/`, `security/`, `config/`, `exception/`, `client/`,
`util/`) and every frontend folder (`pages/`, `components/`, `lib/`,
`context/`) has its own `README.md` listing what each file does — start
there when you land in an unfamiliar directory.

## Study path

This project is meant to be read and rebuilt incrementally, not consumed as
one giant drop. Recommended order (also in `CLAUDE.md`):

1. Scaffold (pom.xml, application.yml) → verify boot
2. Core entities + repositories → verify schema via H2 console
3. DTOs + mappers + an open (no-auth) FavoriteList CRUD slice
4. Auth core: signup/login + JWT filter
5. Refresh + logout (the trickiest part — done after basic auth works)
6. OMDb client + movie caching
7. Add-to-list endpoint (plain), then idempotency layered on top
8. Global exception handling retrofit pass
9. Pattern showcase pass: singleton + builder contrast comments
10. Tests
11. Frontend, page by page
