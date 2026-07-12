'use client';

import { useAuth } from '@/context/AuthContext';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';

export default function ProtectedRoute({
  children,
}: {
  children: React.ReactNode;
}) {
  const { isLoggedIn, isLoading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    // Wait for the initial localStorage check to finish before deciding to
    // redirect - otherwise every page refresh briefly renders isLoggedIn as
    // false (its initial state) and bounces a genuinely logged-in user to
    // /login for a frame.
    if (!isLoading && !isLoggedIn) {
      router.replace('/login');
    }
  }, [isLoading, isLoggedIn, router]);

  if (isLoading || !isLoggedIn) return null;

  return <>{children}</>;
}