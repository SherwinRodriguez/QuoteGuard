// Invoice Types matching backend DTOs

export type InvoiceStatus = 'ACTIVE' | 'REVOKED';

export interface InvoiceItem {
  product: string;
  quantity: number;
  unitPrice: number;
}

export interface Client {
  id: number;
  name: string;
  email: string;
  gstin?: string;
  phone?: string;
}

export interface Invoice {
  id: number;
  uuid?: string;
  invoiceNumber?: string;
  client: Client;
  totalAmount: number;
  subtotal?: number;
  tax?: number;
  currency?: string;
  issueDate?: string;
  dueDate?: string;
  createdAt: string;
  status?: InvoiceStatus;
  revokedAt?: string;
  revokedReason?: string;
  items: InvoiceItem[];
}

export interface InvoiceRequest {
  invoiceNumber?: string;
  issueDate: string;
  dueDate: string;
  currency: string;
  // Accepted but never trusted - InvoiceService.createInvoice always
  // recomputes this server-side from `items` and rejects a totalAmount
  // that doesn't match subtotal + tax. Sent here only so the request body
  // is self-consistent; the server is the source of truth.
  subtotal: number;
  tax: number;
  totalAmount: number;
  clientId: number;
  // NOTE: no userId field - the acting user is resolved server-side from
  // the JWT (see InvoiceRequest.java on the backend). A client-submitted
  // userId was removed in Phase A as an IDOR risk.
  items: InvoiceItem[];
}

export interface RevokeInvoiceRequest {
  reason: string;
}

export type VerificationStatus = 'VERIFIED' | 'REVOKED' | 'MODIFIED' | 'NOT_FOUND';

export interface VerificationResponse {
  status: VerificationStatus;
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
