'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { Invoice } from '@/types/invoice';
import { invoiceService } from '@/services/invoice';

export default function InvoicesPage() {
  const [invoices, setInvoices] = useState<Invoice[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchInvoices = async () => {
    try {
      const userId = localStorage.getItem('userId');
      if (!userId) {
        setError('Please log in to view invoices');
        setLoading(false);
        return;
      }

      const data = await invoiceService.getInvoicesByUser(Number(userId));
      setInvoices(data);
    } catch (error: any) {
      console.error('❌ Failed to fetch invoices:', error);
      setError(error.message || 'Failed to load invoices');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchInvoices();
  }, []);

  const getStatusBadge = (status?: string) => {
    if (status === 'REVOKED') {
      return (
        <span className="px-2 py-1 text-xs font-semibold rounded-full bg-yellow-100 text-yellow-800">
          REVOKED
        </span>
      );
    }
    return (
      <span className="px-2 py-1 text-xs font-semibold rounded-full bg-green-100 text-green-800">
        ACTIVE
      </span>
    );
  };

  const handleDownloadPDF = async (invoiceId: number) => {
    try {
      const blob = await invoiceService.downloadPDF(invoiceId);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `invoice-${invoiceId}.pdf`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (err) {
      console.error('Failed to download PDF:', err);
      alert('❌ PDF not available yet. Please try again in a moment.');
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
        <div className="flex justify-center items-center h-64">
          <p className="text-gray-500">Loading invoices...</p>
        </div>
      ) : error ? (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <p className="text-red-600">❌ {error}</p>
        </div>
      ) : invoices.length === 0 ? (
        <div className="bg-white rounded-lg shadow-md p-12 text-center">
          <p className="text-gray-500 mb-4">No invoices found.</p>
          <Link
            href="/dashboard/invoices/new"
            className="text-blue-600 hover:underline"
          >
            Create your first invoice
          </Link>
        </div>
      ) : (
        <div className="overflow-x-auto bg-white rounded-xl shadow-md">
          <table className="min-w-full text-sm text-left">
            <thead className="bg-blue-100 text-blue-800">
              <tr>
                <th className="px-4 py-3">Invoice #</th>
                <th className="px-4 py-3">Client</th>
                <th className="px-4 py-3">Date</th>
                <th className="px-4 py-3">Due Date</th>
                <th className="px-4 py-3">Amount</th>
                <th className="px-4 py-3">Status</th>
                <th className="px-4 py-3 text-center">Actions</th>
              </tr>
            </thead>
            <tbody>
              {invoices.map((invoice) => (
                <tr key={invoice.id} className="border-b hover:bg-gray-50">
                  <td className="px-4 py-3 font-mono text-xs">
                    {invoice.invoiceNumber || `#${invoice.id}`}
                  </td>
                  <td className="px-4 py-3">
                    <div>
                      <p className="font-medium">{invoice.client.name}</p>
                      <p className="text-xs text-gray-500">{invoice.client.email}</p>
                    </div>
                  </td>
                  <td className="px-4 py-3">
                    {invoice.issueDate || new Date(invoice.createdAt).toLocaleDateString()}
                  </td>
                  <td className="px-4 py-3">
                    {invoice.dueDate || 'N/A'}
                  </td>
                  <td className="px-4 py-3 font-semibold">
                    {invoice.currency || 'INR'} {invoice.totalAmount.toFixed(2)}
                  </td>
                  <td className="px-4 py-3">
                    {getStatusBadge(invoice.status)}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex gap-2 justify-center">
                      <Link
                        href={`/dashboard/invoices/${invoice.id}`}
                        className="text-blue-600 hover:underline text-xs"
                      >
                        View
                      </Link>
                      <button
                        onClick={() => handleDownloadPDF(invoice.id)}
                        className="text-green-600 hover:underline text-xs"
                      >
                        PDF
                      </button>
                      {invoice.uuid && (
                        <Link
                          href={`/verify?uuid=${invoice.uuid}`}
                          target="_blank"
                          className="text-purple-600 hover:underline text-xs"
                        >
                          Verify
                        </Link>
                      )}
                    </div>
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