import { useEffect, useState, type FormEvent } from 'react';
import { apiRequest, ApiError } from '../lib/api';
import { FavoriteListCard } from '../components/FavoriteListCard';
import type { FavoriteListResponse } from '../types';

/** Create/list/delete the current user's favorite lists. */
export function ListsPage() {
  const [lists, setLists] = useState<FavoriteListResponse[]>([]);
  const [name, setName] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  /** Reloads the list of lists from the backend. */
  async function refresh() {
    const data = await apiRequest<FavoriteListResponse[]>('/lists');
    setLists(data);
  }

  useEffect(() => {
    refresh().finally(() => setLoading(false));
  }, []);

  async function handleCreate(e: FormEvent) {
    e.preventDefault();
    if (!name.trim()) return;
    setError('');
    try {
      await apiRequest('/lists', { method: 'POST', body: { name } });
      setName('');
      await refresh();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'Failed to create list');
    }
  }

  async function handleDelete(id: number) {
    try {
      await apiRequest(`/lists/${id}`, { method: 'DELETE' });
      await refresh();
    } catch {
      setError('Failed to delete list');
    }
  }

  return (
    <div className="lists-page">
      <h1>My Lists</h1>
      <form className="create-list-form" onSubmit={handleCreate}>
        <input
          type="text"
          placeholder="New list name"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        <button type="submit">Create</button>
      </form>
      {error && <p className="form-error">{error}</p>}
      {loading ? (
        <p>Loading…</p>
      ) : lists.length === 0 ? (
        <p>No lists yet — create one above.</p>
      ) : (
        <div className="list-grid">
          {lists.map((list) => (
            <FavoriteListCard key={list.id} list={list} onDelete={handleDelete} />
          ))}
        </div>
      )}
    </div>
  );
}
