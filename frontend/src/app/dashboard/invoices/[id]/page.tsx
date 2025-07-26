'use client';

import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';

interface Client {
  name: string;
  email: string;
}

interface Invoice {
  id: number;
  client: Client;
  totalAmount: number;
  createdAt: string;
  items: {
    product: string;
    quantity: number;
    unitPrice: number;
  }[];
}

export default function InvoiceDetailPage() {
  const { id } = useParams();
  const [invoice, setInvoice] = useState<Invoice | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchInvoice = async () => {
      try {
        const res = await fetch(`https://quoteguard-backend.onrender.com/api/invoices/${id}`);
        const data = await res.json();
        setInvoice(data);
      } catch (err) {
        console.error('Error fetching invoice:', err);
      } finally {
        setLoading(false);
      }
    };

    if (id) fetchInvoice();
  }, [id]);

  if (loading) return <p className="p-4">Loading...</p>;
  if (!invoice) return <p className="p-4 text-red-500">Invoice not found.</p>;

  return (
    <div className="min-h-screen px-6 py-10 bg-gray-100">
      <div className="bg-white shadow-md rounded-xl p-6 max-w-3xl mx-auto space-y-6">
        <div>
          <h1 className="text-2xl font-bold text-blue-800 mb-2">Invoice #{invoice.id}</h1>
          <p><strong>Client:</strong> {invoice.client.name} ({invoice.client.email})</p>
          <p><strong>Date:</strong> {invoice.createdAt}</p>
          <p><strong>Total:</strong> ₹{invoice.totalAmount}</p>
        </div>

        <div>
          <h2 className="font-semibold mb-2">Items</h2>
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left border-b">
                <th className="pb-1">Product</th>
                <th className="pb-1">Quantity</th>
                <th className="pb-1">Price</th>
              </tr>
            </thead>
            <tbody>
              {invoice.items?.map((item, idx) => (
                <tr key={idx} className="border-b">
                  <td className="py-1">{item.product}</td>
                  <td className="py-1">{item.quantity}</td>
                  <td className="py-1">₹{item.unitPrice}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {/* Optional QR download */}
        {/* <a
          href={`/qrcodes/invoice-${invoice.id}.png`}
          className="text-blue-600 underline"
          download
        >
          Download QR Code
        </a> */}
      </div>
    </div>
  );
}