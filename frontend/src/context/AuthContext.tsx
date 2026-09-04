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
    console.log(`[auth] user attempting to log in: ${email}`);
    try {
      const result = await apiRequest<AuthResponse>('/auth/login', {
        method: 'POST',
        body: { email, password },
      });
      setUser(result.user);
      console.log(`[auth] login succeeded for: ${email}`);
    } catch (err) {
      console.warn(`[auth] login failed for: ${email}`);
      throw err;
    }
  }

  /** Creates a new account and updates the current user state on success. */
  async function signup(email: string, password: string, displayName: string) {
    console.log(`[auth] user attempting to sign up: ${email}`);
    try {
      const result = await apiRequest<AuthResponse>('/auth/signup', {
        method: 'POST',
        body: { email, password, displayName },
      });
      setUser(result.user);
      console.log(`[auth] signup succeeded for: ${email}`);
    } catch (err) {
      console.warn(`[auth] signup failed for: ${email}`);
      throw err;
    }
  }

  async function logout() {
    console.log('[auth] user logging out');
    // Best-effort: even if the network call fails, clear local state so the UI
    // reflects "logged out" immediately rather than getting stuck.
    await apiRequest('/auth/logout', { method: 'POST' }).catch(() => {
      console.warn('[auth] logout request failed, clearing local state anyway');
    });
    setUser(null);
    console.log('[auth] user logged out');
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
