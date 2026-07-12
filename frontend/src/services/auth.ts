// Auth API calls, matching backend/src/main/java/com/quoteguard/controller/AuthController.java
// exactly: register returns 201 with no body, login/refresh return
// AuthResponse (accessToken/refreshToken/tokenType/userId), logout returns
// 204. All four are public endpoints (skipAuth: true) - see SecurityConfig.
import { apiRequest } from '@/lib/apiClient';

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  userId: number;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export const authService = {
  register(request: RegisterRequest): Promise<void> {
    return apiRequest<void>('/api/auth/register', {
      method: 'POST',
      body: request,
      skipAuth: true,
      responseType: 'void',
    });
  },

  login(request: LoginRequest): Promise<AuthResponse> {
    return apiRequest<AuthResponse>('/api/auth/login', {
      method: 'POST',
      body: request,
      skipAuth: true,
    });
  },

  refresh(refreshToken: string): Promise<AuthResponse> {
    return apiRequest<AuthResponse>('/api/auth/refresh', {
      method: 'POST',
      body: { refreshToken },
      skipAuth: true,
    });
  },

  logout(refreshToken: string): Promise<void> {
    return apiRequest<void>('/api/auth/logout', {
      method: 'POST',
      body: { refreshToken },
      skipAuth: true,
      responseType: 'void',
    });
  },
};
