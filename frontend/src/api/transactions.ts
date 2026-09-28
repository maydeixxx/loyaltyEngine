import { api } from './client';
import type { CreateTransactionPayload, TransactionDTO } from '../types';

export const transactionApi = {
  // Create a new purchase transaction
  async createTransaction(
    payload: CreateTransactionPayload,
    userId?: string,
    customIdempotencyKey?: string
  ): Promise<TransactionDTO> {
    const idempotencyKey = customIdempotencyKey || crypto.randomUUID();
    const headers: Record<string, string> = {
      'X-IDEMPOTENCY-KEY': idempotencyKey,
    };
    if (userId) {
      headers['X-User-Id'] = userId;
    }

    const response = await api.post<TransactionDTO>('/transactions', payload, {
      headers,
    });
    return response.data;
  },

  // Get all transactions for a user
  async getUserTransactions(userId: string): Promise<TransactionDTO[]> {
    const response = await api.get<TransactionDTO[]>(`/transactions/user/${userId}`);
    return response.data;
  },

  // Get single transaction by ID
  async getTransactionById(id: string): Promise<TransactionDTO> {
    const response = await api.get<TransactionDTO>(`/transactions/${id}`);
    return response.data;
  },
};
