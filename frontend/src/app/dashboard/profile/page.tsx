'use client';

import { useAuth } from '@/context/AuthContext';

/**
 * The previous version of this page read "name"/"email" out of localStorage
 * keys that nothing ever wrote to (login only ever stored a fake "token"
 * string and a raw userId - see the AuthContext/tokenStorage rewrite), so
 * this always rendered "N/A" for both fields.
 *
 * Email now comes from the real JWT's "email" claim (decoded client-side,
 * see lib/jwt.ts). There is currently no backend endpoint that returns the
 * user's stored name/profile (no GET /api/users/me) and no name claim in
 * the token - adding one is a backend change, out of scope for this
 * frontend-only phase, so it's called out here rather than faked.
 */
export default function ProfilePage() {
  const { email, userId } = useAuth();

  return (
    <div className="min-h-screen px-6 py-10 bg-gray-100">
      <div className="bg-white p-8 rounded-xl shadow-md max-w-2xl mx-auto mt-16">
        <h1 className="text-3xl font-bold text-blue-800 mb-8">👤 Profile</h1>

        <div className="space-y-6">
          <div className="border-b pb-4">
            <p className="text-sm font-medium text-gray-600 mb-1">Email</p>
            <p className="text-lg font-semibold text-gray-900">{email || 'N/A'}</p>
          </div>
          <div className="border-b pb-4">
            <p className="text-sm font-medium text-gray-600 mb-1">Account ID</p>
            <p className="text-lg font-semibold text-gray-900">{userId ?? 'N/A'}</p>
          </div>
          <div className="pt-4">
            <p className="text-sm text-gray-600">
              To update your profile information, please contact support.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
