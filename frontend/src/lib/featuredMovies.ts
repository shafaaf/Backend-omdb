/**
 * A fixed list of IMDb ids shown on the search page before the user has typed
 * anything. OMDb's free API is search/lookup-only — it has no "now playing" or
 * "trending" endpoint — so this is a curated stand-in rather than a live feed.
 * Each id is fetched via GET /movies/{imdbId} (see SearchPage.tsx), which
 * caches it in the backend's Movie table on first load like any other lookup.
 */
export const FEATURED_IMDB_IDS: string[] = [
  'tt15398776', // Oppenheimer
  'tt1517268', // Barbie
  'tt1877830', // The Batman
  'tt6710474', // Everything Everywhere All at Once
  'tt10872600', // Spider-Man: No Way Home
  'tt1745960', // Top Gun: Maverick
  'tt1160419', // Dune
  'tt6751668', // Parasite
  'tt0468569', // The Dark Knight
  'tt1375666', // Inception
  'tt0816692', // Interstellar
  'tt0111161', // The Shawshank Redemption
];
