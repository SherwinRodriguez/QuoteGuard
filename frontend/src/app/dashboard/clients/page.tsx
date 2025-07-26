'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';

interface Client {
  id: number;
  name: string;
  email: string;
  phone: string;
  gstin: string;
}

export default function ClientsPage() {
  const [clients, setClients] = useState<Client[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchClients = async () => {
      const userId = localStorage.getItem("userId");

      if (!userId) {
        console.error("User ID not found in localStorage");
        setLoading(false);
        return;
      }

      try {
        const res = await fetch(`https://quoteguard-backend.onrender.com/api/clients?userId=${userId}`);
        if (!res.ok) throw new Error("Failed to fetch clients");
        const data = await res.json();
        setClients(data);
      } catch (err) {
        console.error("❌ Failed to fetch clients:", err);
      } finally {
        setLoading(false);
      }
    };

    fetchClients();
  }, []);

  const handleDelete = async (id: number, name: string) => {
    const confirmDelete = confirm(`Are you sure you want to delete ${name}?`);
    if (!confirmDelete) return;

    try {
      const res = await fetch(`https://quoteguard-backend.onrender.com/api/clients/${id}`, {
        method: 'DELETE',
      });

      if (res.ok) {
        setClients(prev => prev.filter(c => c.id !== id));
      } else {
        console.error('Failed to delete client');
      }
    } catch (err) {
      console.error('Error deleting client', err);
    }
  };

  return (
    <div className="min-h-screen bg-gray-100 text-black px-6 pt-24 pb-12">
      <div className="flex justify-between items-center mb-6">
        <h1 className="text-3xl font-bold text-blue-800">👥 Clients</h1>
        <Link
          href="/dashboard/clients/new"
          className="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 transition"
        >
          + Add Client
        </Link>
      </div>

      {loading ? (
        <p>Loading clients...</p>
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
                  onClick={() => handleDelete(client.id, client.name)}
                  className="text-sm text-red-600 hover:underline"
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