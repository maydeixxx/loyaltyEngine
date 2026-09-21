import { api } from './client';
import type { UserDTO } from '../types';

export interface RegisterRequest {
  email: string;
  firstName: string;
  lastName: string;
  password: string;
}

export interface AuthRequest {
  email: string;
  password: string;
}

export const authApi = {
  // Returns raw JWT string
  async login(credentials: AuthRequest): Promise<string> {
    const response = await api.post<string>('/users/auth', credentials);
    return response.data;
  },

  // Returns registered user DTO
  async register(data: RegisterRequest): Promise<UserDTO> {
    const response = await api.post<UserDTO>('/users/register', data);
    return response.data;
  },

  // Returns current user profile based on JWT claims
  async getSelf(): Promise<UserDTO> {
    const response = await api.get<UserDTO>('/users');
    return response.data;
  },
};
