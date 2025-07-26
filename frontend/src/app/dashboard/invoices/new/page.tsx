'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';

interface Client {
  id: number;
  name: string;
}

interface Item {
  product: string;
  quantity: number;
  unitPrice: number;
}

export default function NewInvoicePage() {
  const router = useRouter();

  const [clients, setClients] = useState<Client[]>([]);
  const [clientId, setClientId] = useState('');
  const [issueDate, setIssueDate] = useState('');
  const [paid, setPaid] = useState(false);
  const [items, setItems] = useState<Item[]>([]);
  const [newItem, setNewItem] = useState<Item>({ product: '', quantity: 1, unitPrice: 0 });

  const totalAmount = items.reduce((sum, item) => sum + item.quantity * item.unitPrice, 0);

  useEffect(() => {
  const fetchClients = async () => {
    try {
      const userId = localStorage.getItem('userId');
      if (!userId) return;

      const res = await fetch(`https://quoteguard-backend.onrender.com/api/clients?userId=${userId}`);
      const data = await res.json();
      setClients(data);
    } catch (err) {
      console.error('Failed to fetch clients', err);
    }
  };

  fetchClients();
}, []);

  const handleAddItem = () => {
    if (!newItem.product || newItem.quantity <= 0 || newItem.unitPrice <= 0) return;
    setItems([...items, newItem]);
    setNewItem({ product: '', quantity: 1, unitPrice: 0 });
  };

  const handleSubmit = async (e: React.FormEvent) => {
  e.preventDefault();

  if (!clientId || !issueDate || items.length === 0) {
    alert('Please fill all fields and add at least one item.');
    return;
  }

  const storedUserId = localStorage.getItem('userId');
  if (!storedUserId) {
    alert('❌ userId not found in localStorage');
    return;
  }

  const invoice = {
    clientId: Number(clientId),
    userId: Number(storedUserId), // ✅ using stored userId here
    issueDate,
    paid,
    totalAmount,
    items,
  };

  console.log('📤 Creating invoice with:', invoice); // ✅ debug log

  try {
    const res = await fetch('https://quoteguard-backend.onrender.com/api/invoices', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(invoice),
    });

    if (!res.ok) throw new Error('Invoice creation failed');

    alert('✅ Invoice created successfully');
    router.push('/dashboard/invoices');
  } catch (err) {
    console.error('❌ Error creating invoice:', err);
    alert('Error creating invoice');
  }
};

  return (
    <div className="min-h-screen pt-24 bg-gray-100 text-black px-6 py-8">
      <form onSubmit={handleSubmit} className="max-w-3xl mx-auto bg-white p-6 rounded-xl shadow-md space-y-6">
        <h1 className="text-xl font-bold text-blue-800">Create New Invoice</h1>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {/* Date */}
          <input
            type="date"
            value={issueDate}
            onChange={(e) => setIssueDate(e.target.value)}
            className="border p-2 rounded-md"
            required
          />

          {/* Client */}
          <select
            value={clientId}
            onChange={(e) => setClientId(e.target.value)}
            className="border p-2 rounded-md"
            required
          >
            <option value="">Select Client</option>
            {clients.map((client) => (
              <option key={client.id} value={client.id}>
                {client.name}
              </option>
            ))}
          </select>
        </div>

        {/* Paid Checkbox */}
        <label className="inline-flex items-center gap-2">
          <input
            type="checkbox"
            checked={paid}
            onChange={(e) => setPaid(e.target.checked)}
            className="accent-blue-600"
          />
          Mark as Paid
        </label>

        {/* Add Items */}
        <div>
          <h2 className="font-semibold mb-2">Add Items</h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3 mb-4">
            <input
              type="text"
              placeholder="Product"
              value={newItem.product}
              onChange={(e) => setNewItem({ ...newItem, product: e.target.value })}
              className="border p-2 rounded-md"
            />
            <input
              type="number"
              placeholder="Quantity"
              value={newItem.quantity}
              onChange={(e) => setNewItem({ ...newItem, quantity: Number(e.target.value) })}
              className="border p-2 rounded-md"
            />
            <input
              type="number"
              placeholder="Unit Price"
              value={newItem.unitPrice}
              onChange={(e) => setNewItem({ ...newItem, unitPrice: Number(e.target.value) })}
              className="border p-2 rounded-md"
            />
          </div>

          <button
            type="button"
            onClick={handleAddItem}
            className="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700"
          >
            + Add Item
          </button>
        </div>

        {/* Items List */}
        {items.length > 0 && (
          <ul className="text-sm text-gray-700 space-y-1">
            {items.map((item, idx) => (
              <li key={idx}>
                • {item.product} – {item.quantity} × ₹{item.unitPrice}
              </li>
            ))}
          </ul>
        )}

        {/* Total */}
        <p className="font-semibold text-lg">
          Total: ₹<span className="text-blue-600">{totalAmount}</span>
        </p>

        {/* Submit Button */}
        <button
          type="submit"
          className="bg-blue-600 text-white px-6 py-2 rounded-md hover:bg-blue-700 transition"
        >
          Generate Invoice
        </button>
      </form>
    </div>
  );
}