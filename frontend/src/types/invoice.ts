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
  subtotal: number;
  tax: number;
  totalAmount: number;
  clientId: number;
  userId: number;
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
