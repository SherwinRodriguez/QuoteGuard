'use client';

import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import { Invoice } from '@/types/invoice';
import { invoiceService } from '@/services/invoice';
import { ApiError } from '@/lib/apiError';
import Link from 'next/link';

export default function InvoiceDetailPage() {
  const { id } = useParams();
  const [invoice, setInvoice] = useState<Invoice | null>(null);
  const [loading, setLoading] = useState(true);
  const [showRevokeModal, setShowRevokeModal] = useState(false);
  const [revokeReason, setRevokeReason] = useState('');
  const [revoking, setRevoking] = useState(false);
  const [copied, setCopied] = useState(false);

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

  const handleCopyUUID = () => {
    if (invoice?.uuid) {
      navigator.clipboard.writeText(invoice.uuid);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  const handleRevoke = async () => {
    if (!revokeReason.trim()) {
      alert('Please provide a reason for revocation');
      return;
    }

    if (!invoice?.uuid) {
      alert('Invoice UUID not found');
      return;
    }

    setRevoking(true);

    try {
      // Ownership is enforced server-side from the JWT - no userId needed here.
      await invoiceService.revokeInvoice(invoice.uuid, { reason: revokeReason });

      alert('✅ Invoice revoked successfully');

      // Refresh invoice data
      const updatedInvoice = await invoiceService.getInvoiceById(Number(id));
      setInvoice(updatedInvoice);
      setShowRevokeModal(false);
      setRevokeReason('');
    } catch (err) {
      console.error('Failed to revoke invoice:', err);
      const message = err instanceof ApiError ? err.message : 'Failed to revoke invoice';
      alert(message);
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
      console.error('Failed to download PDF:', err);
      const message = err instanceof ApiError ? err.message : 'PDF not available. It may still be generating.';
      alert(`❌ ${message}`);
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
      <div className="max-w-4xl mx-auto pt-16">
        {/* Header */}
        <div className="bg-white shadow-md rounded-xl p-8 mb-6">
          <div className="flex justify-between items-start mb-6">
            <div>
              <h1 className="text-3xl font-bold text-blue-800 mb-3">
                {invoice.invoiceNumber || `Invoice #${invoice.id}`}
              </h1>
              <div className="flex items-center gap-3">
                {isRevoked ? (
                  <span className="px-3 py-1 text-sm font-semibold rounded-full bg-yellow-100 text-yellow-800">
                    REVOKED
                  </span>
                ) : (
                  <span className="px-3 py-1 text-sm font-semibold rounded-full bg-green-100 text-green-800">
                    ACTIVE
                  </span>
                )}
              </div>
              
              {/* UUID Display with Copy */}
              {invoice.uuid && (
                <div className="mt-3 bg-blue-50 border border-blue-200 rounded-lg p-3">
                  <div className="flex items-center justify-between gap-3">
                    <div className="flex-1">
                      <p className="text-xs font-medium text-gray-600 mb-1">Verification UUID</p>
                      <p className="text-sm font-mono text-gray-900 break-all">{invoice.uuid}</p>
                    </div>
                    <button
                      onClick={handleCopyUUID}
                      className="px-3 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 text-xs font-medium whitespace-nowrap"
                    >
                      {copied ? '✓ Copied!' : '📋 Copy'}
                    </button>
                  </div>
                  <p className="text-xs text-gray-600 mt-2">
                    Use this UUID on the <Link href="/verify" className="text-blue-600 hover:underline">verification page</Link>
                  </p>
                </div>
              )}
            </div>
            <div className="flex gap-2">
              <button
                onClick={handleDownloadPDF}
                className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 text-sm font-medium"
              >
                📄 Download PDF
              </button>
              {invoice.uuid && (
                <Link
                  href={`/verify?uuid=${invoice.uuid}`}
                  target="_blank"
                  className="px-4 py-2 bg-purple-600 text-white rounded-md hover:bg-purple-700 text-sm font-medium"
                >
                  🔍 Verify
                </Link>
              )}
              {!isRevoked && (
                <button
                  onClick={() => setShowRevokeModal(true)}
                  className="px-4 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 text-sm font-medium"
                >
                  ⚠️ Revoke
                </button>
              )}
            </div>
          </div>

          {/* Invoice Details */}
          <div className="grid grid-cols-2 gap-8 text-sm mt-6 pt-6 border-t">
            <div>
              <p className="text-gray-600 font-medium mb-2">Client</p>
              <p className="font-semibold text-gray-900 text-lg">{invoice.client.name}</p>
              <p className="text-gray-700 mt-1">{invoice.client.email}</p>
              {invoice.client.gstin && (
                <p className="text-gray-700 mt-1">GSTIN: {invoice.client.gstin}</p>
              )}
            </div>
            <div className="text-right">
              <p className="text-gray-600 font-medium mb-2">Issue Date</p>
              <p className="font-semibold text-gray-900 text-lg">
                {invoice.issueDate || new Date(invoice.createdAt).toLocaleDateString()}
              </p>
              {invoice.dueDate && (
                <>
                  <p className="text-gray-600 font-medium mt-4 mb-2">Due Date</p>
                  <p className="font-semibold text-gray-900 text-lg">{invoice.dueDate}</p>
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
        <div className="bg-white shadow-md rounded-xl p-8 mb-6">
          <h2 className="text-xl font-bold text-gray-900 mb-6">Invoice Items</h2>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-blue-50">
                <tr>
                  <th className="px-6 py-4 text-left text-sm font-semibold text-gray-900">Product/Service</th>
                  <th className="px-6 py-4 text-right text-sm font-semibold text-gray-900">Quantity</th>
                  <th className="px-6 py-4 text-right text-sm font-semibold text-gray-900">Unit Price</th>
                  <th className="px-6 py-4 text-right text-sm font-semibold text-gray-900">Total</th>
                </tr>
              </thead>
              <tbody>
                {invoice.items?.map((item, idx) => (
                  <tr key={idx} className="border-t border-gray-200">
                    <td className="px-6 py-4 text-gray-900 font-medium">{item.product}</td>
                    <td className="px-6 py-4 text-right text-gray-900">{item.quantity}</td>
                    <td className="px-6 py-4 text-right text-gray-900">
                      {invoice.currency || 'INR'} {item.unitPrice.toFixed(2)}
                    </td>
                    <td className="px-6 py-4 text-right text-gray-900 font-semibold">
                      {invoice.currency || 'INR'} {(item.quantity * item.unitPrice).toFixed(2)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Totals */}
          <div className="mt-8 border-t-2 border-gray-200 pt-6">
            <div className="max-w-sm ml-auto space-y-3">
              {invoice.subtotal !== undefined && (
                <div className="flex justify-between text-gray-900">
                  <span className="font-medium text-base">Subtotal:</span>
                  <span className="font-semibold text-base">{invoice.currency || 'INR'} {invoice.subtotal.toFixed(2)}</span>
                </div>
              )}
              {invoice.tax !== undefined && (
                <div className="flex justify-between text-gray-900">
                  <span className="font-medium text-base">Tax:</span>
                  <span className="font-semibold text-base">{invoice.currency || 'INR'} {invoice.tax.toFixed(2)}</span>
                </div>
              )}
              <div className="flex justify-between text-xl font-bold border-t-2 border-gray-300 pt-3 text-gray-900">
                <span>Total:</span>
                <span className="text-blue-700">{invoice.currency || 'INR'} {invoice.totalAmount.toFixed(2)}</span>
              </div>
            </div>
          </div>
        </div>

        {/* Back Button */}
        <Link
          href="/dashboard/invoices"
          className="text-blue-600 hover:underline font-medium text-base inline-block mb-8"
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