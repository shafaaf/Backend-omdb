import { useRef, useState } from 'react';
import { apiRequest, ApiError } from '../lib/api';
import { newIdempotencyKey } from '../lib/idempotency';
import type { FavoriteListItemResponse, FavoriteListResponse } from '../types';

interface Props {
  imdbId: string;
  lists: FavoriteListResponse[];
}

type Status = 'idle' | 'saving' | 'done' | 'error';

/** List picker + "Add" button for adding one movie to one of the user's lists. */
export function AddToListButton({ imdbId, lists }: Props) {
  const [selectedListId, setSelectedListId] = useState<number | ''>(lists[0]?.id ?? '');
  const [status, setStatus] = useState<Status>('idle');
  const [errorMessage, setErrorMessage] = useState('');

  // Generated once for this logical "add" action and reused across any retries
  // of it — see lib/idempotency.ts. Only cleared on success, so a failed
  // request followed by the user clicking "Add" again replays safely instead
  // of risking a duplicate list entry.
  const idempotencyKeyRef = useRef<string | null>(null);

  /** Sends the add-to-list request for the selected list, reusing the same idempotency key on retry. */
  async function handleAdd() {
    if (!selectedListId) return;
    idempotencyKeyRef.current ??= newIdempotencyKey();

    setStatus('saving');
    setErrorMessage('');
    try {
      await apiRequest<FavoriteListItemResponse>(`/lists/${selectedListId}/movies`, {
        method: 'POST',
        body: { imdbId },
        idempotencyKey: idempotencyKeyRef.current,
      });
      setStatus('done');
      idempotencyKeyRef.current = null;
    } catch (err) {
      setStatus('error');
      setErrorMessage(err instanceof ApiError ? err.message : 'Failed to add movie');
    }
  }

  if (lists.length === 0) {
    return <span className="add-to-list-hint">Create a list first</span>;
  }

  return (
    <div className="add-to-list">
      <select
        value={selectedListId}
        onChange={(e) => setSelectedListId(Number(e.target.value))}
        disabled={status === 'saving'}
      >
        {lists.map((list) => (
          <option key={list.id} value={list.id}>
            {list.name}
          </option>
        ))}
      </select>
      <button onClick={handleAdd} disabled={status === 'saving' || status === 'done'}>
        {status === 'done' ? 'Added' : status === 'saving' ? 'Adding…' : 'Add'}
      </button>
      {status === 'error' && <p className="add-to-list-error">{errorMessage}</p>}
    </div>
  );
}
