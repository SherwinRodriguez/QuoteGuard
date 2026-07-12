'use client';

import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { authService } from '@/services/auth';
import { tokenStorage } from '@/lib/tokenStorage';
import { decodeAccessToken } from '@/lib/jwt';

interface AuthContextType {
  isLoggedIn: boolean;
  /**
   * True until the initial localStorage check on mount has completed.
   * ProtectedRoute waits for this before redirecting to /login, so a
   * logged-in user doesn't see a flash-redirect on every page refresh.
   */
  isLoading: boolean;
  userId: number | null;
  email: string | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider = ({ children }: { children: React.ReactNode }) => {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [userId, setUserId] = useState<number | null>(null);
  const [email, setEmail] = useState<string | null>(null);
  const router = useRouter();

  const hydrateFromToken = useCallback((accessToken: string | null) => {
    if (!accessToken) {
      setIsLoggedIn(false);
      setUserId(null);
      setEmail(null);
      return;
    }
    const decoded = decodeAccessToken(accessToken);
    setIsLoggedIn(true);
    setUserId(decoded ? Number(decoded.sub) : null);
    setEmail(decoded?.email ?? null);
  }, []);

  useEffect(() => {
    hydrateFromToken(tokenStorage.getAccessToken());
    setIsLoading(false);
    // Only ever needs to run once, on mount - hydrateFromToken is stable
    // (useCallback with no deps) so this isn't missing a dependency.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const login = async (loginEmail: string, password: string) => {
    const response = await authService.login({ email: loginEmail, password });
    tokenStorage.setTokens(response.accessToken, response.refreshToken);
    hydrateFromToken(response.accessToken);
    router.push('/dashboard');
  };

  const logout = async () => {
    const refreshToken = tokenStorage.getRefreshToken();
    // Clear client-side state immediately - the user is "logged out" the
    // instant this runs, regardless of whether the best-effort server-side
    // revocation call below succeeds or fails.
    tokenStorage.clear();
    hydrateFromToken(null);
    router.push('/login');

    if (refreshToken) {
      authService.logout(refreshToken).catch(() => {
        // Refresh token may already be expired/revoked - nothing to do,
        // the client-side session is already gone.
      });
    }
  };

  return (
    <AuthContext.Provider value={{ isLoggedIn, isLoading, userId, email, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
