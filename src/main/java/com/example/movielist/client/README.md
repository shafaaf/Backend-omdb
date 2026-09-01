# client/

The OMDb API integration — everything that knows about OMDb's specific JSON
shape lives here, isolated from the rest of the app behind the `OmdbClient`
interface.

| File | What it is |
|---|---|
| `OmdbClient.java` | Interface: `search(searchTerm)` → `List<OmdbSearchItem>`, `fetchByImdbId(imdbId)` → `Optional<OmdbMovieResponse>` |
| `OmdbClientImpl.java` | Calls OMDb via the `RestClient` bean (`config/RestClientConfig`), builds the query with `util/OmdbApiRequest`, throttles via `util/OmdbRateLimiterSingleton`, wraps any HTTP/network failure in `exception/ExternalApiException`. OMDb's own "not found" response (`Response: "False"`) is treated as an empty result, not an error. |
| `OmdbMovieResponse.java` | Raw shape of OMDb's full-detail response (`i=` lookup by imdbID) — `Title`, `Plot`, `Genre`, `Director`, `imdbRating`, etc. `isFound()` checks OMDb's own `Response` field. |
| `OmdbSearchItem.java` | Raw shape of one entry in OMDb's search results (`s=` query) — lighter than the detail response: no plot/genre/director/rating. |
| `OmdbSearchResponse.java` | Wraps a `List<OmdbSearchItem>` plus OMDb's `Response`/`Error` fields. `isSuccess()` checks whether the search actually returned results. |

`mapper/MovieMapper` is what translates these raw shapes into the app's own
`entity/Movie` and `dto/response/MovieResponse`/`MovieSearchResultResponse` —
this package never touches those types directly.
