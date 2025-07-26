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
  qrToken: string;
  paid: boolean; // ✅ Make sure this is present
}

export default function InvoicesPage() {
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchInvoices = async () => {
    try {
      const userId = localStorage.getItem('userId');
      const res = await fetch(`https://quoteguard-backend.onrender.com/api/invoices?userId=${userId}`);
      if (!res.ok) throw new Error(await res.text());
      const data = await res.json();
      setInvoices(data);
    } catch (error: any) {
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
      const res = await fetch(`https://quoteguard-backend.onrender.com/api/invoices/${id}`, {
        method: 'DELETE',
      });

      if (res.ok) {
        setInvoices(invoices.filter(invoice => invoice.id !== id));
        alert('✅ Invoice deleted');
      } else {
        alert('❌ Delete failed: ' + (await res.text()));
      }
    } catch (err) {
      console.error('Delete failed', err);
      alert('❌ Something went wrong.');
    }
  };

  const handleMarkAsPaid = async (id: number) => {
    try {
      const res = await fetch(`https://quoteguard-backend.onrender.com/api/invoices/${id}/mark-paid`, {
        method: 'PATCH',
      });

      if (res.ok) {
        alert('✅ Invoice marked as paid');
        fetchInvoices(); // refresh list
      } else {
        const err = await res.text();
        alert('❌ Failed to mark as paid: ' + err);
      }
    } catch (err) {
      console.error('❌ Mark paid error:', err);
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
                <th className="px-4 py-3">QR Token</th>
                <th className="px-4 py-3">Paid</th>
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
                  <td className="px-4 py-2 max-w-[180px]">
                    <div className="flex items-center gap-2">
                      <code className="bg-gray-100 px-2 py-1 rounded font-mono text-xs truncate max-w-[150px]">
                        {invoice.qrToken}
                      </code>
                      <button
                        onClick={() => {
                          navigator.clipboard.writeText(invoice.qrToken);
                          alert("✅ Token copied");
                        }}
                        className="text-xs text-blue-600 hover:underline"
                      >
                        Copy
                      </button>
                    </div>
                  </td>
                  <td className="px-4 py-2">
                    {invoice.paid ? (
                      <span className="text-green-600 font-semibold">Paid</span>
                    ) : (
                      <button
                        onClick={() => handleMarkAsPaid(invoice.id)}
                        className="text-blue-600 underline text-xs hover:text-blue-800"
                      >
                        Mark as Paid
                      </button>
                    )}
                  </td>
                  <td className="px-4 py-2 flex flex-col gap-1 md:flex-row">
                    <a
                      href={`https://quoteguard-backend.onrender.com/api/invoices/pdf/${invoice.id}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="text-blue-600 hover:underline text-xs"
                    >
                      Download PDF
                    </a>
                    <button
                      onClick={() => handleDelete(invoice.id)}
                      className="text-red-600 hover:text-red-800 underline text-xs"
                    >
                      Delete
                    </button>
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