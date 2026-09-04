import { Link } from 'react-router-dom';
import type { FavoriteListResponse } from '../types';

interface Props {
  list: FavoriteListResponse;
  onDelete: (id: number) => void;
}

/** One list summary row: name (links to detail), item count, delete button. */
export function FavoriteListCard({ list, onDelete }: Props) {
  function handleClick() {
    console.log(`[lists] user clicked list: "${list.name}" (${list.id})`);
  }

  return (
    <div className="list-card">
      <Link to={`/lists/${list.id}`} className="list-card-name" onClick={handleClick}>
        {list.name}
      </Link>
      <span className="list-card-count">
        {list.itemCount} {list.itemCount === 1 ? 'movie' : 'movies'}
      </span>
      <button className="list-card-delete" onClick={() => onDelete(list.id)}>
        Delete
      </button>
    </div>
  );
}
