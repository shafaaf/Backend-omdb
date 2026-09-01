import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { apiRequest, ApiError } from '../lib/api';
import { useAuth } from '../context/AuthContext';
import { AddToListButton } from '../components/AddToListButton';
import type { FavoriteListResponse, MovieResponse } from '../types';

/**
 * Public route — GET /api/movies/{imdbId} requires no auth on the backend
 * (see MovieController), matching real IMDb: anyone can look up a movie, only
 * adding it to a list needs an account.
 */
export function MovieDetailPage() {
  const { imdbId } = useParams<{ imdbId: string }>();
  const { user } = useAuth();
  const [movie, setMovie] = useState<MovieResponse | null>(null);
  const [lists, setLists] = useState<FavoriteListResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    setLoading(true);
    setError('');
    apiRequest<MovieResponse>(`/movies/${imdbId}`)
      .then(setMovie)
      .catch((err) => setError(err instanceof ApiError ? err.message : 'Failed to load movie'))
      .finally(() => setLoading(false));
  }, [imdbId]);

  useEffect(() => {
    if (!user) {
      setLists([]);
      return;
    }
    apiRequest<FavoriteListResponse[]>('/lists').then(setLists).catch(() => {});
  }, [user]);

  if (loading) return <p className="page-loading">Loading…</p>;
  if (error) return <p className="form-error">{error}</p>;
  if (!movie) return null;

  return (
    <div className="movie-detail-page">
      <Link to="/search" className="back-link">
        &larr; Back to search
      </Link>
      <div className="movie-detail">
        {movie.posterUrl ? (
          <img src={movie.posterUrl} alt={movie.title} className="movie-detail-poster" />
        ) : (
          <div className="movie-detail-poster movie-card-poster-placeholder">No image</div>
        )}
        <div className="movie-detail-body">
          <h1>
            {movie.title} {movie.releaseYear && <span className="movie-detail-year">({movie.releaseYear})</span>}
          </h1>
          {movie.genre && <p className="movie-detail-meta">{movie.genre}</p>}
          {movie.director && (
            <p className="movie-detail-meta">
              <strong>Director:</strong> {movie.director}
            </p>
          )}
          {movie.imdbRating && (
            <p className="movie-detail-meta">
              <strong>IMDb rating:</strong> {movie.imdbRating}
            </p>
          )}
          {movie.plot && <p className="movie-detail-plot">{movie.plot}</p>}

          {user ? (
            <AddToListButton imdbId={movie.externalId} lists={lists} />
          ) : (
            <Link to="/login" className="movie-card-login-hint">
              Log in to add to a list
            </Link>
          )}
        </div>
      </div>
    </div>
  );
}
