import { createContext, useContext, useEffect, useState, type ReactNode } from 'react';
import { apiRequest } from '../lib/api';
import type { AuthResponse, UserResponse } from '../types';

interface AuthContextValue {
  user: UserResponse | null;
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  signup: (email: string, password: string, displayName: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

/** Wraps the app, loads the current user on mount, and exposes auth actions via useAuth(). */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    apiRequest<UserResponse>('/auth/me')
      .then(setUser)
      .catch(() => setUser(null))
      .finally(() => setLoading(false));
  }, []);

  /** Logs the user in and updates the current user state on success. */
  async function login(email: string, password: string) {
    const result = await apiRequest<AuthResponse>('/auth/login', {
      method: 'POST',
      body: { email, password },
    });
    setUser(result.user);
  }

  /** Creates a new account and updates the current user state on success. */
  async function signup(email: string, password: string, displayName: string) {
    const result = await apiRequest<AuthResponse>('/auth/signup', {
      method: 'POST',
      body: { email, password, displayName },
    });
    setUser(result.user);
  }

  async function logout() {
    // Best-effort: even if the network call fails, clear local state so the UI
    // reflects "logged out" immediately rather than getting stuck.
    await apiRequest('/auth/logout', { method: 'POST' }).catch(() => {});
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, signup, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

/** Hook for reading auth state and calling login/signup/logout from any component. */
export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within AuthProvider');
  }
  return context;
}
