// Centralizes access to the two tokens issued by POST /api/auth/login:
// a short-lived access token (sent as a Bearer header on every request) and
// a long-lived, rotating refresh token (sent only to /api/auth/refresh and
// /api/auth/logout). Previously the app stored neither of these - login
// just saved the literal success MESSAGE STRING from the old ad hoc
// response shape under the key "token", and a separate unsigned "userId"
// read straight from localStorage, trusted with no verification at all.
// Every API call now authenticates with the real, server-issued,
// cryptographically signed JWT instead.
const ACCESS_TOKEN_KEY = 'qg_access_token';
const REFRESH_TOKEN_KEY = 'qg_refresh_token';

function isBrowser(): boolean {
  return typeof window !== 'undefined';
}

export const tokenStorage = {
  getAccessToken(): string | null {
    if (!isBrowser()) return null;
    return localStorage.getItem(ACCESS_TOKEN_KEY);
  },

  getRefreshToken(): string | null {
    if (!isBrowser()) return null;
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  },

  setTokens(accessToken: string, refreshToken: string): void {
    if (!isBrowser()) return;
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken);
  },

  clear(): void {
    if (!isBrowser()) return;
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    // Best-effort cleanup of the old, pre-refactor keys so returning users
    // don't end up with stale, meaningless values sitting in storage.
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
  },
};
