// Invoice Service - API calls

import { Invoice, InvoiceRequest, RevokeInvoiceRequest, VerificationResponse } from '@/types/invoice';

const API_BASE = 'http://localhost:8080/api';

export const invoiceService = {
  // Create new invoice
  async createInvoice(request: InvoiceRequest): Promise<string> {
    const res = await fetch(`${API_BASE}/invoices`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    });

    if (!res.ok) {
      const error = await res.text();
      throw new Error(error || 'Failed to create invoice');
    }

    return res.text();
  },

  // Get all invoices for user
  async getInvoicesByUser(userId: number): Promise<Invoice[]> {
    const res = await fetch(`${API_BASE}/invoices?userId=${userId}`);
    
    if (!res.ok) {
      throw new Error('Failed to fetch invoices');
    }

    return res.json();
  },

  // Get invoice by ID
  async getInvoiceById(id: number): Promise<Invoice> {
    const res = await fetch(`${API_BASE}/invoices/${id}`);
    
    if (!res.ok) {
      throw new Error('Invoice not found');
    }

    return res.json();
  },

  // Revoke invoice
  async revokeInvoice(uuid: string, userId: number, request: RevokeInvoiceRequest): Promise<string> {
    const res = await fetch(`${API_BASE}/invoices/${uuid}/revoke?userId=${userId}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    });

    if (!res.ok) {
      const error = await res.text();
      throw new Error(error || 'Failed to revoke invoice');
    }

    return res.text();
  },

  // Verify invoice (public endpoint)
  async verifyInvoice(uuid: string): Promise<VerificationResponse> {
    const res = await fetch(`${API_BASE}/invoices/verify/${uuid}`);
    
    if (!res.ok) {
      throw new Error('Verification failed');
    }

    return res.json();
  },

  // Download invoice PDF
  async downloadPDF(invoiceId: number): Promise<Blob> {
    const res = await fetch(`${API_BASE}/invoices/pdf/${invoiceId}`);
    
    if (!res.ok) {
      throw new Error('PDF not found');
    }

    return res.blob();
  },

  // Get PDF URL for preview
  getPDFUrl(invoiceId: number): string {
    return `${API_BASE}/invoices/pdf/${invoiceId}`;
  },
};
