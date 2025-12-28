'use client';

import { useState, useEffect } from 'react';
import { useSearchParams } from 'next/navigation';

/**
 * Public Invoice Verification Page
 * 
 * This page is accessed by ANYONE who scans the QR code.
 * NO AUTHENTICATION required.
 * 
 * URL: /verify/{uuid} or /verify?uuid={uuid}
 */

interface VerificationResponse {
  status: 'VERIFIED' | 'REVOKED' | 'MODIFIED' | 'NOT_FOUND';
  message: string;
  freelancerName?: string;
  invoiceNumber?: string;
  issueDate?: string;
  dueDate?: string;
  currency?: string;
  totalAmount?: number;
  revokedAt?: string;
  revokedReason?: string;
  verificationTimestamp: string;
}

export default function VerifyInvoicePage() {
  const searchParams = useSearchParams();
  const uuidFromUrl = searchParams.get('uuid');
  
  const [uuid, setUuid] = useState(uuidFromUrl || '');
  const [result, setResult] = useState<VerificationResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Auto-verify if UUID in URL
  useEffect(() => {
    if (uuidFromUrl) {
      handleVerify(uuidFromUrl);
    }
  }, [uuidFromUrl]);

  const handleVerify = async (uuidToVerify?: string) => {
    const targetUuid = uuidToVerify || uuid;
    
    if (!targetUuid.trim()) {
      setError('Please enter an invoice UUID');
      return;
    }

    setLoading(true);
    setResult(null);
    setError(null);

    try {
      const res = await fetch(`http://localhost:8080/api/invoices/verify/${targetUuid}`);
      
      if (!res.ok) {
        throw new Error('Verification failed');
      }
      
      const data: VerificationResponse = await res.json();
      setResult(data);
    } catch (err) {
      console.error('❌ Verification failed:', err);
      setError('Unable to verify invoice. Please check the UUID and try again.');
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = (status?: string) => {
    switch (status) {
      case 'VERIFIED': return 'bg-green-50 border-green-500';
      case 'REVOKED': return 'bg-yellow-50 border-yellow-500';
      case 'MODIFIED': return 'bg-red-50 border-red-500';
      case 'NOT_FOUND': return 'bg-gray-50 border-gray-500';
      default: return 'bg-gray-50 border-gray-300';
    }
  };

  const getStatusIcon = (status?: string) => {
    switch (status) {
      case 'VERIFIED': return '✅';
      case 'REVOKED': return '⚠️';
      case 'MODIFIED': return '🚨';
      case 'NOT_FOUND': return '❌';
      default: return '❓';
    }
  };

  return (
    <div className="min-h-screen px-6 py-10 bg-gradient-to-br from-blue-50 to-gray-100 flex flex-col items-center justify-center">
      <div className="max-w-2xl w-full">
        {/* Header */}
        <div className="text-center mb-8">
          <h1 className="text-4xl font-bold text-gray-900 mb-2">
            🔐 Invoice Verification
          </h1>
          <p className="text-gray-600">
            Verify the authenticity of QuoteGuard invoices
          </p>
        </div>

        {/* Input Section */}
        <div className="bg-white p-6 rounded-xl shadow-lg mb-6">
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Invoice UUID
          </label>
          <div className="flex gap-3">
            <input
              type="text"
              placeholder="Enter invoice UUID"
              value={uuid}
              onChange={(e) => setUuid(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && handleVerify()}
              className="flex-1 border border-gray-300 px-4 py-3 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              disabled={loading}
            />
            <button
              onClick={() => handleVerify()}
              disabled={loading || !uuid.trim()}
              className="bg-blue-600 text-white px-6 py-3 rounded-lg hover:bg-blue-700 transition disabled:bg-gray-400 disabled:cursor-not-allowed font-medium"
            >
              {loading ? 'Verifying...' : 'Verify'}
            </button>
          </div>
          {error && (
            <p className="text-red-600 text-sm mt-2">{error}</p>
          )}
        </div>

        {/* Results Section */}
        {result && (
          <div className={`border-4 rounded-xl p-6 shadow-lg ${getStatusColor(result.status)}`}>
            {/* Status Header */}
            <div className="flex items-center justify-between mb-4 pb-4 border-b border-gray-300">
              <div className="flex items-center gap-3">
                <span className="text-4xl">{getStatusIcon(result.status)}</span>
                <div>
                  <h2 className="text-2xl font-bold text-gray-900">
                    {result.status.replace('_', ' ')}
                  </h2>
                  <p className="text-sm text-gray-600 mt-1">{result.message}</p>
                </div>
              </div>
            </div>

            {/* Invoice Details (if found) */}
            {result.freelancerName && (
              <div className="space-y-3">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <p className="text-xs text-gray-500 uppercase">Issued By</p>
                    <p className="font-semibold text-gray-900">{result.freelancerName}</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-500 uppercase">Invoice Number</p>
                    <p className="font-semibold text-gray-900">{result.invoiceNumber}</p>
                  </div>
                  <div>
                    <p className="text-xs text-gray-500 uppercase">Issue Date</p>
                    <p className="font-semibold text-gray-900">{result.issueDate}</p>
                  </div>
                  {result.dueDate && (
                    <div>
                      <p className="text-xs text-gray-500 uppercase">Due Date</p>
                      <p className="font-semibold text-gray-900">{result.dueDate}</p>
                    </div>
                  )}
                  {result.totalAmount && (
                    <div>
                      <p className="text-xs text-gray-500 uppercase">Total Amount</p>
                      <p className="font-semibold text-gray-900 text-lg">
                        {result.currency} {result.totalAmount.toFixed(2)}
                      </p>
                    </div>
                  )}
                </div>

                {/* Revocation Info */}
                {result.status === 'REVOKED' && result.revokedReason && (
                  <div className="mt-4 p-4 bg-yellow-100 border border-yellow-400 rounded-lg">
                    <p className="text-xs text-yellow-800 uppercase font-semibold mb-1">
                      Revocation Reason
                    </p>
                    <p className="text-yellow-900">{result.revokedReason}</p>
                    {result.revokedAt && (
                      <p className="text-xs text-yellow-700 mt-2">
                        Revoked on: {new Date(result.revokedAt).toLocaleString()}
                      </p>
                    )}
                  </div>
                )}

                {/* Warning for Modified */}
                {result.status === 'MODIFIED' && (
                  <div className="mt-4 p-4 bg-red-100 border border-red-400 rounded-lg">
                    <p className="text-red-900 font-semibold">
                      ⚠️ This invoice has been tampered with after issuance. DO NOT TRUST this invoice.
                    </p>
                  </div>
                )}
              </div>
            )}

            {/* Verification Timestamp */}
            <div className="mt-6 pt-4 border-t border-gray-300">
              <p className="text-xs text-gray-500">
                Verified at: {new Date(result.verificationTimestamp).toLocaleString()}
              </p>
            </div>
          </div>
        )}

        {/* Info Section */}
        <div className="mt-8 text-center text-sm text-gray-600">
          <p>
            This verification system uses cryptographic hashing to ensure invoice integrity.
          </p>
          <p className="mt-2">
            Powered by <span className="font-semibold text-blue-600">QuoteGuard</span>
          </p>
        </div>
      </div>
    </div>
  );
}
