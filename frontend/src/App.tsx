import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import { NavBar } from './components/NavBar';
import { LoginPage } from './pages/LoginPage';
import { SignupPage } from './pages/SignupPage';
import { SearchPage } from './pages/SearchPage';
import { MovieDetailPage } from './pages/MovieDetailPage';
import { ListsPage } from './pages/ListsPage';
import { ListDetailPage } from './pages/ListDetailPage';

/** Top-level route table. /search and /movies/:imdbId are public; /lists and
 *  /lists/:listId require login (wrapped in ProtectedRoute). */
function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <NavBar />
        <main className="app-main">
          <Routes>
            <Route path="/" element={<Navigate to="/search" replace />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/signup" element={<SignupPage />} />
            <Route path="/search" element={<SearchPage />} />
            <Route path="/movies/:imdbId" element={<MovieDetailPage />} />
            <Route element={<ProtectedRoute />}>
              <Route path="/lists" element={<ListsPage />} />
              <Route path="/lists/:listId" element={<ListDetailPage />} />
            </Route>
            <Route path="*" element={<Navigate to="/search" replace />} />
          </Routes>
        </main>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
