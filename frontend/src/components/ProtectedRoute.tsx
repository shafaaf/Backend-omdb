import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/** Layout route that redirects to /login unless a user is signed in; otherwise renders its children. */
export function ProtectedRoute() {
  const { user, loading } = useAuth();

  if (loading) {
    return <p className="page-loading">Loading…</p>;
  }
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  return <Outlet />;
}
