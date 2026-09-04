import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ProtectedRoute } from './ProtectedRoute';
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

describe('ProtectedRoute', () => {
  let mockApiRequest: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    mockApiRequest = vi.fn();
    // @ts-ignore
    apiModule.apiRequest = mockApiRequest;
  });

  it('should show loading state while checking auth', () => {
    // Mock /auth/me to never resolve
    mockApiRequest.mockImplementation(
      () => new Promise(() => {}) // Pending forever
    );

    render(
      <BrowserRouter>
        <AuthProvider>
          <Routes>
            <Route element={<ProtectedRoute />}>
              <Route path="/" element={<div>Protected Content</div>} />
            </Route>
            <Route path="/login" element={<div>Login Page</div>} />
          </Routes>
        </AuthProvider>
      </BrowserRouter>
    );

    expect(screen.getByText('Loading…')).toBeInTheDocument();
  });

  it('should render outlet when user is authenticated', async () => {
    const mockUser = { id: 1, email: 'user@example.com', displayName: 'Test User' };
    mockApiRequest.mockResolvedValueOnce(mockUser);

    render(
      <BrowserRouter initialEntries={['/']}>
        <AuthProvider>
          <Routes>
            <Route element={<ProtectedRoute />}>
              <Route path="/" element={<div>Protected Content</div>} />
            </Route>
            <Route path="/login" element={<div>Login Page</div>} />
          </Routes>
        </AuthProvider>
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Protected Content')).toBeInTheDocument();
    });
  });

  it('should redirect to login when user is not authenticated', async () => {
    mockApiRequest.mockRejectedValueOnce(new Error('Not authenticated'));

    render(
      <BrowserRouter initialEntries={['/']}>
        <AuthProvider>
          <Routes>
            <Route element={<ProtectedRoute />}>
              <Route path="/" element={<div>Protected Content</div>} />
            </Route>
            <Route path="/login" element={<div>Login Page</div>} />
          </Routes>
        </AuthProvider>
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(screen.getByText('Login Page')).toBeInTheDocument();
    });

    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
  });
});
