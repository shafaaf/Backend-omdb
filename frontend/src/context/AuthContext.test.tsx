import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AuthProvider, useAuth } from './AuthContext';
import * as apiModule from '../lib/api';

// Mock the apiRequest function
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

// Test component that uses the auth hook
function TestComponent() {
  const { user, loading, login, logout, signup } = useAuth();

  if (loading) {
    return <div data-testid="loading-state">Loading...</div>;
  }

  return (
    <div>
      <div data-testid="user">{user ? `${user.email}` : 'not logged in'}</div>
      <button onClick={() => login('test@example.com', 'password123')}>
        Login
      </button>
      <button onClick={() => signup('new@example.com', 'password123', 'New User')}>
        Signup
      </button>
      <button onClick={() => logout()}>Logout</button>
    </div>
  );
}

describe('AuthContext', () => {
  let mockApiRequest: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    mockApiRequest = vi.fn();
    // @ts-ignore
    apiModule.apiRequest = mockApiRequest;
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('should load user on mount via GET /auth/me', async () => {
    const mockUser = { id: 1, email: 'user@example.com', displayName: 'Test User' };
    mockApiRequest.mockResolvedValueOnce(mockUser);

    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    );

    // Wait for loading to complete
    await waitFor(() => {
      expect(screen.getByTestId('user')).toHaveTextContent('user@example.com');
    });

    expect(mockApiRequest).toHaveBeenCalledWith('/auth/me');
  });

  it('should set user to null if /auth/me call fails', async () => {
    mockApiRequest.mockRejectedValueOnce(new Error('Network error'));

    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('user')).toHaveTextContent('not logged in');
    });
  });

  it('should update user on successful login', async () => {
    mockApiRequest.mockResolvedValueOnce(null); // initial /auth/me
    const loginResponse = {
      user: { id: 1, email: 'test@example.com', displayName: 'Test' },
      accessTokenExpiresAt: '2026-09-04T15:00:00Z',
    };
    mockApiRequest.mockResolvedValueOnce(loginResponse);

    const user = userEvent.setup();
    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('user')).toHaveTextContent('not logged in');
    });

    // Trigger login
    const loginButton = screen.getByText('Login');
    await user.click(loginButton);

    await waitFor(() => {
      expect(screen.getByTestId('user')).toHaveTextContent('test@example.com');
    });

    expect(mockApiRequest).toHaveBeenCalledWith('/auth/login', {
      method: 'POST',
      body: { email: 'test@example.com', password: 'password123' },
    });
  });

  it('should update user on successful signup', async () => {
    mockApiRequest.mockResolvedValueOnce(null); // initial /auth/me
    const signupResponse = {
      user: { id: 2, email: 'new@example.com', displayName: 'New User' },
      accessTokenExpiresAt: '2026-09-04T15:00:00Z',
    };
    mockApiRequest.mockResolvedValueOnce(signupResponse);

    const user = userEvent.setup();
    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('user')).toHaveTextContent('not logged in');
    });

    const signupButton = screen.getByText('Signup');
    await user.click(signupButton);

    await waitFor(() => {
      expect(screen.getByTestId('user')).toHaveTextContent('new@example.com');
    });

    expect(mockApiRequest).toHaveBeenCalledWith('/auth/signup', {
      method: 'POST',
      body: {
        email: 'new@example.com',
        password: 'password123',
        displayName: 'New User',
      },
    });
  });

  it('should clear user on logout', async () => {
    const mockUser = { id: 1, email: 'user@example.com', displayName: 'Test User' };
    mockApiRequest.mockResolvedValueOnce(mockUser); // initial /auth/me
    mockApiRequest.mockResolvedValueOnce(undefined); // logout call

    const user = userEvent.setup();
    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('user')).toHaveTextContent('user@example.com');
    });

    const logoutButton = screen.getByText('Logout');
    await user.click(logoutButton);

    await waitFor(() => {
      expect(screen.getByTestId('user')).toHaveTextContent('not logged in');
    });

    expect(mockApiRequest).toHaveBeenCalledWith('/auth/logout', { method: 'POST' });
  });

  it('should clear user even if logout call fails', async () => {
    const mockUser = { id: 1, email: 'user@example.com', displayName: 'Test User' };
    mockApiRequest.mockResolvedValueOnce(mockUser); // initial /auth/me
    mockApiRequest.mockRejectedValueOnce(new Error('Network error')); // logout fails

    const user = userEvent.setup();
    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId('user')).toHaveTextContent('user@example.com');
    });

    const logoutButton = screen.getByText('Logout');
    await user.click(logoutButton);

    // User should still be cleared even though the network call failed
    await waitFor(() => {
      expect(screen.getByTestId('user')).toHaveTextContent('not logged in');
    });
  });
});
