import { api } from './client';
import type { UpdateUserPayload, UserDTO } from '../types';

export const usersApi = {
  // Admin: Get list of all users
  async getAllUsers(): Promise<UserDTO[]> {
    const response = await api.get<UserDTO[]>('/users/get_all');
    return response.data;
  },

  // Admin: Get user by UUID
  async getUserById(id: string): Promise<UserDTO> {
    const response = await api.get<UserDTO>(`/users/id/${id}`);
    return response.data;
  },

  // User / Admin: Update user fields
  async updateUser(email: string, payload: UpdateUserPayload): Promise<void> {
    await api.put(`/users/${email}`, payload);
  },

  // User / Admin: Delete user
  async deleteUser(email: string): Promise<void> {
    await api.delete(`/users/${email}`);
  },
};
