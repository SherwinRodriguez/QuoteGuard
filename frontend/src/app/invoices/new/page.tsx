'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';

interface Client {
  id: number;
  name: string;
}

interface InvoiceItem {
  product: string;
  quantity: number;
  unitPrice: number;
}

export default function NewInvoicePage() {
  const router = useRouter();

  const [clients, setClients] = useState<Client[]>([]);
  const [form, setForm] = useState({
    issueDate: '',
    paid: false,
    totalAmount: 0,
    clientId: 0,
    userId: 1, // Hardcoded for now
    items: [] as InvoiceItem[]
  });

  const [item, setItem] = useState<InvoiceItem>({ product: '', quantity: 1, unitPrice: 0 });

  useEffect(() => {
    const fetchClients = async () => {
      try {
        const res = await fetch('http://localhost:8080/api/clients');
        const data = await res.json();
        setClients(data);
      } catch (err) {
        console.error('Error fetching clients:', err);
      }
    };
    fetchClients();
  }, []);

  const handleFormChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    const { name, value, type, checked } = e.target;
    setForm({
      ...form,
      [name]: type === 'checkbox' ? checked : value
    });
  };

  const handleItemChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setItem({
      ...item,
      [name]: name === 'product' ? value : Number(value)
    });
  };

  const addItem = () => {
    if (item.product && item.quantity && item.unitPrice) {
      const updatedItems = [...form.items, item];
      const updatedTotal = updatedItems.reduce(
        (sum, i) => sum + i.quantity * i.unitPrice,
        0
      );
      setForm({ ...form, items: updatedItems, totalAmount: updatedTotal });
      setItem({ product: '', quantity: 1, unitPrice: 0 });
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const res = await fetch('http://localhost:8080/api/invoices', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(form)
      });

      if (!res.ok) throw new Error('Invoice creation failed');
      alert('✅ Invoice created');
      router.push('/invoices');
    } catch (err) {
      alert('❌ Error creating invoice');
      console.error(err);
    }
  };

  return (
    <div className="min-h-screen px-6 py-10 bg-gray-100 text-black">
      <h1 className="text-2xl font-bold text-blue-800 mb-6">Create New Invoice</h1>

      <form onSubmit={handleSubmit} className="space-y-6 max-w-3xl bg-white p-6 rounded-xl shadow-md">
        {/* Invoice Fields */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <input
            type="date"
            name="issueDate"
            value={form.issueDate}
            onChange={handleFormChange}
            required
            className="border px-3 py-2 rounded"
          />

          <select
            name="clientId"
            value={form.clientId}
            onChange={handleFormChange}
            required
            className="border px-3 py-2 rounded"
          >
            <option value="">Select Client</option>
            {clients.map(client => (
              <option key={client.id} value={client.id}>{client.name}</option>
            ))}
          </select>

          <label className="flex items-center gap-2">
            <input
              type="checkbox"
              name="paid"
              checked={form.paid}
              onChange={handleFormChange}
            />
            Mark as Paid
          </label>
        </div>

        {/* Items List */}
        <div className="space-y-4">
          <h2 className="text-lg font-semibold">Add Items</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <input
              type="text"
              name="product"
              value={item.product}
              onChange={handleItemChange}
              placeholder="Product Name"
              className="border px-3 py-2 rounded"
              required
            />
            <input
              type="number"
              name="quantity"
              value={item.quantity}
              onChange={handleItemChange}
              placeholder="Qty"
              className="border px-3 py-2 rounded"
              required
            />
            <input
              type="number"
              name="unitPrice"
              value={item.unitPrice}
              onChange={handleItemChange}
              placeholder="Unit Price"
              className="border px-3 py-2 rounded"
              required
            />
          </div>
          <button
            type="button"
            onClick={addItem}
            className="mt-2 px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700"
          >
            + Add Item
          </button>

          {form.items.length > 0 && (
            <ul className="mt-4 list-disc pl-5 text-sm text-gray-800">
              {form.items.map((it, idx) => (
                <li key={idx}>
                  {it.product} - {it.quantity} x ₹{it.unitPrice}
                </li>
              ))}
            </ul>
          )}
        </div>

        <div>
          <strong>Total:</strong> ₹{form.totalAmount}
        </div>

        <button
          type="submit"
          className="bg-blue-600 text-white py-2 px-6 rounded hover:bg-blue-700 transition"
        >
          Generate Invoice
        </button>
      </form>
    </div>
  );
}