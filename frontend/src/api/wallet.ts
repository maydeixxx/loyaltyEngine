import { api } from './client';
import type { WalletTransactionDTO } from '../types';

export const walletApi = {
  // Returns current balance as number (BigDecimal)
  async getBalance(userId: string): Promise<number> {
    const response = await api.get<number>(`/wallets/${userId}/balance`);
    return Number(response.data);
  },

  // Returns transactions history for the wallet
  async getHistory(userId: string): Promise<WalletTransactionDTO[]> {
    const response = await api.get<WalletTransactionDTO[]>(`/wallets/${userId}/history`);
    return response.data;
  },

  // Admin only: Block wallet
  async blockWallet(userId: string): Promise<void> {
    await api.put(`/wallets/${userId}/block`);
  },

  // Admin only: Unblock wallet
  async unblockWallet(userId: string): Promise<void> {
    await api.put(`/wallets/${userId}/unblock`);
  },
};
