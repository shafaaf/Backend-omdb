import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { AddToListButton } from './AddToListButton';
import * as apiModule from '../lib/api';
import * as idempotencyModule from '../lib/idempotency';

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

vi.mock('../lib/idempotency', () => ({
  newIdempotencyKey: vi.fn(),
}));

describe('AddToListButton', () => {
  const mockLists = [
    { id: 1, name: 'Favorites', itemCount: 3, createdAt: '2026-09-04T00:00:00Z' },
    { id: 2, name: 'To Watch', itemCount: 5, createdAt: '2026-09-04T00:00:00Z' },
  ];

  let mockApiRequest: ReturnType<typeof vi.fn>;
  let mockNewIdempotencyKey: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    mockApiRequest = vi.fn();
    mockNewIdempotencyKey = vi.fn().mockReturnValue('key-12345');
    // @ts-ignore
    apiModule.apiRequest = mockApiRequest;
    // @ts-ignore
    idempotencyModule.newIdempotencyKey = mockNewIdempotencyKey;
  });

  it('should render select dropdown with lists and Add button', () => {
    render(<AddToListButton imdbId="tt0111161" lists={mockLists} />);

    const select = screen.getByRole('combobox');
    expect(select).toHaveTextContent('Favorites');
    expect(select).toHaveTextContent('To Watch');

    const button = screen.getByRole('button', { name: /Add/i });
    expect(button).toBeInTheDocument();
  });

  it('should send POST request with correct parameters', async () => {
    mockApiRequest.mockResolvedValueOnce({
      id: 1,
      movie: {},
      addedAt: '2026-09-04T00:00:00Z',
    });

    const user = userEvent.setup();
    render(<AddToListButton imdbId="tt0111161" lists={mockLists} />);

    const select = screen.getByRole('combobox');
    await user.selectOptions(select, '2');

    const button = screen.getByRole('button', { name: /Add/i });
    await user.click(button);

    await waitFor(() => {
      expect(mockApiRequest).toHaveBeenCalledWith(
        '/lists/2/movies',
        expect.objectContaining({
          method: 'POST',
          body: { imdbId: 'tt0111161' },
          idempotencyKey: 'key-12345',
        })
      );
    });
  });

  it('should show Added status after successful request', async () => {
    mockApiRequest.mockResolvedValueOnce({
      id: 1,
      movie: {},
      addedAt: '2026-09-04T00:00:00Z',
    });

    const user = userEvent.setup();
    render(<AddToListButton imdbId="tt0111161" lists={mockLists} />);

    const button = screen.getByRole('button', { name: /Add/i });
    await user.click(button);

    await waitFor(() => {
      expect(screen.getByText('Added')).toBeInTheDocument();
    });
  });

  it('should show error message on failed request', async () => {
    const error = new (apiModule.ApiError as any)('Movie not found', 404);
    mockApiRequest.mockRejectedValueOnce(error);

    const user = userEvent.setup();
    render(<AddToListButton imdbId="tt0111161" lists={mockLists} />);

    const button = screen.getByRole('button', { name: /Add/i });
    await user.click(button);

    await waitFor(() => {
      expect(screen.getByText('Movie not found')).toBeInTheDocument();
    });
  });

  it('should reuse idempotency key on retry', async () => {
    mockApiRequest.mockRejectedValueOnce(new Error('Network error'));
    const mockResponse = { id: 1, movie: {}, addedAt: '2026-09-04T00:00:00Z' };
    mockApiRequest.mockResolvedValueOnce(mockResponse);

    const user = userEvent.setup();
    render(<AddToListButton imdbId="tt0111161" lists={mockLists} />);

    const button = screen.getByRole('button', { name: /Add/i });

    // First attempt
    await user.click(button);
    await waitFor(() => {
      expect(screen.getByText(/Failed to add movie/)).toBeInTheDocument();
    });

    // Key should have been generated once
    expect(mockNewIdempotencyKey).toHaveBeenCalledTimes(1);

    // Retry - key should be reused
    await user.click(button);
    await waitFor(() => {
      expect(screen.getByText('Added')).toBeInTheDocument();
    });

    // newIdempotencyKey should NOT have been called again
    expect(mockNewIdempotencyKey).toHaveBeenCalledTimes(1);
  });

  it('should show hint when no lists available', () => {
    render(<AddToListButton imdbId="tt0111161" lists={[]} />);

    expect(screen.getByText('Create a list first')).toBeInTheDocument();
    expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
  });

  it('should disable select while saving', async () => {
    // Mock a slow request
    mockApiRequest.mockImplementation(
      () => new Promise((resolve) => setTimeout(() => resolve({}), 100))
    );

    const user = userEvent.setup();
    render(<AddToListButton imdbId="tt0111161" lists={mockLists} />);

    const select = screen.getByRole('combobox');
    const button = screen.getByRole('button', { name: /Add/i });

    await user.click(button);

    // Select should be disabled while saving
    expect(select).toBeDisabled();

    // Wait for request to complete
    await waitFor(() => {
      expect(select).not.toBeDisabled();
    });
  });
});
