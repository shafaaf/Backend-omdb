# repository/

Spring Data JPA repositories — one per entity, mostly derived-method queries
(Spring generates the implementation from the method name). Each extends
`JpaRepository<Entity, IdType>`, which already provides `save`, `findById`,
`findAll`, `delete`, `existsById`, etc. — only the extra query methods are
listed below.

| File | Extra methods |
|---|---|
| `UserRepository.java` | `findByEmail`, `existsByEmail` |
| `MovieRepository.java` | `findByExternalId` (the OMDb cache lookup) |
| `FavoriteListRepository.java` | `findByOwnerId`, `findByIdAndOwnerId` (scopes ownership *in the query*, so a non-owner's lookup finds nothing rather than needing a manual check), `existsByOwnerIdAndName` |
| `FavoriteListItemRepository.java` | `existsByListIdAndMovieId`, `findByListIdAndMovieId`, `countByListId`, and one hand-written JPQL query `findByListIdWithMovie` (`JOIN FETCH` to avoid N+1 when listing a list's contents) — shown deliberately alongside the derived-method queries in the same file |
| `RefreshTokenRepository.java` | `findByTokenHash` |
| `TokenBlacklistRepository.java` | none — `existsById(jti)` from `JpaRepository` is exactly what `JwtAuthenticationFilter` needs |
| `IdempotencyRecordRepository.java` | `findByIdempotencyKeyAndUserIdAndEndpointPath` |


My chatgpt prompt:
Just like the guide on java api calling and parsing, usage you made, can you make something similar for java backend concepts.

Use a usecase like imdb where users can read the site without logging in and see movie ratings, but if logged in can make watchlists. Not too complex.

Concepts would include but not limited to for example: Beans, autowired, @Component, Cors, Jpa, entities, DTO, mappers, repositories, idempotency, lombok, , controllers, csrf, Slf4j, services etc

And have structure and flow, decide on an order that makes sense. Can even group concepts together as needed.
Output have it on chatgpt here nicely. Once I validate, then only we will go with the pdf.

Dont me it unnecessary long, make it easu to understand, concise, summarized as needed