# mapper/

Hand-written, static, stateless mapping between entities and DTOs — no
MapStruct. That's deliberate: a generated mapper removes boilerplate but hides
the exact field-by-field translation behind annotation processing, which is
worse for a codebase meant to be read line by line. A real production team
with dozens of these would likely reach for MapStruct instead.

| File | Methods |
|---|---|
| `UserMapper.java` | `toResponse(User)` → `UserResponse` |
| `MovieMapper.java` | `fromOmdb(OmdbMovieResponse)` → builds a not-yet-saved `Movie`; `toResponse(Movie)` → `MovieResponse`; `toSearchResult(OmdbSearchItem)` → `MovieSearchResultResponse`. Private helpers `parseYear` (handles OMDb's `"2015–2019"` range format) and `nullIfNotAvailable` (OMDb uses the literal string `"N/A"` for missing fields). |
| `FavoriteListMapper.java` | `toResponse(FavoriteList, itemCount)` → `FavoriteListResponse` (item count passed in rather than derived from the lazy `items` collection, to avoid loading it just to count it); `toItemResponse(FavoriteListItem)` → `FavoriteListItemResponse` |
