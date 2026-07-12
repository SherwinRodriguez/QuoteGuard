'use client';

import { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { clientService, Client, ClientRequest } from '@/services/client';
import { ApiError } from '@/lib/apiError';

export default function EditClientPage() {
  const params = useParams();
  const router = useRouter();
  const [client, setClient] = useState<Client | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchClient = async () => {
      try {
        const data = await clientService.getClientById(Number(params.id));
        setClient(data);
      } catch (err) {
        console.error('Failed to load client:', err);
        setError(err instanceof ApiError ? err.message : 'Failed to load client');
      } finally {
        setLoading(false);
      }
    };

    if (params?.id) fetchClient();
  }, [params?.id]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!client) return;

    const request: ClientRequest = {
      name: client.name,
      email: client.email,
      phone: client.phone,
      gstin: client.gstin,
    };

    setSaving(true);
    try {
      await clientService.updateClient(Number(params.id), request);
      router.push('/dashboard/clients');
    } catch (err) {
      console.error('Error updating client:', err);
      const message = err instanceof ApiError ? err.message : 'Update failed';
      alert(`❌ ${message}`);
    } finally {
      setSaving(false);
    }
  };

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setClient((prev) => (prev ? { ...prev, [name]: value } : null));
  };

  if (loading) return <p>Loading...</p>;
  if (error) return <p className="p-4 text-red-500">{error}</p>;
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
          required
          className="w-full border px-4 py-2 rounded"
        />
        <input
          name="email"
          value={client.email}
          onChange={handleChange}
          placeholder="Email"
          required
          className="w-full border px-4 py-2 rounded"
        />
        <input
          name="phone"
          value={client.phone ?? ''}
          onChange={handleChange}
          placeholder="Phone"
          className="w-full border px-4 py-2 rounded"
        />
        <input
          name="gstin"
          value={client.gstin ?? ''}
          onChange={handleChange}
          placeholder="GSTIN"
          className="w-full border px-4 py-2 rounded"
        />
        <button
          type="submit"
          disabled={saving}
          className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 disabled:bg-gray-400"
        >
          {saving ? 'Saving...' : 'Save Changes'}
        </button>
      </form>
    </div>
  );
}
