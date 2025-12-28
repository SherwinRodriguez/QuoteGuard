'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { invoiceService } from '@/services/invoice';
import { InvoiceItem, InvoiceRequest } from '@/types/invoice';

interface Client {
  id: number;
  name: string;
}

export default function NewInvoicePage() {
  const router = useRouter();

  const [clients, setClients] = useState<Client[]>([]);
  const [clientId, setClientId] = useState('');
  const [invoiceNumber, setInvoiceNumber] = useState('');
  const [issueDate, setIssueDate] = useState('');
  const [dueDate, setDueDate] = useState('');
  const [currency, setCurrency] = useState('INR');
  const [items, setItems] = useState<InvoiceItem[]>([]);
  const [newItem, setNewItem] = useState<InvoiceItem>({ 
    product: '', 
    quantity: 1, 
    unitPrice: 0 
  });
  const [taxRate, setTaxRate] = useState(18); // Default 18% GST
  const [loading, setLoading] = useState(false);

  // Calculate amounts
  const subtotal = items.reduce((sum, item) => sum + item.quantity * item.unitPrice, 0);
  const tax = (subtotal * taxRate) / 100;
  const totalAmount = subtotal + tax;

  useEffect(() => {
    const fetchClients = async () => {
      try {
        const userId = localStorage.getItem('userId');
        if (!userId) return;

        const res = await fetch(`http://localhost:8080/api/clients?userId=${userId}`);
        const data = await res.json();
        setClients(data);
      } catch (err) {
        console.error('Failed to fetch clients', err);
      }
    };

    fetchClients();

    // Set default dates
    const today = new Date().toISOString().split('T')[0];
    const thirtyDaysLater = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split('T')[0];
    setIssueDate(today);
    setDueDate(thirtyDaysLater);
  }, []);

  const handleAddItem = () => {
    if (!newItem.product || newItem.quantity <= 0 || newItem.unitPrice <= 0) {
      alert('Please fill all item fields correctly');
      return;
    }
    setItems([...items, newItem]);
    setNewItem({ product: '', quantity: 1, unitPrice: 0 });
  };

  const handleRemoveItem = (index: number) => {
    setItems(items.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!clientId || !issueDate || !dueDate || items.length === 0) {
      alert('Please fill all required fields and add at least one item.');
      return;
    }

    const storedUserId = localStorage.getItem('userId');
    if (!storedUserId) {
      alert('❌ Please log in to create an invoice');
      router.push('/login');
      return;
    }

    const request: InvoiceRequest = {
      invoiceNumber: invoiceNumber || undefined, // Auto-generated if empty
      clientId: Number(clientId),
      userId: Number(storedUserId),
      issueDate,
      dueDate,
      currency,
      subtotal: Number(subtotal.toFixed(2)),
      tax: Number(tax.toFixed(2)),
      totalAmount: Number(totalAmount.toFixed(2)),
      items,
    };

    setLoading(true);

    try {
      await invoiceService.createInvoice(request);
      alert('✅ Invoice created successfully');
      router.push('/dashboard/invoices');
    } catch (err: any) {
      console.error('❌ Error creating invoice:', err);
      alert(err.message || 'Error creating invoice');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen pt-24 bg-gray-100 text-black px-6 py-8">
      <form onSubmit={handleSubmit} className="max-w-4xl mx-auto bg-white p-6 rounded-xl shadow-md space-y-6">
        <h1 className="text-2xl font-bold text-blue-800">Create New Invoice</h1>

        {/* Basic Information */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium mb-1">Client *</label>
            <select
              value={clientId}
              onChange={(e) => setClientId(e.target.value)}
              required
              className="w-full border px-3 py-2 rounded-md focus:ring-2 focus:ring-blue-500"
            >
              <option value="">Select Client</option>
              {clients.map((client) => (
                <option key={client.id} value={client.id}>
                  {client.name}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium mb-1">Invoice Number (Optional)</label>
            <input
              type="text"
              value={invoiceNumber}
              onChange={(e) => setInvoiceNumber(e.target.value)}
              placeholder="Auto-generated if empty"
              className="w-full border px-3 py-2 rounded-md focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium mb-1">Issue Date *</label>
            <input
              type="date"
              value={issueDate}
              onChange={(e) => setIssueDate(e.target.value)}
              required
              className="w-full border px-3 py-2 rounded-md focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium mb-1">Due Date *</label>
            <input
              type="date"
              value={dueDate}
              onChange={(e) => setDueDate(e.target.value)}
              required
              className="w-full border px-3 py-2 rounded-md focus:ring-2 focus:ring-blue-500"
            />
          </div>

          <div>
            <label className="block text-sm font-medium mb-1">Currency *</label>
            <select
              value={currency}
              onChange={(e) => setCurrency(e.target.value)}
              className="w-full border px-3 py-2 rounded-md focus:ring-2 focus:ring-blue-500"
            >
              <option value="INR">INR (₹)</option>
              <option value="USD">USD ($)</option>
              <option value="EUR">EUR (€)</option>
              <option value="GBP">GBP (£)</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium mb-1">Tax Rate (%)</label>
            <input
              type="number"
              value={taxRate}
              onChange={(e) => setTaxRate(Number(e.target.value))}
              min="0"
              max="100"
              step="0.01"
              className="w-full border px-3 py-2 rounded-md focus:ring-2 focus:ring-blue-500"
            />
          </div>
        </div>

        {/* Line Items */}
        <div className="border-t pt-6">
          <h2 className="text-lg font-semibold mb-4">Invoice Items</h2>
          
          {/* Add Item Form */}
          <div className="grid grid-cols-1 md:grid-cols-4 gap-3 mb-4">
            <input
              type="text"
              placeholder="Product/Service"
              value={newItem.product}
              onChange={(e) => setNewItem({ ...newItem, product: e.target.value })}
              className="border px-3 py-2 rounded-md"
            />
            <input
              type="number"
              placeholder="Quantity"
              value={newItem.quantity}
              onChange={(e) => setNewItem({ ...newItem, quantity: Number(e.target.value) })}
              min="1"
              className="border px-3 py-2 rounded-md"
            />
            <input
              type="number"
              placeholder="Unit Price"
              value={newItem.unitPrice}
              onChange={(e) => setNewItem({ ...newItem, unitPrice: Number(e.target.value) })}
              min="0"
              step="0.01"
              className="border px-3 py-2 rounded-md"
            />
            <button
              type="button"
              onClick={handleAddItem}
              className="bg-green-600 text-white px-4 py-2 rounded-md hover:bg-green-700"
            >
              + Add Item
            </button>
          </div>

          {/* Items List */}
          {items.length > 0 && (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead className="bg-gray-100">
                  <tr>
                    <th className="px-4 py-2 text-left">Product</th>
                    <th className="px-4 py-2 text-right">Quantity</th>
                    <th className="px-4 py-2 text-right">Unit Price</th>
                    <th className="px-4 py-2 text-right">Total</th>
                    <th className="px-4 py-2"></th>
                  </tr>
                </thead>
                <tbody>
                  {items.map((item, idx) => (
                    <tr key={idx} className="border-t">
                      <td className="px-4 py-2">{item.product}</td>
                      <td className="px-4 py-2 text-right">{item.quantity}</td>
                      <td className="px-4 py-2 text-right">{currency} {item.unitPrice.toFixed(2)}</td>
                      <td className="px-4 py-2 text-right">
                        {currency} {(item.quantity * item.unitPrice).toFixed(2)}
                      </td>
                      <td className="px-4 py-2 text-center">
                        <button
                          type="button"
                          onClick={() => handleRemoveItem(idx)}
                          className="text-red-600 hover:text-red-800"
                        >
                          Remove
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Totals */}
        <div className="border-t pt-6">
          <div className="max-w-xs ml-auto space-y-2">
            <div className="flex justify-between">
              <span className="font-medium">Subtotal:</span>
              <span>{currency} {subtotal.toFixed(2)}</span>
            </div>
            <div className="flex justify-between">
              <span className="font-medium">Tax ({taxRate}%):</span>
              <span>{currency} {tax.toFixed(2)}</span>
            </div>
            <div className="flex justify-between text-lg font-bold border-t pt-2">
              <span>Total:</span>
              <span>{currency} {totalAmount.toFixed(2)}</span>
            </div>
          </div>
        </div>

        {/* Actions */}
        <div className="flex gap-4 justify-end border-t pt-6">
          <button
            type="button"
            onClick={() => router.back()}
            className="px-6 py-2 border border-gray-300 rounded-md hover:bg-gray-100"
            disabled={loading}
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={loading || items.length === 0}
            className="px-6 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed"
          >
            {loading ? 'Creating...' : 'Create Invoice'}
          </button>
        </div>
      </form>
    </div>
  );
}
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