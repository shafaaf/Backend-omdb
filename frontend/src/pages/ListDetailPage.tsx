import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { apiRequest, ApiError } from '../lib/api';
import type { FavoriteListItemResponse, FavoriteListResponse } from '../types';

/** Shows one favorite list's movies; lets the user remove a movie from it. */
export function ListDetailPage() {
  const { listId } = useParams<{ listId: string }>();
  const [list, setList] = useState<FavoriteListResponse | null>(null);
  const [items, setItems] = useState<FavoriteListItemResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  /** Reloads the list and its items together. */
  async function refresh() {
    const [listData, itemsData] = await Promise.all([
      apiRequest<FavoriteListResponse>(`/lists/${listId}`),
      apiRequest<FavoriteListItemResponse[]>(`/lists/${listId}/movies`),
    ]);
    setList(listData);
    setItems(itemsData);
  }

  useEffect(() => {
    setLoading(true);
    refresh()
      .catch((err) => setError(err instanceof ApiError ? err.message : 'Failed to load list'))
      .finally(() => setLoading(false));
  }, [listId]);

  /** Removes one movie from this list, then reloads. */
  async function handleRemove(imdbId: string) {
    console.log(`[lists] user removing movie ${imdbId} from list ${listId}`);
    try {
      await apiRequest(`/lists/${listId}/movies/${imdbId}`, { method: 'DELETE' });
      await refresh();
      console.log(`[lists] removed movie ${imdbId} from list ${listId}`);
    } catch {
      console.warn(`[lists] failed to remove movie ${imdbId} from list ${listId}`);
      setError('Failed to remove movie');
    }
  }

  if (loading) return <p className="page-loading">Loading…</p>;
  if (error) return <p className="form-error">{error}</p>;

  return (
    <div className="list-detail-page">
      <Link to="/lists" className="back-link">
        &larr; Back to lists
      </Link>
      <h1>{list?.name}</h1>
      {items.length === 0 ? (
        <p>No movies yet — add some from Search.</p>
      ) : (
        <div className="movie-grid">
          {items.map((item) => (
            <div key={item.id} className="movie-card">
              {item.movie.posterUrl ? (
                <img src={item.movie.posterUrl} alt={item.movie.title} className="movie-card-poster" />
              ) : (
                <div className="movie-card-poster movie-card-poster-placeholder">No image</div>
              )}
              <div className="movie-card-body">
                <h3>{item.movie.title}</h3>
                {item.movie.releaseYear && <p className="movie-card-year">{item.movie.releaseYear}</p>}
                <button onClick={() => handleRemove(item.movie.externalId)}>Remove</button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
