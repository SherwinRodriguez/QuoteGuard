// Client (customer) CRUD, matching backend/src/main/java/com/quoteguard/controller/ClientController.java.
// No userId parameter anywhere here - every endpoint is authenticated and
// the acting user is resolved server-side from the JWT (apiClient attaches
// the Authorization header automatically). Previously every call site did
// `fetch(...?userId=${localStorage.getItem("userId")})`, trusting a raw,
// unsigned value straight out of localStorage - closed by the backend in
// Phase A/B, and this is the frontend catching up to that.
import { apiRequest } from '@/lib/apiClient';

export interface Client {
  id: number;
  name: string;
  email: string;
  gstin?: string;
  phone?: string;
}

export interface ClientRequest {
  name: string;
  email: string;
  gstin?: string;
  phone?: string;
}

export const clientService = {
  getClients(): Promise<Client[]> {
    return apiRequest<Client[]>('/api/clients');
  },

  getClientById(id: number): Promise<Client> {
    return apiRequest<Client>(`/api/clients/${id}`);
  },

  createClient(request: ClientRequest): Promise<Client> {
    return apiRequest<Client>('/api/clients', { method: 'POST', body: request });
  },

  updateClient(id: number, request: ClientRequest): Promise<Client> {
    return apiRequest<Client>(`/api/clients/${id}`, { method: 'PUT', body: request });
  },

  deleteClient(id: number): Promise<void> {
    return apiRequest<void>(`/api/clients/${id}`, { method: 'DELETE', responseType: 'void' });
  },
};
