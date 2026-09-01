import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/** Top navigation bar — shown on every page. */
export function NavBar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  async function handleLogout() {
    await logout();
    navigate('/login');
  }

  return (
    <nav className="navbar">
      <Link to="/search" className="navbar-brand">movielist</Link>
      <div className="navbar-links">
        <Link to="/search">Search</Link>
        {user && <Link to="/lists">My Lists</Link>}
      </div>
      <div className="navbar-user">
        {user ? (
          <>
            <span className="navbar-username">{user.displayName}</span>
            <button onClick={handleLogout}>Log out</button>
          </>
        ) : (
          <Link to="/login">Log in</Link>
        )}
      </div>
    </nav>
  );
}
