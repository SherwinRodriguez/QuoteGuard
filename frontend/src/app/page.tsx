'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import About from '@/components/layout/About';

export default function Page() {
  const router = useRouter();

  const handleGetStarted = () => {
    const userId = localStorage.getItem("userId");
    if (userId) {
      router.push('/dashboard'); // ✅ if logged in
    } else {
      router.push('/login'); // 🔐 redirect to login if not
    }
  };

  return (
    <div>
      <div className="min-h-screen flex flex-col items-center justify-center bg-[#ecf0f1] space-y-6">
        <h1 className="text-5xl font-bold text-blue-900">Quote Guard</h1>
        <p className="text-lg text-gray-700 text-center max-w-xl">
          Securely generate, manage, and track professional invoices with built-in PDF and QR code support.
        </p>
        <button
          onClick={handleGetStarted}
          className="bg-blue-600 text-white px-6 py-3 rounded-lg hover:bg-blue-700 transition"
        >
          🚀 Get Started
        </button>
      </div>

      <About />
    </div>
  );
}