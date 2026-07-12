// Matches backend/src/main/java/com/quoteguard/controller/DashboardController.java,
// which resolves the acting user from the JWT (no userId query param).
import { apiRequest } from '@/lib/apiClient';

export interface DashboardStats {
  clients: number;
  invoices: number;
  pending: number;
}

export const dashboardService = {
  getStats(): Promise<DashboardStats> {
    return apiRequest<DashboardStats>('/api/dashboard/stats');
  },
};
