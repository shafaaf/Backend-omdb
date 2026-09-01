# How this app wires itself together

If you haven't touched Spring in a while, this is the part that looks like
magic: no class ever writes `new SomeService()`, yet everything just works
at startup. This page explains that, in plain terms, with one real example
traced end to end.

## The short version

1. You run `./mvnw spring-boot:run`.
2. Spring scans the code for classes marked `@Service`, `@Repository`,
   `@Controller`, `@Component`, `@Configuration`.
3. It creates **one instance of each** (called a "bean") and holds them all
   in one container (the "ApplicationContext").
4. When a class needs another class, it just asks for it as a constructor
   parameter. Spring finds a matching bean in the container and hands it
   over automatically. That handing-over is "dependency injection" — a class
   never builds its own dependencies, they're given to it.

## What that looks like in this codebase

Every service/controller here follows the same shape:

```java
@Service
@RequiredArgsConstructor   // Lombok: generates a constructor from the fields below
public class FavoriteListServiceImpl implements FavoriteListService {
    private final FavoriteListRepository favoriteListRepository;
    private final UserRepository userRepository;
    // ...
}
```

`@RequiredArgsConstructor` makes Lombok generate this constructor for you at
compile time:

```java
public FavoriteListServiceImpl(FavoriteListRepository r1, UserRepository r2) {
    this.favoriteListRepository = r1;
    this.userRepository = r2;
}
```

Spring sees that constructor, sees it needs those two types, finds matching
beans (Spring Data JPA auto-creates a repository bean for every interface
extending `JpaRepository`), and calls the constructor itself — once, at
startup, with the real objects. You never write `new FavoriteListServiceImpl(...)`
anywhere in this codebase.

## Where a config value (like the JWT secret) actually comes from

This is the part that trips people up: tracing one value from your `.env`
file all the way to the code that uses it.

```
.env file:  JWT_SECRET=abc123...
    ↓  (you `source .env`, or your IDE sets it)
environment variable JWT_SECRET
    ↓
application.yml:  movielist.jwt.secret: ${JWT_SECRET:}
    ↓  (Spring resolves the ${...} placeholder at startup)
config/JwtProperties.java — a record bound to "movielist.jwt.*"
    ↓  (JwtProperties becomes a bean itself — see @ConfigurationPropertiesScan
    ↓   on MovielistApplication)
security/JwtService.java — takes JwtProperties as a constructor parameter
    ↓
JwtService reads properties.secret() to sign a token
```

`OMDB_API_KEY` follows the identical path into `config/OmdbProperties.java` →
`client/OmdbClientImpl.java`.

If either env var is missing, `JwtProperties`/`OmdbProperties` fail
validation (`@NotBlank`) the moment Spring tries to build them, so the app
refuses to start — you get a clear startup error instead of a confusing
crash later while handling a real request.

## Tracing one real request

Once startup finishes and every bean exists, here's `POST /api/lists`:

1. `FavoriteListController` (a bean) receives the HTTP request.
2. It calls `FavoriteListService` (a bean it was handed in its constructor).
3. That service calls `FavoriteListRepository` (handed to *it* the same way).
4. The repository talks to the database, returns an entity.
5. The service hands that entity to `FavoriteListMapper` — a plain static
   method, not a bean, just a function — to turn it into a DTO.
6. The controller returns the DTO; Spring turns it into JSON.

Every arrow above was decided once, at startup, by Spring reading
constructors — nothing is looked up by name per-request.

## One thing worth knowing: beans are usually singletons

Every bean above is built exactly once and shared by every request. That's
safe here because none of these classes store per-request data as instance
fields — everything request-specific comes in as a method parameter instead.
See `config/PasswordEncoderConfig.java` and `util/OmdbRateLimiterSingleton.java`
for two different flavors of "there's only one of this in the whole app" —
one Spring-managed, one hand-written — contrasted in their own comments.

---

Read next: `src/main/java/com/example/movielist/controller/README.md`, then
`service/README.md`, for what actually happens once a request lands.
