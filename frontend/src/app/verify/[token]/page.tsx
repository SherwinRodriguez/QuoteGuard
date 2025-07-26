'use client';

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";

export default function VerifyInvoicePage() {
  const { token } = useParams();
  const [loading, setLoading] = useState(true);
  const [invoice, setInvoice] = useState<any>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    const fetchInvoice = async () => {
      try {
        const res = await fetch(`https://quoteguard-backend.onrender.com/api/invoices/verify/${token}`);
        if (!res.ok) throw new Error(await res.text());
        const data = await res.json();
        setInvoice(data);
      } catch (err: any) {
        setError(err.message || "Failed to fetch invoice.");
      } finally {
        setLoading(false);
      }
    };

    if (token) fetchInvoice();
  }, [token]);

  if (loading) return <p className="p-10">Loading...</p>;

  if (error) {
    return (
      <div className="p-10 text-red-600 text-xl">
        ❌ {error}
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto p-6 bg-white shadow-lg rounded-xl mt-24">
      <h1 className="text-2xl font-bold mb-4">✅ Invoice Verified</h1>
      <p className="text-gray-600 mb-2">Invoice Number: <strong>{invoice.invoiceNumber}</strong></p>
      <p className="text-gray-600 mb-2">Client: <strong>{invoice.client.name}</strong></p>
      <p className="text-gray-600 mb-2">Issue Date: <strong>{invoice.issueDate}</strong></p>
      <p className="text-gray-600 mb-2">Total Amount: ₹{invoice.totalAmount}</p>
      <p className="text-gray-600 mb-2">Paid: <strong>{invoice.paid ? "Yes" : "No"}</strong></p>

      <h2 className="mt-6 text-lg font-semibold">Items:</h2>
      <ul className="list-disc pl-5 mt-2">
        {invoice.items.map((item: any, idx: number) => (
          <li key={idx}>
            {item.product} - {item.quantity} x ₹{item.unitPrice}
          </li>
        ))}
      </ul>
    </div>
  );
}