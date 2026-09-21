import { api } from './client';
import type { CashbackRuleDTO, CreateRulePayload, UpdateRulePayload } from '../types';

export const rulesApi = {
  // Admin: Get all cashback rules
  async getAllRules(): Promise<CashbackRuleDTO[]> {
    const response = await api.get<CashbackRuleDTO[]>('/rules');
    return response.data;
  },

  // Admin: Create new cashback rule
  async createRule(payload: CreateRulePayload): Promise<string> {
    const response = await api.post<string>('/rules', payload);
    return response.data;
  },

  // Admin: Update rule
  async updateRule(id: string, payload: UpdateRulePayload): Promise<string> {
    const response = await api.put<string>(`/rules/${id}`, payload);
    return response.data;
  },

  // Admin: Delete rule
  async deleteRule(id: string): Promise<void> {
    await api.delete(`/rules/${id}`);
  },
};
