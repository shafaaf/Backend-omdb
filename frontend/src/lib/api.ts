import type { ErrorResponse } from '../types';

const API_BASE = 'http://localhost:8080/api';

/** Thrown by apiRequest for any non-2xx response; carries the HTTP status and, for validation failures, per-field messages. */
export class ApiError extends Error {
  status: number;
  fieldErrors: Record<string, string> | null;

  constructor(message: string, status: number, fieldErrors: Record<string, string> | null = null) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

interface RequestOptions {
  method?: string;
  body?: unknown;
  /** Attached as the Idempotency-Key header — see lib/idempotency.ts. */
  idempotencyKey?: string;
}

/** Masks password fields before a request body hits the console — everything else about
 *  a request is useful to see in activity logs, but credentials never should be. */
function redactBody(body: unknown): unknown {
  if (!body || typeof body !== 'object') return body;
  const clone: Record<string, unknown> = { ...(body as Record<string, unknown>) };
  if ('password' in clone) clone.password = '[redacted]';
  return clone;
}

// Sends one request to the backend with the standard headers/credentials.
//
// No token handling here at all — auth cookies are httpOnly, set by the backend
// (see AuthController), so the browser attaches them automatically via
// credentials: 'include'. This is the practical payoff of that storage choice:
// there is nothing for this file to read, store, or accidentally leak.
async function rawRequest(path: string, options: RequestOptions = {}): Promise<Response> {
  const headers: Record<string, string> = {
    // Required by the backend's CsrfHeaderFilter on every state-changing request —
    // a cross-site form/img can't attach a custom header, so this alone rules out
    // the classic CSRF forgery even with SameSite=Lax cookies. See CLAUDE.md.
    'X-Requested-With': 'XMLHttpRequest',
  };
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  if (options.idempotencyKey) {
    headers['Idempotency-Key'] = options.idempotencyKey;
  }

  return fetch(`${API_BASE}${path}`, {
    method: options.method ?? 'GET',
    credentials: 'include',
    headers,
    body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
  });
}

let refreshInFlight: Promise<boolean> | null = null;

/** Dedupes concurrent refresh attempts if several requests 401 at once. */
function refreshAccessToken(): Promise<boolean> {
  if (!refreshInFlight) {
    refreshInFlight = rawRequest('/auth/refresh', { method: 'POST' })
      .then((res) => res.ok)
      .finally(() => {
        refreshInFlight = null;
      });
  }
  return refreshInFlight;
}

/**
 * A 401 triggers exactly one /auth/refresh + retry — never an unbounded loop —
 * so an expired access token is transparently renewed mid-session, but a
 * genuinely invalid session still surfaces as a normal ApiError the caller
 * (AuthContext, page components) can react to.
 */
export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const method = options.method ?? 'GET';
  console.log(`[api] request: ${method} ${path}`, options.body !== undefined ? redactBody(options.body) : '');
  let response = await rawRequest(path, options);
  console.log(`[api] response: ${method} ${path} -> ${response.status}`);

  if (response.status === 401 && path !== '/auth/refresh' && path !== '/auth/login') {
    console.log(`[api] ${method} ${path} got 401, attempting token refresh`);
    const refreshed = await refreshAccessToken();
    if (refreshed) {
      console.log(`[api] token refreshed, retrying ${method} ${path}`);
      response = await rawRequest(path, options);
      console.log(`[api] response: ${method} ${path} retry -> ${response.status}`);
    } else {
      console.log('[api] token refresh failed');
    }
  }

  if (!response.ok) {
    const body: ErrorResponse | null = await response.json().catch(() => null);
    console.warn(`[api] ${method} ${path} failed with ${response.status}: ${body?.message ?? 'no message'}`, body);
    throw new ApiError(
      body?.message ?? `Request failed (${response.status})`,
      response.status,
      body?.fieldErrors ?? null,
    );
  }

  if (response.status === 204) {
    console.log(`[api] ${method} ${path} -> 204 No Content`);
    return undefined as T;
  }
  const data = await response.json();
  console.log(`[api] ${method} ${path} response body:`, data);
  return data;
}
