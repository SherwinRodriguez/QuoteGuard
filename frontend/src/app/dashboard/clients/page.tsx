'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';
import { clientService, Client } from '@/services/client';

export default function ClientsPage() {
  const [clients, setClients] = useState<Client[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchClients = async () => {
      try {
        const data = await clientService.getClients();
        setClients(data);
      } catch (err) {
        console.error('❌ Failed to fetch clients:', err);
        setError('Failed to load clients');
      } finally {
        setLoading(false);
      }
    };

    fetchClients();
  }, []);

  const handleDelete = async (client: Client) => {
    const confirmDelete = confirm(`Are you sure you want to delete ${client.name}?`);
    if (!confirmDelete) return;

    try {
      await clientService.deleteClient(client.id);
      setClients((prev) => prev.filter((c) => c.id !== client.id));
    } catch (err) {
      console.error('Failed to delete client', err);
      alert('❌ Failed to delete client');
    }
  };

  return (
    <div className="min-h-screen px-6 pt-28 pb-8 text-black bg-gray-100">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-2xl font-bold text-blue-800">Clients</h1>

        <Link
          href="/dashboard/clients/new"
          className="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 transition"
        >
          + New Client
        </Link>
      </div>

      {loading ? (
        <p>Loading clients...</p>
      ) : error ? (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <p className="text-red-600">❌ {error}</p>
        </div>
      ) : clients.length === 0 ? (
        <p>No clients found.</p>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {clients.map((client) => (
            <div key={client.id} className="bg-white p-4 rounded-xl shadow-md">
              <h2 className="text-xl font-semibold text-gray-800">{client.name}</h2>
              <p className="text-sm text-gray-600">{client.email}</p>
              <p className="text-sm text-gray-600">{client.phone}</p>
              <p className="text-sm text-gray-600">{client.gstin}</p>

              <div className="mt-4 flex gap-4">
                <Link
                  href={`/dashboard/clients/${client.id}/edit`}
                  className="text-sm text-blue-600 hover:underline"
                >
                  Edit
                </Link>

                <button
                  className="text-sm text-red-600 hover:underline"
                  onClick={() => handleDelete(client)}
                >
                  Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
