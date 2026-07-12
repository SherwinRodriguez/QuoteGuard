// Single fetch wrapper for the whole app. Before this, every page had its
// own copy-pasted fetch() call with a hardcoded base URL, no auth header,
// and no shared error handling - 9+ independent implementations of the same
// thing, none of which worked against the JWT-authenticated backend built
// in Phase A. This is the one place that now knows how to:
//   - build the request URL from the configured API_BASE_URL
//   - attach "Authorization: Bearer <accessToken>" automatically
//   - transparently refresh an expired access token ONCE and retry on a 401
//   - parse RFC 9457 application/problem+json error bodies into ApiError
'use client';

import { API_BASE_URL } from './env';
import { ApiError } from './apiError';
import { tokenStorage } from './tokenStorage';

export interface ApiRequestOptions extends Omit<RequestInit, 'body'> {
  /** JSON-serializable request body. Omit for GET/DELETE with no body. */
  body?: unknown;
  /**
   * Skips attaching the Authorization header and skips the 401-refresh-retry
   * flow. Use for the auth endpoints themselves (login/register/refresh/
   * logout) and the public invoice-verification endpoint - all five are
   * explicitly permitAll in SecurityConfig and don't take an access token.
   */
  skipAuth?: boolean;
  /** How to parse a successful response body. Defaults to "json". */
  responseType?: 'json' | 'text' | 'blob' | 'void';
}

// Concurrent requests that all hit a 401 at the same moment (e.g. a page
// that fires off three parallel fetches right as the access token expires)
// must trigger exactly ONE refresh call, not one per failed request - every
// caller awaits this same in-flight promise instead of racing the endpoint.
let refreshPromise: Promise<string | null> | null = null;

interface RawAuthResponse {
  accessToken: string;
  refreshToken: string;
}

async function performRefresh(): Promise<string | null> {
  const refreshToken = tokenStorage.getRefreshToken();
  if (!refreshToken) return null;

  try {
    const res = await fetch(`${API_BASE_URL}/api/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    });

    if (!res.ok) {
      tokenStorage.clear();
      return null;
    }

    const data = (await res.json()) as RawAuthResponse;
    tokenStorage.setTokens(data.accessToken, data.refreshToken);
    return data.accessToken;
  } catch {
    tokenStorage.clear();
    return null;
  }
}

function refreshAccessToken(): Promise<string | null> {
  if (!refreshPromise) {
    refreshPromise = performRefresh().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

async function toApiError(res: Response): Promise<ApiError> {
  let detail = res.statusText || `Request failed with status ${res.status}`;
  let fieldErrors: Record<string, string> | undefined;

  try {
    const body = await res.json();
    if (typeof body?.detail === 'string') detail = body.detail;
    if (body?.errors && typeof body.errors === 'object') fieldErrors = body.errors;
  } catch {
    // Not every error response is JSON (e.g. Spring Security's default 401
    // entry point can return an empty body) - fall back to statusText.
  }

  return new ApiError(res.status, detail, fieldErrors);
}

export async function apiRequest<T = unknown>(
  path: string,
  options: ApiRequestOptions = {}
): Promise<T> {
  const { body, skipAuth = false, responseType = 'json', headers, ...rest } = options;

  const doFetch = async (): Promise<Response> => {
    const finalHeaders = new Headers(headers);
    if (body !== undefined && !finalHeaders.has('Content-Type')) {
      finalHeaders.set('Content-Type', 'application/json');
    }
    if (!skipAuth) {
      const accessToken = tokenStorage.getAccessToken();
      if (accessToken) finalHeaders.set('Authorization', `Bearer ${accessToken}`);
    }

    return fetch(`${API_BASE_URL}${path}`, {
      ...rest,
      headers: finalHeaders,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  };

  let res = await doFetch();

  if (res.status === 401 && !skipAuth) {
    const newAccessToken = await refreshAccessToken();
    if (newAccessToken) {
      res = await doFetch();
    }
  }

  if (!res.ok) {
    throw await toApiError(res);
  }

  if (responseType === 'void' || res.status === 204) {
    return undefined as T;
  }
  if (responseType === 'blob') {
    return (await res.blob()) as unknown as T;
  }
  if (responseType === 'text') {
    return (await res.text()) as unknown as T;
  }
  return (await res.json()) as T;
}
