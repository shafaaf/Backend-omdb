import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { AddToListButton } from './AddToListButton';
import type { FavoriteListResponse, MovieSearchResultResponse } from '../types';

interface Props {
  movie: MovieSearchResultResponse;
  lists: FavoriteListResponse[];
}

/** One search-result card: poster, title, year, and the add-to-list control (or a login hint). */
export function MovieCard({ movie, lists }: Props) {
  const { user } = useAuth();

  return (
    <div className="movie-card">
      <Link to={`/movies/${movie.externalId}`} className="movie-card-link">
        {movie.posterUrl ? (
          <img src={movie.posterUrl} alt={movie.title} className="movie-card-poster" />
        ) : (
          <div className="movie-card-poster movie-card-poster-placeholder">No image</div>
        )}
      </Link>
      <div className="movie-card-body">
        <h3>
          <Link to={`/movies/${movie.externalId}`} className="movie-card-title-link">
            {movie.title}
          </Link>
        </h3>
        {movie.releaseYear && <p className="movie-card-year">{movie.releaseYear}</p>}
        {user ? (
          <AddToListButton imdbId={movie.externalId} lists={lists} />
        ) : (
          <Link to="/login" className="movie-card-login-hint">
            Log in to add to a list
          </Link>
        )}
      </div>
    </div>
  );
}
