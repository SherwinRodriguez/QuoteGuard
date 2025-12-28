'use client';

import { useEffect, useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { Invoice } from '@/types/invoice';
import { invoiceService } from '@/services/invoice';
import Link from 'next/link';

export default function InvoiceDetailPage() {
  const { id } = useParams();
  const router = useRouter();
  const [invoice, setInvoice] = useState<Invoice | null>(null);
  const [loading, setLoading] = useState(true);
  const [showRevokeModal, setShowRevokeModal] = useState(false);
  const [revokeReason, setRevokeReason] = useState('');
  const [revoking, setRevoking] = useState(false);

  useEffect(() => {
    const fetchInvoice = async () => {
      try {
        const data = await invoiceService.getInvoiceById(Number(id));
        setInvoice(data);
      } catch (err) {
        console.error('Error fetching invoice:', err);
      } finally {
        setLoading(false);
      }
    };

    if (id) fetchInvoice();
  }, [id]);

  const handleRevoke = async () => {
    if (!revokeReason.trim()) {
      alert('Please provide a reason for revocation');
      return;
    }

    if (!invoice?.uuid) {
      alert('Invoice UUID not found');
      return;
    }

    const userId = localStorage.getItem('userId');
    if (!userId) {
      alert('Please log in to revoke invoices');
      return;
    }

    setRevoking(true);

    try {
      await invoiceService.revokeInvoice(invoice.uuid, Number(userId), {
        reason: revokeReason,
      });

      alert('✅ Invoice revoked successfully');
      
      // Refresh invoice data
      const updatedInvoice = await invoiceService.getInvoiceById(Number(id));
      setInvoice(updatedInvoice);
      setShowRevokeModal(false);
      setRevokeReason('');
    } catch (err: any) {
      console.error('Failed to revoke invoice:', err);
      alert(err.message || 'Failed to revoke invoice');
    } finally {
      setRevoking(false);
    }
  };

  const handleDownloadPDF = async () => {
    if (!invoice) return;
    
    try {
      const blob = await invoiceService.downloadPDF(invoice.id);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `invoice-${invoice.invoiceNumber || invoice.id}.pdf`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
    } catch (err) {
      alert('❌ PDF not available. It may still be generating.');
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <p className="text-gray-500">Loading invoice...</p>
      </div>
    );
  }

  if (!invoice) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <p className="text-red-500 mb-4">Invoice not found</p>
          <Link href="/dashboard/invoices" className="text-blue-600 hover:underline">
            Back to Invoices
          </Link>
        </div>
      </div>
    );
  }

  const isRevoked = invoice.status === 'REVOKED';

  return (
    <div className="min-h-screen px-6 py-10 bg-gray-100">
      <div className="max-w-4xl mx-auto">
        {/* Header */}
        <div className="bg-white shadow-md rounded-xl p-6 mb-6">
          <div className="flex justify-between items-start mb-4">
            <div>
              <h1 className="text-3xl font-bold text-blue-800 mb-2">
                {invoice.invoiceNumber || `Invoice #${invoice.id}`}
              </h1>
              <div className="flex items-center gap-2">
                {isRevoked ? (
                  <span className="px-3 py-1 text-sm font-semibold rounded-full bg-yellow-100 text-yellow-800">
                    REVOKED
                  </span>
                ) : (
                  <span className="px-3 py-1 text-sm font-semibold rounded-full bg-green-100 text-green-800">
                    ACTIVE
                  </span>
                )}
                {invoice.uuid && (
                  <span className="text-xs text-gray-500 font-mono">
                    UUID: {invoice.uuid.substring(0, 8)}...
                  </span>
                )}
              </div>
            </div>
            <div className="flex gap-2">
              <button
                onClick={handleDownloadPDF}
                className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 text-sm"
              >
                📄 Download PDF
              </button>
              {invoice.uuid && (
                <Link
                  href={`/verify?uuid=${invoice.uuid}`}
                  target="_blank"
                  className="px-4 py-2 bg-purple-600 text-white rounded-md hover:bg-purple-700 text-sm"
                >
                  🔍 Verify
                </Link>
              )}
              {!isRevoked && (
                <button
                  onClick={() => setShowRevokeModal(true)}
                  className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 text-sm"
                >
                  ⚠️ Revoke
                </button>
              )}
            </div>
          </div>

          {/* Invoice Details */}
          <div className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <p className="text-gray-500">Client</p>
              <p className="font-semibold">{invoice.client.name}</p>
              <p className="text-gray-600">{invoice.client.email}</p>
              {invoice.client.gstin && (
                <p className="text-gray-600">GSTIN: {invoice.client.gstin}</p>
              )}
            </div>
            <div className="text-right">
              <p className="text-gray-500">Issue Date</p>
              <p className="font-semibold">
                {invoice.issueDate || new Date(invoice.createdAt).toLocaleDateString()}
              </p>
              {invoice.dueDate && (
                <>
                  <p className="text-gray-500 mt-2">Due Date</p>
                  <p className="font-semibold">{invoice.dueDate}</p>
                </>
              )}
            </div>
          </div>

          {/* Revocation Notice */}
          {isRevoked && invoice.revokedReason && (
            <div className="mt-4 p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
              <p className="font-semibold text-yellow-900 mb-1">Revocation Notice</p>
              <p className="text-sm text-yellow-800">{invoice.revokedReason}</p>
              {invoice.revokedAt && (
                <p className="text-xs text-yellow-700 mt-2">
                  Revoked on: {new Date(invoice.revokedAt).toLocaleString()}
                </p>
              )}
            </div>
          )}
        </div>

        {/* Items */}
        <div className="bg-white shadow-md rounded-xl p-6 mb-6">
          <h2 className="text-lg font-semibold mb-4">Invoice Items</h2>
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead className="bg-gray-100">
                <tr>
                  <th className="px-4 py-2 text-left">Product/Service</th>
                  <th className="px-4 py-2 text-right">Quantity</th>
                  <th className="px-4 py-2 text-right">Unit Price</th>
                  <th className="px-4 py-2 text-right">Total</th>
                </tr>
              </thead>
              <tbody>
                {invoice.items?.map((item, idx) => (
                  <tr key={idx} className="border-t">
                    <td className="px-4 py-3">{item.product}</td>
                    <td className="px-4 py-3 text-right">{item.quantity}</td>
                    <td className="px-4 py-3 text-right">
                      {invoice.currency || 'INR'} {item.unitPrice.toFixed(2)}
                    </td>
                    <td className="px-4 py-3 text-right">
                      {invoice.currency || 'INR'} {(item.quantity * item.unitPrice).toFixed(2)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Totals */}
          <div className="mt-6 border-t pt-4">
            <div className="max-w-xs ml-auto space-y-2">
              {invoice.subtotal !== undefined && (
                <div className="flex justify-between">
                  <span className="font-medium">Subtotal:</span>
                  <span>{invoice.currency || 'INR'} {invoice.subtotal.toFixed(2)}</span>
                </div>
              )}
              {invoice.tax !== undefined && (
                <div className="flex justify-between">
                  <span className="font-medium">Tax:</span>
                  <span>{invoice.currency || 'INR'} {invoice.tax.toFixed(2)}</span>
                </div>
              )}
              <div className="flex justify-between text-lg font-bold border-t pt-2">
                <span>Total:</span>
                <span>{invoice.currency || 'INR'} {invoice.totalAmount.toFixed(2)}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Back Button */}
        <Link
          href="/dashboard/invoices"
          className="text-blue-600 hover:underline"
        >
          ← Back to Invoices
        </Link>
      </div>

      {/* Revoke Modal */}
      {showRevokeModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 max-w-md w-full mx-4">
            <h3 className="text-xl font-bold mb-4">Revoke Invoice</h3>
            <p className="text-sm text-gray-600 mb-4">
              Are you sure you want to revoke this invoice? This action cannot be undone.
              Revoked invoices will fail verification checks.
            </p>
            <div className="mb-4">
              <label className="block text-sm font-medium mb-2">
                Reason for revocation *
              </label>
              <textarea
                value={revokeReason}
                onChange={(e) => setRevokeReason(e.target.value)}
                placeholder="e.g., Incorrect amount, duplicate invoice, etc."
                rows={3}
                className="w-full border border-gray-300 rounded-md px-3 py-2 focus:ring-2 focus:ring-red-500"
                required
              />
            </div>
            <div className="flex gap-3 justify-end">
              <button
                onClick={() => {
                  setShowRevokeModal(false);
                  setRevokeReason('');
                }}
                disabled={revoking}
                className="px-4 py-2 border border-gray-300 rounded-md hover:bg-gray-100"
              >
                Cancel
              </button>
              <button
                onClick={handleRevoke}
                disabled={revoking || !revokeReason.trim()}
                className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 disabled:bg-gray-400"
              >
                {revoking ? 'Revoking...' : 'Revoke Invoice'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}