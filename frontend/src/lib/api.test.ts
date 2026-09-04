import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { apiRequest, ApiError } from './api';

describe('api.ts', () => {
  let fetchMock: typeof global.fetch;

  beforeEach(() => {
    fetchMock = vi.fn();
    global.fetch = fetchMock;
  });

  afterEach(() => {
    vi.resetAllMocks();
  });

  it('should make a successful GET request with credentials', async () => {
    const mockData = { id: 1, email: 'test@example.com' };
    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify(mockData), { status: 200 })
    );

    const result = await apiRequest<typeof mockData>('/auth/me');

    expect(result).toEqual(mockData);
    expect(fetchMock).toHaveBeenCalledWith('http://localhost:8080/api/auth/me', {
      method: 'GET',
      credentials: 'include',
      headers: {
        'X-Requested-With': 'XMLHttpRequest',
      },
      body: undefined,
    });
  });

  it('should include Idempotency-Key header when provided', async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify({}), { status: 200 })
    );

    await apiRequest('/lists/1/movies', {
      method: 'POST',
      body: { imdbId: 'tt0111161' },
      idempotencyKey: 'key-123',
    });

    const callArgs = fetchMock.mock.calls[0];
    const headers = callArgs[1].headers;
    expect(headers['Idempotency-Key']).toBe('key-123');
  });

  it('should set Content-Type header for POST requests with body', async () => {
    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify({}), { status: 200 })
    );

    await apiRequest('/auth/login', {
      method: 'POST',
      body: { email: 'test@example.com', password: 'pass123' },
    });

    const callArgs = fetchMock.mock.calls[0];
    const headers = callArgs[1].headers;
    expect(headers['Content-Type']).toBe('application/json');
  });

  it('should retry with refresh on 401, then succeed', async () => {
    const mockData = { id: 1, email: 'test@example.com' };

    // First call returns 401 (expired access token)
    // Second call to /auth/refresh returns 200 (refresh succeeds)
    // Third call (retry) returns 200 with data
    fetchMock
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(new Response(null, { status: 200 })) // refresh
      .mockResolvedValueOnce(new Response(JSON.stringify(mockData), { status: 200 })); // retry

    const result = await apiRequest<typeof mockData>('/lists/1');

    expect(result).toEqual(mockData);
    expect(fetchMock).toHaveBeenCalledTimes(3);
    // Verify the refresh call was made
    expect(fetchMock.mock.calls[1][0]).toContain('/auth/refresh');
  });

  it('should NOT retry refresh calls on 401', async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 401 }));

    try {
      await apiRequest('/auth/refresh', { method: 'POST' });
    } catch (err) {
      expect(err instanceof ApiError).toBe(true);
    }

    // Should only call once, no retry on refresh itself
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it('should throw ApiError with correct status on 404', async () => {
    const errorResponse = {
      status: 404,
      message: 'List not found',
      fieldErrors: null,
    };
    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify(errorResponse), { status: 404 })
    );

    try {
      await apiRequest('/lists/999');
      expect.fail('Should have thrown ApiError');
    } catch (err) {
      expect(err instanceof ApiError).toBe(true);
      if (err instanceof ApiError) {
        expect(err.status).toBe(404);
        expect(err.message).toBe('List not found');
      }
    }
  });

  it('should return undefined for 204 No Content response', async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));

    const result = await apiRequest('/auth/logout', { method: 'POST' });

    expect(result).toBeUndefined();
  });

  it('should parse field errors from error response', async () => {
    const errorResponse = {
      status: 400,
      message: 'Validation failed',
      fieldErrors: {
        email: 'Invalid email format',
        password: 'Password must be at least 8 characters',
      },
    };
    fetchMock.mockResolvedValueOnce(
      new Response(JSON.stringify(errorResponse), { status: 400 })
    );

    try {
      await apiRequest('/auth/signup', {
        method: 'POST',
        body: { email: 'bad', password: 'short' },
      });
    } catch (err) {
      expect(err instanceof ApiError).toBe(true);
      if (err instanceof ApiError) {
        expect(err.fieldErrors).toEqual({
          email: 'Invalid email format',
          password: 'Password must be at least 8 characters',
        });
      }
    }
  });
});
