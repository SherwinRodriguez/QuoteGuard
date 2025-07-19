'use client';

import { useEffect, useState } from 'react';

export default function ProfilePage() {
  const [email, setEmail] = useState('');
  const [name, setName] = useState('');

  useEffect(() => {
    // Temporary localStorage values — assume you stored this during login
    const storedEmail = localStorage.getItem('email');
    const storedName = localStorage.getItem('name');

    if (storedEmail) setEmail(storedEmail);
    if (storedName) setName(storedName);
  }, []);

  return (
    <div className="min-h-screen px-6 py-10 bg-gray-100">
      <div className="bg-white p-6 rounded-xl shadow-md max-w-xl mx-auto space-y-4">
        <h1 className="text-2xl font-bold text-blue-800 mb-4">👤 Profile</h1>

        <div className="space-y-2">
          <div>
            <span className="font-semibold">Name:</span> {name || 'N/A'}
          </div>
          <div>
            <span className="font-semibold">Email:</span> {email || 'N/A'}
          </div>
        </div>
      </div>
    </div>
  );
}