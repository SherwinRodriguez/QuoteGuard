'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';

interface Client {
  id: number;
  name: string;
  email: string;
  gstin: string;
  phone: string;
}

interface Invoice {
  id: number;
  client: Client;
  totalAmount: number;
  createdAt: string;
}

export default function InvoicesPage() {
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // ✅ Fetch invoices for the logged-in user
  const fetchInvoices = async () => {
    try {
      // 🔥 HARDCODED USER ID (replace with localStorage.getItem later)
      const userId = localStorage.getItem('userId');
      console.log("👤 Using hardcoded userId:", userId);

      const res = await fetch(`http://localhost:8080/api/invoices?userId=${userId}`);
      if (!res.ok) {
        const errText = await res.text();
        throw new Error(`HTTP ${res.status}: ${errText}`);
      }

      const data = await res.json();
      console.log("📦 Invoices received from backend:", data);
      setInvoices(data);
    } catch (error: any) {
      console.error("❌ Failed to fetch invoices:", error);
      setError(error.message || "Unknown error");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchInvoices();
  }, []);

  const handleDelete = async (id: number) => {
    const confirmed = window.confirm('Are you sure you want to delete this invoice?');
    if (!confirmed) return;

    try {
      const res = await fetch(`http://localhost:8080/api/invoices/${id}`, {
        method: 'DELETE',
      });

      if (res.ok) {
        setInvoices(invoices.filter(invoice => invoice.id !== id));
        alert('✅ Invoice deleted');
      } else {
        const error = await res.text();
        alert('❌ Delete failed: ' + error);
      }
    } catch (err) {
      console.error('Delete failed', err);
      alert('❌ Something went wrong.');
    }
  };

  return (
    <div className="min-h-screen px-6 py-10 text-black bg-gray-100">
      <div className="flex justify-between pt-10 items-center mb-6">
        <h1 className="text-2xl font-bold text-blue-800">Invoices</h1>
        <Link
          href="/dashboard/invoices/new"
          className="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 transition"
        >
          + New Invoice
        </Link>
      </div>

      {loading ? (
        <p>Loading invoices...</p>
      ) : error ? (
        <p className="text-red-600">❌ {error}</p>
      ) : invoices.length === 0 ? (
        <p>No invoices found.</p>
      ) : (
        <div className="overflow-x-auto bg-white rounded-xl shadow-md">
          <table className="min-w-full text-sm text-left">
            <thead className="bg-blue-100 text-blue-800">
              <tr>
                <th className="px-4 py-3">#</th>
                <th className="px-4 py-3">Client</th>
                <th className="px-4 py-3">Email</th>
                <th className="px-4 py-3">Total</th>
                <th className="px-4 py-3">Created At</th>
                <th className="px-4 py-3">Action</th>
              </tr>
            </thead>
            <tbody>
              {invoices.map((invoice, index) => (
                <tr key={invoice.id} className="border-t hover:bg-gray-50">
                  <td className="px-4 py-2">{index + 1}</td>
                  <td className="px-4 py-2">{invoice.client.name}</td>
                  <td className="px-4 py-2">{invoice.client.email}</td>
                  <td className="px-4 py-2">₹{invoice.totalAmount}</td>
                  <td className="px-4 py-2">{invoice.createdAt}</td>
                  <td className="px-4 py-2 flex gap-2">
                    <button
                      onClick={() => handleDelete(invoice.id)}
                      className="text-red-600 hover:text-red-800 underline"
                    >
                      Delete
                    </button>
                    <a
                      href={`http://localhost:8080/api/invoices/pdf/${invoice.id}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-blue-600 hover:underline"
                    >
                      Download PDF
                    </a>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}