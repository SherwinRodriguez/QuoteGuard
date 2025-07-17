'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';

export default function NewClientPage() {
  const router = useRouter();

  const [formData, setFormData] = useState({
    name: '',
    email: '',
    phone: '',
    gstin: '',
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      const res = await fetch('http://localhost:8080/api/clients', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(formData),
      });

      if (!res.ok) throw new Error('Failed to add client');

      alert('✅ Client added successfully');
      router.push('/clients'); // or stay on same page if preferred
    } catch (err) {
      alert('❌ Error adding client');
      console.error(err);
    }
  };

  return (
    <div className="min-h-screen px-6 py-8 bg-gray-100">
      <h1 className="text-2xl font-bold text-blue-800 mb-6">Add New Client</h1>
      <form onSubmit={handleSubmit} className="space-y-4 max-w-xl bg-white p-6 rounded-xl shadow-md">
        {['name', 'email', 'phone', 'gstin'].map((field) => (
          <div key={field}>
            <label className="block text-sm font-medium mb-1 capitalize">{field}</label>
            <input
              type="text"
              name={field}
              value={formData[field as keyof typeof formData]}
              onChange={handleChange}
              className="w-full border px-3 py-2 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
              required
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