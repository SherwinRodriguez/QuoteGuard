'use client';

import { useState } from 'react';

export default function VerifyInvoicePage() {
  const [token, setToken] = useState('');
  const [result, setResult] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleVerify = async () => {
    if (!token.trim()) return;

    setLoading(true);
    setResult(null);

    try {
      const res = await fetch(`http://localhost:8080/api/invoices/verify/${token}`);
      const text = await res.text();
      setResult(text);
    } catch (err) {
      console.error('❌ Verification failed:', err);
      setResult('❌ Error verifying invoice');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen px-6 py-10 bg-gray-100 flex flex-col items-center justify-center">
      <h1 className="text-2xl font-bold text-blue-800 mb-4">Verify Invoice</h1>

      <div className="bg-white p-6 rounded-xl shadow-md w-full max-w-md space-y-4">
        <input
          type="text"
          placeholder="Enter QR Token"
          value={token}
          onChange={(e) => setToken(e.target.value)}
          className="w-full border px-4 py-2 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
        <button
          onClick={handleVerify}
          className="w-full bg-blue-600 text-white py-2 rounded-md hover:bg-blue-700 transition"
        >
          {loading ? 'Verifying...' : 'Verify'}
        </button>

        {result && (
          <div className={`text-center font-medium ${result.includes('✅') ? 'text-green-600' : 'text-red-600'}`}>
            {result}
          </div>
        )}
      </div>
    </div>
  );
}