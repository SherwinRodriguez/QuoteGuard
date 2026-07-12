// Invoice Service - matches backend/src/main/java/com/quoteguard/controller/InvoiceController.java.
//
// Two response-shape details worth noting since they're easy to get wrong:
// - createInvoice/revokeInvoice return ResponseEntity<String> (a plain-text
//   confirmation message, not JSON) -> responseType: 'text'.
// - getInvoices/getInvoiceById are authenticated; verifyInvoice is the one
//   PUBLIC endpoint in this file (skipAuth: true) - it's what the QR code
//   on a generated PDF links to, and must be reachable with no login.
import { apiRequest } from '@/lib/apiClient';
import { Invoice, InvoiceRequest, RevokeInvoiceRequest, VerificationResponse } from '@/types/invoice';

export const invoiceService = {
  createInvoice(request: InvoiceRequest): Promise<string> {
    return apiRequest<string>('/api/invoices', {
      method: 'POST',
      body: request,
      responseType: 'text',
    });
  },

  getInvoices(): Promise<Invoice[]> {
    return apiRequest<Invoice[]>('/api/invoices');
  },

  getInvoiceById(id: number): Promise<Invoice> {
    return apiRequest<Invoice>(`/api/invoices/${id}`);
  },

  revokeInvoice(uuid: string, request: RevokeInvoiceRequest): Promise<string> {
    return apiRequest<string>(`/api/invoices/${uuid}/revoke`, {
      method: 'POST',
      body: request,
      responseType: 'text',
    });
  },

  /** PUBLIC endpoint - reachable with no login, same as scanning the QR code on the PDF. */
  verifyInvoice(uuid: string): Promise<VerificationResponse> {
    return apiRequest<VerificationResponse>(`/api/invoices/verify/${uuid}`, {
      skipAuth: true,
    });
  },

  downloadPDF(invoiceId: number): Promise<Blob> {
    return apiRequest<Blob>(`/api/invoices/pdf/${invoiceId}`, {
      responseType: 'blob',
    });
  },
};
