'use client';

import { useEffect } from 'react';
import { useAuth } from '@/context/AuthContext';
import { useRouter } from 'next/navigation';

export default function DashboardPage() {
  const { isLoggedIn } = useAuth();
  const router = useRouter();

  useEffect(() => {
    if (!isLoggedIn) {
      router.push('/login');
    }
  }, [isLoggedIn,router]);

  return (
    <div className="min-h-screen px-6 py-8 bg-gray-100">
      <h1 className="text-3xl font-bold mb-6 text-blue-800">Welcome to QuoteGuard Dashboard</h1>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <div className="bg-white p-6 rounded-xl shadow-md">
          <h2 className="text-lg font-semibold text-gray-700 mb-2">Total Clients</h2>
          <p className="text-2xl font-bold text-blue-600">0</p>
        </div>
        <div className="bg-white p-6 rounded-xl shadow-md">
          <h2 className="text-lg font-semibold text-gray-700 mb-2">Invoices Created</h2>
          <p className="text-2xl font-bold text-green-600">0</p>
        </div>
        <div className="bg-white p-6 rounded-xl shadow-md">
          <h2 className="text-lg font-semibold text-gray-700 mb-2">Pending Verifications</h2>
          <p className="text-2xl font-bold text-red-600">0</p>
        </div>
      </div>
    </div>
  );
}