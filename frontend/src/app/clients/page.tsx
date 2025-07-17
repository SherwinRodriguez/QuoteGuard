'use client';

import { useEffect, useState } from 'react';

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
      try {
        const res = await fetch('http://localhost:8080/api/clients');
        const data = await res.json();
        setClients(data);
      } catch (err) {
        console.error('Error fetching clients', err);
      } finally {
        setLoading(false);
      }
    };

    fetchClients();
  }, []);

  return (
    <div className="min-h-screen px-6 py-8 bg-gray-100">
      <h1 className="text-2xl font-bold text-blue-800 mb-6">Client List</h1>

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
            </div>
          ))}
        </div>
      )}
    </div>
  );
}