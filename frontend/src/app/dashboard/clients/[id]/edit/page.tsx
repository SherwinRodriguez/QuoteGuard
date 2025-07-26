'use client';

import { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';

interface Client {
  id: number;
  name: string;
  email: string;
  phone: string;
  gstin: string;
}

export default function EditClientPage() {
  const params = useParams(); // ❌ don't wrap this with `use()`
  const router = useRouter();
  const [client, setClient] = useState<Client | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchClient = async () => {
      try {
        const res = await fetch(`https://quoteguard-backend.onrender.com/api/clients/${params.id}`);
        const data = await res.json();
        setClient(data);
      } catch (error) {
        console.error('Failed to load client:', error);
      } finally {
        setLoading(false);
      }
    };

    if (params?.id) fetchClient();
  }, [params?.id]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      const res = await fetch(`https://quoteguard-backend.onrender.com/api/clients/${params.id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(client),
      });

      if (!res.ok) throw new Error('Update failed');
      router.push('/dashboard/clients');
    } catch (error) {
      console.error('Error updating client:', error);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setClient(prev => prev ? { ...prev, [name]: value } : null);
  };

  if (loading) return <p>Loading...</p>;
  if (!client) return <p>Client not found</p>;

  return (
    <div className="min-h-screen px-6 pt-28 pb-8 text-black bg-gray-100">
      <h1 className="text-2xl font-bold mb-4 text-blue-800">Edit Client</h1>
      <form onSubmit={handleSubmit} className="space-y-4 bg-white p-6 rounded shadow-md max-w-lg">
        <input
          name="name"
          value={client.name}
          onChange={handleChange}
          placeholder="Name"
          className="w-full border px-4 py-2 rounded"
        />
        <input
          name="email"
          value={client.email}
          onChange={handleChange}
          placeholder="Email"
          className="w-full border px-4 py-2 rounded"
        />
        <input
          name="phone"
          value={client.phone}
          onChange={handleChange}
          placeholder="Phone"
          className="w-full border px-4 py-2 rounded"
        />
        <input
          name="gstin"
          value={client.gstin}
          onChange={handleChange}
          placeholder="GSTIN"
          className="w-full border px-4 py-2 rounded"
        />
        <button
          type="submit"
          className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
        >
          Save Changes
        </button>
      </form>
    </div>
  );
}