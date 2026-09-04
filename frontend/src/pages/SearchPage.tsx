import { useEffect, useState, type FormEvent } from 'react';
import { apiRequest, ApiError } from '../lib/api';
import { useAuth } from '../context/AuthContext';
import { usePageView } from '../lib/pageView';
import { MovieCard } from '../components/MovieCard';
import { FEATURED_IMDB_IDS } from '../lib/featuredMovies';
import type { FavoriteListResponse, MovieResponse, MovieSearchResultResponse } from '../types';

function toSearchResult(movie: MovieResponse): MovieSearchResultResponse {
  return {
    externalId: movie.externalId,
    title: movie.title,
    releaseYear: movie.releaseYear,
    posterUrl: movie.posterUrl,
  };
}

/** Public movie search page; shows the add-to-list control only when signed in. */
export function SearchPage() {
  usePageView('Search');
  const { user } = useAuth();
  const [title, setTitle] = useState('');
  const [results, setResults] = useState<MovieSearchResultResponse[]>([]);
  const [featured, setFeatured] = useState<MovieSearchResultResponse[]>([]);
  const [featuredLoading, setFeaturedLoading] = useState(true);
  const [lists, setLists] = useState<FavoriteListResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [searched, setSearched] = useState(false);

  // Movie search itself doesn't require auth (mirrors the backend's permitAll on
  // GET /api/movies/**), but the user's own lists are only fetched — and the
  // "add to list" control only shown — once logged in.
  useEffect(() => {
    if (!user) {
      setLists([]);
      return;
    }
    apiRequest<FavoriteListResponse[]>('/lists').then(setLists).catch(() => {});
  }, [user]);

  // Default "landing" content before a search is made. There's no "now playing"
  // endpoint on OMDb's free API — this is a fixed curated list, one GET
  // /movies/{imdbId} call per title. allSettled so one bad/missing id doesn't
  // blank out the rest of the grid.
  useEffect(() => {
    setFeaturedLoading(true);
    Promise.allSettled(FEATURED_IMDB_IDS.map((id) => apiRequest<MovieResponse>(`/movies/${id}`)))
      .then((outcomes) => {
        const loaded = outcomes
          .filter((outcome) => outcome.status === 'fulfilled')
          .map((outcome) => toSearchResult(outcome.value));
        setFeatured(loaded);
      })
      .finally(() => setFeaturedLoading(false));
  }, []);

  /** Runs the movie search and updates the results list. */
  async function handleSearch(e: FormEvent) {
    e.preventDefault();
    if (!title.trim()) return;
    setLoading(true);
    setError('');
    setSearched(true);
    console.log(`[search] user searched for: "${title}"`);
    try {
      const found = await apiRequest<MovieSearchResultResponse[]>(
        `/movies/search?title=${encodeURIComponent(title)}`,
      );
      setResults(found);
      console.log(`[search] "${title}" returned ${found.length} result(s)`);
    } catch (err) {
      console.warn(`[search] "${title}" failed`);
      setError(err instanceof ApiError ? err.message : 'Search failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="search-page">
      <form className="search-form" onSubmit={handleSearch}>
        <input
          type="text"
          placeholder="Search movies…"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
        />
        <button type="submit" disabled={loading}>
          {loading ? 'Searching…' : 'Search'}
        </button>
      </form>

      {searched ? (
        <>
          {error && <p className="form-error">{error}</p>}
          {!loading && !error && results.length === 0 && <p>No movies found.</p>}
          <div className="movie-grid">
            {results.map((movie) => (
              <MovieCard key={movie.externalId} movie={movie} lists={lists} />
            ))}
          </div>
        </>
      ) : (
        <>
          <h2 className="section-heading">Featured movies</h2>
          {featuredLoading ? (
            <p className="page-loading">Loading…</p>
          ) : featured.length === 0 ? (
            <p>Featured movies are unavailable right now — try searching above.</p>
          ) : (
            <div className="movie-grid">
              {featured.map((movie) => (
                <MovieCard key={movie.externalId} movie={movie} lists={lists} />
              ))}
            </div>
          )}
        </>
      )}
    </div>
  );
}
