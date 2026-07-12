// Reads the payload of the access token issued by JwtService.generateToken()
// on the backend (subject = user ID, "email" claim - see
// backend/src/main/java/com/quoteguard/security/JwtService.java). This is a
// client-side CONVENIENCE decode only - it does NOT verify the signature.
// It must never be used as a security boundary; it exists purely so the UI
// can show "logged in as x@example.com" and know when to proactively treat
// a token as stale, without an extra network round trip. The server
// verifies the signature on every single request regardless of what the
// client believes.
export interface DecodedAccessToken {
  sub: string; // user ID, as a string (JWT subject claims are always strings)
  email: string;
  iat: number;
  exp: number;
}

export function decodeAccessToken(token: string): DecodedAccessToken | null {
  try {
    const payload = token.split('.')[1];
    if (!payload) return null;

    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const json =
      typeof window !== 'undefined'
        ? window.atob(base64)
        : Buffer.from(base64, 'base64').toString('utf-8');

    return JSON.parse(json) as DecodedAccessToken;
  } catch {
    return null;
  }
}

export function isTokenExpired(token: string): boolean {
  const decoded = decodeAccessToken(token);
  if (!decoded) return true;
  return decoded.exp * 1000 <= Date.now();
}
