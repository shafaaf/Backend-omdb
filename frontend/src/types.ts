// Mirrors the backend's dto/response and dto/request records exactly — see
// com.example.movielist.dto on the backend. Kept as one file since the surface
// is small; a larger app would generate these from an OpenAPI spec instead.

export interface UserResponse {
  id: number;
  email: string;
  displayName: string;
}

export interface AuthResponse {
  user: UserResponse;
  accessTokenExpiresAt: string;
}

export interface FavoriteListResponse {
  id: number;
  name: string;
  itemCount: number;
  createdAt: string;
}

export interface MovieResponse {
  id: number;
  externalId: string;
  title: string;
  releaseYear: number | null;
  posterUrl: string | null;
  plot: string | null;
  genre: string | null;
  director: string | null;
  imdbRating: string | null;
}

export interface MovieSearchResultResponse {
  externalId: string;
  title: string;
  releaseYear: number | null;
  posterUrl: string | null;
}

export interface FavoriteListItemResponse {
  id: number;
  movie: MovieResponse;
  addedAt: string;
}

export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors: Record<string, string> | null;
}
