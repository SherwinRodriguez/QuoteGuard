'use client';

import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import Image from 'next/image';

interface Client {
  name: string;
  email: string;
  phone: string;
  gstin: string;
}

interface Item {
  description: string;
  quantity: number;
  price: number;
}

interface Invoice {
  id: number;
  client: Client;
  items: Item[];
  totalAmount: number;
  qrCodeUrl?: string; // optional
  pdfUrl?: string;    // optional
}

export default function InvoiceDetailPage() {
  const { id } = useParams();
  const [invoice, setInvoice] = useState<Invoice | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchInvoice = async () => {
      try {
        const res = await fetch(`http://localhost:8080/api/invoices/${id}`);
        const data = await res.json();
        setInvoice(data);
      } catch (err) {
        console.error('Failed to fetch invoice:', err);
      } finally {
        setLoading(false);
      }
    };

    if (id) fetchInvoice();
  }, [id]);

  if (loading) return <p className="p-6">Loading invoice...</p>;
  if (!invoice) return <p className="p-6">Invoice not found.</p>;

  return (
    <div className="min-h-screen p-8 bg-gray-100">
      <h1 className="text-2xl font-bold mb-6 text-blue-800">Invoice #{invoice.id}</h1>

      <div className="bg-white rounded-xl shadow p-6 space-y-6 max-w-2xl">
        <div>
          <h2 className="text-lg font-semibold">Client Info</h2>
          <p><strong>Name:</strong> {invoice.client.name}</p>
          <p><strong>Email:</strong> {invoice.client.email}</p>
          <p><strong>Phone:</strong> {invoice.client.phone}</p>
          <p><strong>GSTIN:</strong> {invoice.client.gstin}</p>
        </div>

        <div>
          <h2 className="text-lg font-semibold">Items</h2>
          <table className="w-full text-sm border">
            <thead>
              <tr className="bg-blue-100">
                <th className="p-2 border">Description</th>
                <th className="p-2 border">Qty</th>
                <th className="p-2 border">Price</th>
                <th className="p-2 border">Total</th>
              </tr>
            </thead>
            <tbody>
              {invoice.items.map((item, idx) => (
                <tr key={idx}>
                  <td className="p-2 border">{item.description}</td>
                  <td className="p-2 border text-center">{item.quantity}</td>
                  <td className="p-2 border text-right">₹{item.price}</td>
                  <td className="p-2 border text-right">₹{item.quantity * item.price}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <div className="text-right font-semibold text-lg">
          Total: ₹{invoice.totalAmount}
        </div>

        {invoice.qrCodeUrl && (
          <div>
            <h2 className="text-lg font-semibold mb-2">QR Code</h2>
            <Image
                src={invoice.qrCodeUrl}
                alt="QR Code"
                width={160}
                height={160}
                className="rounded-md"
            />
          </div>
        )}

        {invoice.pdfUrl && (
          <div className="mt-4">
            <a
              href={invoice.pdfUrl}
              target="_blank"
              rel="noopener noreferrer"
              className="text-blue-600 underline"
            >
              🔽 Download PDF
            </a>
          </div>
        )}
      </div>
    </div>
  );
}