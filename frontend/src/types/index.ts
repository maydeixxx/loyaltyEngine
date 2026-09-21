export type UserRole = 'ROLE_USER' | 'ROLE_ADMIN' | 'USER' | 'ADMIN';

export interface UserDTO {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
}

export interface JWTPayload {
  sub: string;
  User_id: string;
  Role: string;
  exp: number;
  iat: number;
}

export type TransactionType = 'CREDIT' | 'DEBIT';

export interface WalletTransactionDTO {
  id: string;
  walletId: string;
  transactionId: string;
  amount: number;
  type: TransactionType;
  createdAt: string;
  description: string;
}

export interface TransactionItemDTO {
  category: string;
  name: string;
  price: number;
}

export interface CreateTransactionItem {
  category: string;
  name: string;
  price: number;
}

export interface CreateTransactionPayload {
  amount: number;
  items: CreateTransactionItem[];
  useCashbackBalance: boolean;
}

export interface TransactionDTO {
  id: string;
  userId: string;
  idempotencyKey: string;
  amount: number;
  items: TransactionItemDTO[];
  createdAt: string;
  status: 'NEW' | 'PENDING' | 'HANDLED' | 'FAILED' | 'REJECTED' | string;
}

export interface CashbackRuleDTO {
  id: string | { value: string };
  category: string;
  percentage: number;
  validFrom: string;
  validTo: string;
}

export interface CreateRulePayload {
  category: string;
  percentage: number;
  validFrom: string;
  validTo: string;
}

export interface UpdateRulePayload {
  category?: string;
  percentage?: number;
}

export interface UpdateUserPayload {
  fieldToUpdate: 'firstname' | 'lastname' | 'email' | 'password';
  email?: string;
  firstName?: string;
  lastName?: string;
  oldPassword?: string;
  newPassword?: string;
}
