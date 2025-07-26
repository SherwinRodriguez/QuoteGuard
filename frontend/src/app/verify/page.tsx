'use client';

import { useState } from "react";
import { useRouter } from "next/navigation";

export default function VerifyPage() {
  const [token, setToken] = useState("");
  const router = useRouter();

  const handleVerify = () => {
    if (token.trim()) {
      router.push(`/verify/${token.trim()}`);
    }
  };

  return (
    <div className="pt-24 bg-black min-h-screen flex justify-center items-start">
        <div className="bg-white text-black p-6 rounded-xl shadow-xl w-full max-w-md">
            <div className="max-w-md mx-auto mt-32 bg-white p-6 rounded-lg shadow">
                <h1 className="text-2xl font-bold mb-4">🔍 Verify Invoice</h1>
                <input
                type="text"
                placeholder="Enter invoice token"
                value={token}
                onChange={(e) => setToken(e.target.value)}
                className="w-full px-4 py-2 border rounded-md mb-4"
                />
                <button
                onClick={handleVerify}
                className="w-full bg-blue-600 text-white py-2 rounded hover:bg-blue-700"
                >
                Verify
                </button>
            </div>
        </div>
    </div>
  );
}