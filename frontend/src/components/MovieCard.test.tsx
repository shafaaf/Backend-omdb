import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { BrowserRouter } from 'react-router-dom';
import { MovieCard } from './MovieCard';
import { AuthProvider } from '../context/AuthContext';
import * as apiModule from '../lib/api';

vi.mock('../lib/api', () => ({
  apiRequest: vi.fn(),
  ApiError: class ApiError extends Error {
    status: number;
    fieldErrors: Record<string, string> | null;
    constructor(message: string, status: number, fieldErrors = null) {
      super(message);
      this.status = status;
      this.fieldErrors = fieldErrors;
    }
  },
}));

describe('MovieCard', () => {
  const mockMovie = {
    externalId: 'tt0111161',
    title: 'The Shawshank Redemption',
    releaseYear: 1994,
    posterUrl: 'http://example.com/poster.jpg',
    imdbRating: '9.3',
  };

  const mockLists = [
    { id: 1, name: 'Favorites', itemCount: 3, createdAt: '2026-09-04T00:00:00Z' },
    { id: 2, name: 'To Watch', itemCount: 5, createdAt: '2026-09-04T00:00:00Z' },
  ];

  let mockApiRequest: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    mockApiRequest = vi.fn();
    // @ts-ignore
    apiModule.apiRequest = mockApiRequest;
  });

  it('should render movie title and year', async () => {
    mockApiRequest.mockResolvedValueOnce(null); // /auth/me returns not logged in

    render(
      <BrowserRouter>
        <AuthProvider>
          <MovieCard movie={mockMovie} lists={mockLists} />
        </AuthProvider>
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('The Shawshank Redemption')).toBeInTheDocument();
    });
    expect(screen.getByText('1994')).toBeInTheDocument();
  });

  it('should render poster image when posterUrl is provided', async () => {
    mockApiRequest.mockResolvedValueOnce(null);

    render(
      <BrowserRouter>
        <AuthProvider>
          <MovieCard movie={mockMovie} lists={mockLists} />
        </AuthProvider>
      </BrowserRouter>
    );

    const poster = await screen.findByAltText('The Shawshank Redemption');
    expect(poster).toHaveAttribute('src', 'http://example.com/poster.jpg');
  });

  it('should render placeholder when posterUrl is null', async () => {
    mockApiRequest.mockResolvedValueOnce(null);

    const movieWithoutPoster = { ...mockMovie, posterUrl: null };

    render(
      <BrowserRouter>
        <AuthProvider>
          <MovieCard movie={movieWithoutPoster} lists={mockLists} />
        </AuthProvider>
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('No image')).toBeInTheDocument();
    });
  });

  it('should show login hint when user is not authenticated', async () => {
    mockApiRequest.mockResolvedValueOnce(null); // /auth/me fails

    render(
      <BrowserRouter>
        <AuthProvider>
          <MovieCard movie={mockMovie} lists={mockLists} />
        </AuthProvider>
      </BrowserRouter>
    );

    const loginLink = await screen.findByText('Log in to add to a list');
    expect(loginLink).toHaveAttribute('href', '/login');
  });

  it('should show AddToListButton when user is authenticated', async () => {
    const mockUser = { id: 1, email: 'user@example.com', displayName: 'Test User' };
    mockApiRequest.mockResolvedValueOnce(mockUser);

    render(
      <BrowserRouter>
        <AuthProvider>
          <MovieCard movie={mockMovie} lists={mockLists} />
        </AuthProvider>
      </BrowserRouter>
    );

    // Wait for the component to load authenticated state and show the button
    const addButton = await screen.findByRole('button', { name: /Add/i });
    expect(addButton).toBeInTheDocument();

    // Should have a select dropdown with list options
    const select = screen.getByRole('combobox');
    expect(select).toHaveTextContent('Favorites');
    expect(select).toHaveTextContent('To Watch');
  });

  it('should link movie title to movie detail page', async () => {
    mockApiRequest.mockResolvedValueOnce(null);

    render(
      <BrowserRouter>
        <AuthProvider>
          <MovieCard movie={mockMovie} lists={mockLists} />
        </AuthProvider>
      </BrowserRouter>
    );

    await waitFor(() => {
      const titleLinks = screen.getAllByRole('link', {
        name: 'The Shawshank Redemption',
      });
      // Both the poster link and title link go to the same movie page
      expect(titleLinks.length).toBeGreaterThan(0);
      expect(titleLinks[0]).toHaveAttribute('href', '/movies/tt0111161');
    });
  });

  it('should not display release year if it is null', async () => {
    mockApiRequest.mockResolvedValueOnce(null);

    const movieWithoutYear = { ...mockMovie, releaseYear: null };

    const { container } = render(
      <BrowserRouter>
        <AuthProvider>
          <MovieCard movie={movieWithoutYear} lists={mockLists} />
        </AuthProvider>
      </BrowserRouter>
    );

    await waitFor(() => {
      const yearElement = container.querySelector('.movie-card-year');
      expect(yearElement).not.toBeInTheDocument();
    });
  });

  it('should display IMDb rating when present', async () => {
    mockApiRequest.mockResolvedValueOnce(null);

    render(
      <BrowserRouter>
        <AuthProvider>
          <MovieCard movie={mockMovie} lists={mockLists} />
        </AuthProvider>
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('★ 9.3')).toBeInTheDocument();
    });
  });

  it('should not display IMDb rating if it is null', async () => {
    mockApiRequest.mockResolvedValueOnce(null);

    const movieWithoutRating = { ...mockMovie, imdbRating: null };

    const { container } = render(
      <BrowserRouter>
        <AuthProvider>
          <MovieCard movie={movieWithoutRating} lists={mockLists} />
        </AuthProvider>
      </BrowserRouter>
    );

    await waitFor(() => {
      const ratingElement = container.querySelector('.movie-card-rating');
      expect(ratingElement).not.toBeInTheDocument();
    });
  });
});
