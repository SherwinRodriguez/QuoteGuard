'use client';

import React from 'react';
import { useEffect, useState } from 'react';
import Link from 'next/link';

interface Item {
  product: string;
  quantity: number;
  unitPrice: number;
}

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
  items: Item[];
}

export default function InvoicesPage() {
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchInvoices = async () => {
      try {
        const res = await fetch('http://localhost:8080/api/invoices');
        const data = await res.json();
        setInvoices(data);
      } catch (err) {
        console.error('❌ Failed to fetch invoices:', err);
      } finally {
        setLoading(false);
      }
    };

    fetchInvoices();
  }, []);

  return (
    <div className="min-h-screen px-6 py-10 text-black bg-gray-100">
      <div className="flex justify-between pt-10 items-center mb-6">
        <h1 className="text-2xl font-bold text-blue-800">Invoices</h1>

        {/* ✅ New Invoice Button */}
        <Link
          href="/dashboard/invoices/new"
          className="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 transition"
        >
          + New Invoice
        </Link>
      </div>

      {loading ? (
        <p className="text-gray-500">Loading invoices...</p>
      ) : invoices.length === 0 ? (
        <p className="text-gray-600">No invoices found.</p>
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
                  <th className="px-4 py-3">Actions</th>
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
                    <td className="px-4 py-2">
                      <a
                        href={`http://localhost:8080/api/invoices/pdf/${invoice.id}`}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="text-sm text-white bg-green-600 px-3 py-1 rounded hover:bg-green-700"
                      >
                        Download
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