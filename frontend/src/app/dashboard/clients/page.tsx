'use client';

import { useEffect, useState } from 'react';
import Link from 'next/link';

<Link
  href="/clients/new"
  className="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 transition"
>
  + New Client
</Link>

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
      const res = await fetch(`http://localhost:8080/api/clients?userId=${userId}`);
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

              <div className="mt-4 flex gap-4">
                {/* Edit Button */}
                <Link
                  href={`/dashboard/clients/${client.id}/edit`}
                  className="text-sm text-blue-600 hover:underline"
                >
                  Edit
                </Link>

                {/* Delete Button */}
                <button
                  className="text-sm text-red-600 hover:underline"
                  onClick={async () => {
                    const confirmDelete = confirm(`Are you sure you want to delete ${client.name}?`);
                    if (!confirmDelete) return;

                    try {
                      const res = await fetch(`http://localhost:8080/api/clients/${client.id}`, {
                        method: 'DELETE',
                      });

                      if (res.ok) {
                        setClients(prev => prev.filter(c => c.id !== client.id));
                      } else {
                        console.error('Failed to delete client');
                      }
                    } catch (err) {
                      console.error('Error deleting client', err);
                    }
                  }}
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