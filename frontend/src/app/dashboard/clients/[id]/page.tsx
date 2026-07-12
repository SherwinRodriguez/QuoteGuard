'use client';

import { useEffect, useState } from 'react';
import { useParams, useSearchParams, useRouter } from 'next/navigation';
import { clientService, Client } from '@/services/client';
import { ApiError } from '@/lib/apiError';

export default function ClientDetailsPage() {
  const { id } = useParams();
  const router = useRouter();
  const searchParams = useSearchParams();
  const isEditMode = searchParams.get('edit') === 'true';

  const [client, setClient] = useState<Client | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    phone: '',
    gstin: '',
  });

  useEffect(() => {
    const fetchClient = async () => {
      try {
        const data = await clientService.getClientById(Number(id));
        setClient(data);
        setFormData({
          name: data.name,
          email: data.email,
          phone: data.phone ?? '',
          gstin: data.gstin ?? '',
        });
      } catch (err) {
        console.error(err);
        setError(err instanceof ApiError ? err.message : 'Failed to load client');
      } finally {
        setLoading(false);
      }
    };
    if (id) fetchClient();
  }, [id]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      const updated = await clientService.updateClient(Number(id), formData);
      setClient(updated);
      alert('✅ Client updated');
      router.push(`/dashboard/clients/${id}`);
    } catch (err) {
      console.error(err);
      const message = err instanceof ApiError ? err.message : 'Update failed';
      alert(`❌ ${message}`);
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <p className="p-4">Loading client...</p>;
  if (error) return <p className="p-4 text-red-500">{error}</p>;
  if (!client) return <p className="p-4 text-red-500">Client not found.</p>;

  return (
    <div className="min-h-screen pt-28 px-6">
      <h1 className="text-2xl font-bold text-blue-700 mb-4">
        {isEditMode ? 'Edit Client' : 'Client Details'}
      </h1>

      {isEditMode ? (
        <form onSubmit={handleUpdate} className="space-y-4 max-w-xl bg-white p-6 rounded-xl shadow-md">
          {['name', 'email', 'phone', 'gstin'].map((field) => (
            <div key={field}>
              <label className="block text-sm font-medium mb-1 capitalize">{field}</label>
              <input
                type="text"
                name={field}
                value={formData[field as keyof typeof formData]}
                onChange={handleChange}
                className="w-full border px-3 py-2 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                required={field === 'name' || field === 'email'}
              />
            </div>
          ))}
          <button
            type="submit"
            disabled={saving}
            className="bg-blue-600 text-white py-2 px-4 rounded-md hover:bg-blue-700 disabled:bg-gray-400"
          >
            {saving ? 'Saving...' : 'Save Changes'}
          </button>
        </form>
      ) : (
        <div className="bg-white shadow-md p-6 rounded-xl max-w-xl space-y-2">
          <p><strong>Name:</strong> {client.name}</p>
          <p><strong>Email:</strong> {client.email}</p>
          <p><strong>Phone:</strong> {client.phone}</p>
          <p><strong>GSTIN:</strong> {client.gstin}</p>
          <button
            onClick={() => router.push(`/dashboard/clients/${id}?edit=true`)}
            className="mt-4 text-blue-600 hover:underline"
          >
            ✏️ Edit Client
          </button>
        </div>
      )}
    </div>
  );
}
