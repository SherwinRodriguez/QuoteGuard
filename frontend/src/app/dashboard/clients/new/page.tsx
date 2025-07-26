'use client';

import { useRouter } from 'next/navigation';
import { useState } from 'react';

export default function NewClientPage() {
  const router = useRouter();
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    gstin: '',
    phone: '',
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

const handleSubmit = async (e: React.FormEvent) => {
  e.preventDefault();


  try {
      const userId = localStorage.getItem("userId"); // must be stored at login
      const res = await fetch(`https://quoteguard-backend.onrender.com/api/clients?userId=${userId}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(formData),
});

    if (!res.ok) throw new Error('Failed to create client');

    alert('✅ Client created');
    router.push('/dashboard/clients');
  } catch (err) {
    alert('❌ Error creating client');
    console.error(err);
  }
};

  return (
    <div className="min-h-screen px-6 pt-28 pb-8 text-black bg-gray-100">
      <h1 className="text-2xl font-bold text-blue-800 mb-6">Add New Client</h1>

      <form
        onSubmit={handleSubmit}
        className="space-y-4 max-w-xl bg-white p-6 rounded-xl shadow-md"
      >
        {['name', 'email', 'gstin', 'phone'].map((field) => (
          <div key={field}>
            <label className="block text-sm font-medium mb-1 capitalize">
              {field}
            </label>
            <input
              type="text"
              name={field}
              value={formData[field as keyof typeof formData]}
              onChange={handleChange}
              required
              className="w-full border px-3 py-2 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
        ))}

        <button
          type="submit"
          className="bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 transition"
        >
          Save Client
        </button>
      </form>
    </div>
  );
}