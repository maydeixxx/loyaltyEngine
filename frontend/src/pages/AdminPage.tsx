import React, { useState, useEffect } from 'react';
import { rulesApi } from '../api/rules';
import { usersApi } from '../api/users';
import { walletApi } from '../api/wallet';
import { transactionApi } from '../api/transactions';
import type { CashbackRuleDTO, UserDTO, TransactionDTO } from '../types';
import {
  Shield,
  Percent,
  Users,
  Search,
  Plus,
  Trash2,
  Lock,
  Unlock,
  RefreshCw,
  AlertCircle,
  CheckCircle2,
  Calendar,
  X,
  RotateCcw,
} from 'lucide-react';

export const AdminPage: React.FC = () => {
  // Active Tab
  const [activeTab, setActiveTab] = useState<'rules' | 'users' | 'search'>('rules');

  // Rules State
  const [rules, setRules] = useState<CashbackRuleDTO[]>([]);
  const [loadingRules, setLoadingRules] = useState(false);
  const [showAddRuleModal, setShowAddRuleModal] = useState(false);
  const [ruleCategory, setRuleCategory] = useState('');
  const [rulePercentage, setRulePercentage] = useState('5.0');
  const [ruleValidFrom, setRuleValidFrom] = useState('');
  const [ruleValidTo, setRuleValidTo] = useState('');
  const [ruleActionMsg, setRuleActionMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Users State
  const [users, setUsers] = useState<UserDTO[]>([]);
  const [loadingUsers, setLoadingUsers] = useState(false);
  const [walletActionMsg, setWalletActionMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Search Transaction State
  const [searchTxId, setSearchTxId] = useState('');
  const [searchedTx, setSearchedTx] = useState<TransactionDTO | null>(null);
  const [searchingTx, setSearchingTx] = useState(false);
  const [searchTxError, setSearchTxError] = useState<string | null>(null);
  const [cancellingTxId, setCancellingTxId] = useState<string | null>(null);
  const [cancelSuccessMsg, setCancelSuccessMsg] = useState<string | null>(null);
  const [cancelErrorMsg, setCancelErrorMsg] = useState<string | null>(null);

  // Load Rules
  const loadRules = async () => {
    setLoadingRules(true);
    try {
      const data = await rulesApi.getAllRules();
      setRules(data);
    } catch (e) {
      console.error('Failed to load rules', e);
    } finally {
      setLoadingRules(false);
    }
  };

  // Load Users
  const loadUsers = async () => {
    setLoadingUsers(true);
    try {
      const data = await usersApi.getAllUsers();
      setUsers(data);
    } catch (e) {
      console.error('Failed to load users', e);
    } finally {
      setLoadingUsers(false);
    }
  };

  useEffect(() => {
    if (activeTab === 'rules') {
      loadRules();
    } else if (activeTab === 'users') {
      loadUsers();
    }
  }, [activeTab]);

  // Handle Add Rule
  const handleCreateRule = async (e: React.FormEvent) => {
    e.preventDefault();
    setRuleActionMsg(null);

    const pct = parseFloat(rulePercentage);
    if (isNaN(pct) || pct < 1) {
      setRuleActionMsg({ type: 'error', text: 'Процент кэшбэка не может быть меньше 1%' });
      return;
    }

    try {
      const fromIso = ruleValidFrom ? new Date(ruleValidFrom).toISOString().slice(0, 19) : '';
      const toIso = ruleValidTo ? new Date(ruleValidTo).toISOString().slice(0, 19) : '';

      await rulesApi.createRule({
        category: ruleCategory.trim().toLowerCase(),
        percentage: pct,
        validFrom: fromIso,
        validTo: toIso,
      });

      setRuleActionMsg({ type: 'success', text: `Правило для категории "${ruleCategory}" успешно создано!` });
      setShowAddRuleModal(false);
      setRuleCategory('');
      setRulePercentage('5.0');
      loadRules();
    } catch (err: any) {
      console.error('Failed to create rule', err);
      const msg = err.response?.data?.message || err.response?.data || 'Ошибка при создании правила';
      setRuleActionMsg({ type: 'error', text: typeof msg === 'string' ? msg : 'Ошибка создания' });
    }
  };

  // Handle Delete Rule
  const handleDeleteRule = async (ruleId: string) => {
    if (!confirm('Удалить это правило кэшбэка?')) return;
    try {
      await rulesApi.deleteRule(ruleId);
      setRuleActionMsg({ type: 'success', text: 'Правило удалено' });
      loadRules();
    } catch (e) {
      console.error('Failed to delete rule', e);
      setRuleActionMsg({ type: 'error', text: 'Не удалось удалить правило' });
    }
  };

  // Helper to extract rule UUID
  const getRuleIdString = (id: string | { value: string }): string => {
    if (typeof id === 'string') return id;
    return id?.value || '';
  };

  // Handle Block / Unblock Wallet
  const handleBlockWallet = async (userId: string) => {
    try {
      await walletApi.blockWallet(userId);
      setWalletActionMsg({ type: 'success', text: `Кошелек ${userId.substring(0, 8)} заблокирован` });
    } catch (err) {
      console.error(err);
      setWalletActionMsg({ type: 'error', text: 'Не удалось заблокировать кошелек' });
    }
  };

  const handleUnblockWallet = async (userId: string) => {
    try {
      await walletApi.unblockWallet(userId);
      setWalletActionMsg({ type: 'success', text: `Кошелек ${userId.substring(0, 8)} разблокирован` });
    } catch (err) {
      console.error(err);
      setWalletActionMsg({ type: 'error', text: 'Не удалось разблокировать кошелек' });
    }
  };

  // Search Transaction
  const handleSearchTx = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!searchTxId.trim()) return;
    setSearchTxError(null);
    setSearchedTx(null);
    setCancelErrorMsg(null);
    setCancelSuccessMsg(null);
    setSearchingTx(true);

    try {
      const tx = await transactionApi.getTransactionById(searchTxId.trim());
      setSearchedTx(tx);
    } catch (err: any) {
      setSearchTxError('Транзакция с таким UUID не найдена');
    } finally {
      setSearchingTx(false);
    }
  };

  // Admin Cancel Transaction
  const handleAdminCancelTx = async () => {
    if (!searchedTx) return;
    if (!window.confirm(`Вы уверены, что хотите аннулировать чек ${searchedTx.id}? Начисленный кэшбэк пользователя (${searchedTx.userId}) будет списан, а потраченные баллы возвращены.`)) {
      return;
    }

    setCancellingTxId(searchedTx.id);
    setCancelErrorMsg(null);
    setCancelSuccessMsg(null);

    try {
      await transactionApi.cancelTransaction(searchedTx.id, searchedTx.userId);
      setCancelSuccessMsg('Возврат успешно оформлен администратором. Чек аннулирован.');
      setSearchedTx({ ...searchedTx, status: 'CANCELLED' });
    } catch (err: any) {
      console.error('Failed to cancel transaction by admin:', err);
      const msg = err.response?.data?.message || err.message || 'Ошибка отмены транзакции';
      setCancelErrorMsg(typeof msg === 'string' ? msg : 'Не удалось отменить транзакцию');
    } finally {
      setCancellingTxId(null);
    }
  };

  return (
    <div className="min-h-screen bg-[#0d0d0f] py-8 sm:py-12 px-4 sm:px-6 lg:px-8 pb-32 md:pb-16 max-w-7xl mx-auto space-y-8">
      {/* Header */}
      <div className="apple-card p-6 sm:p-7 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-5">
        <div className="flex items-center space-x-3.5">
          <div className="p-3 bg-gradient-to-tr from-[#ff9f0a]/30 to-[#ff9f0a]/10 border border-[#ff9f0a]/30 rounded-2xl text-[#ff9f0a] flex-shrink-0 shadow-inner">
            <Shield className="w-6 h-6" />
          </div>
          <div>
            <span className="text-[11px] uppercase tracking-widest text-[#2997ff] font-semibold">
              Root Console • Admin
            </span>
            <h1 className="text-xl sm:text-2xl font-semibold text-white tracking-tight mt-0.5">
              Панель управления
            </h1>
            <p className="text-xs text-white/50 mt-0.5">
              Правила кэшбэка, клиенты и мониторинг операций
            </p>
          </div>
        </div>

        {/* Apple Segmented Control */}
        <div className="flex bg-white/[0.04] p-1 rounded-full border border-white/10 w-full sm:w-auto backdrop-blur-xl">
          <button
            onClick={() => setActiveTab('rules')}
            className={`flex-1 sm:flex-none flex items-center justify-center space-x-1.5 px-4 py-1.5 rounded-full text-xs transition-all duration-200 ${
              activeTab === 'rules'
                ? 'bg-white text-black font-semibold shadow-sm'
                : 'text-white/60 hover:text-white'
            }`}
          >
            <Percent className="w-3.5 h-3.5" />
            <span>Правила</span>
          </button>
          <button
            onClick={() => setActiveTab('users')}
            className={`flex-1 sm:flex-none flex items-center justify-center space-x-1.5 px-4 py-1.5 rounded-full text-xs transition-all duration-200 ${
              activeTab === 'users'
                ? 'bg-white text-black font-semibold shadow-sm'
                : 'text-white/60 hover:text-white'
            }`}
          >
            <Users className="w-3.5 h-3.5" />
            <span>Клиенты</span>
          </button>
          <button
            onClick={() => setActiveTab('search')}
            className={`flex-1 sm:flex-none flex items-center justify-center space-x-1.5 px-4 py-1.5 rounded-full text-xs transition-all duration-200 ${
              activeTab === 'search'
                ? 'bg-white text-black font-semibold shadow-sm'
                : 'text-white/60 hover:text-white'
            }`}
          >
            <Search className="w-3.5 h-3.5" />
            <span>Поиск</span>
          </button>
        </div>
      </div>

      {/* TAB 1: RULES */}
      {activeTab === 'rules' && (
        <div className="apple-card p-6 sm:p-8 space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 pb-4 border-b border-white/[0.06]">
            <div>
              <h2 className="text-base sm:text-lg font-semibold text-white tracking-tight">Правила кэшбэка</h2>
              <p className="text-xs text-white/50 mt-0.5">
                Базовая ставка по умолчанию — 1%. Для указанных категорий действует персональный процент.
              </p>
            </div>

            <div className="flex items-center space-x-2">
              <button
                onClick={loadRules}
                className="p-2 text-white/50 hover:text-white bg-white/[0.04] hover:bg-white/[0.08] rounded-full transition-all duration-200 border border-white/5"
                title="Обновить список"
              >
                <RefreshCw className={`w-4 h-4 ${loadingRules ? 'animate-spin' : ''}`} />
              </button>
              <button
                onClick={() => setShowAddRuleModal(true)}
                className="px-5 py-2 bg-white hover:bg-white/90 text-black rounded-full text-xs font-semibold flex items-center space-x-1.5 transition-all duration-200 active:scale-[0.98] shadow-sm"
              >
                <Plus className="w-4 h-4" />
                <span>Добавить правило</span>
              </button>
            </div>
          </div>

          {ruleActionMsg && (
            <div
              className={`p-3.5 rounded-2xl text-xs flex items-center space-x-2.5 ${
                ruleActionMsg.type === 'success'
                  ? 'bg-[#30d158]/15 text-[#30d158] border border-[#30d158]/30'
                  : 'bg-[#ff453a]/15 text-[#ff453a] border border-[#ff453a]/30'
              }`}
            >
              {ruleActionMsg.type === 'success' ? (
                <CheckCircle2 className="w-4 h-4 text-[#30d158] flex-shrink-0" />
              ) : (
                <AlertCircle className="w-4 h-4 text-[#ff453a] flex-shrink-0" />
              )}
              <span className="font-medium">{ruleActionMsg.text}</span>
            </div>
          )}

          {/* Rules Table */}
          <div className="border border-white/10 rounded-2xl overflow-x-auto bg-white/[0.02]">
            <div className="min-w-[550px]">
              <div className="bg-white/[0.03] px-4 py-3 text-[11px] font-medium uppercase tracking-wider text-white/40 grid grid-cols-12 gap-2 border-b border-white/[0.06]">
                <span className="col-span-3">Категория</span>
                <span className="col-span-2">Кэшбэк</span>
                <span className="col-span-3">Действует с</span>
                <span className="col-span-3">Действует до</span>
                <span className="col-span-1 text-right">Действия</span>
              </div>

              <div className="divide-y divide-white/[0.04]">
                {rules.length === 0 ? (
                  <div className="p-8 text-center text-xs text-white/40">
                    Правил пока нет. Нажмите "Добавить правило", чтобы настроить категорию.
                  </div>
                ) : (
                  rules.map((rule, idx) => {
                    const idStr = getRuleIdString(rule.id);
                    return (
                      <div
                        key={idx}
                        className="px-4 py-3.5 text-xs grid grid-cols-12 gap-2 items-center hover:bg-white/[0.02] transition"
                      >
                        <span className="col-span-3 font-medium text-white flex items-center space-x-2">
                          <span className="w-2 h-2 rounded-full bg-[#2997ff]"></span>
                          <span className="font-mono bg-white/[0.04] text-white/90 px-2 py-0.5 rounded-full border border-white/10">
                            {rule.category}
                          </span>
                        </span>
                        <span className="col-span-2 font-bold text-[#30d158] text-sm">
                          {rule.percentage}%
                        </span>
                        <span className="col-span-3 text-white/50 text-[11px] flex items-center space-x-1.5">
                          <Calendar className="w-3.5 h-3.5 text-white/40" />
                          <span>{rule.validFrom ? new Date(rule.validFrom).toLocaleDateString('ru-RU') : '—'}</span>
                        </span>
                        <span className="col-span-3 text-white/50 text-[11px] flex items-center space-x-1.5">
                          <Calendar className="w-3.5 h-3.5 text-white/40" />
                          <span>{rule.validTo ? new Date(rule.validTo).toLocaleDateString('ru-RU') : '—'}</span>
                        </span>
                        <div className="col-span-1 text-right">
                          <button
                            onClick={() => handleDeleteRule(idStr)}
                            title="Удалить правило"
                            className="p-1.5 text-white/40 hover:text-[#ff453a] hover:bg-[#ff453a]/10 rounded-full transition"
                          >
                            <Trash2 className="w-4 h-4" />
                          </button>
                        </div>
                      </div>
                    );
                  })
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* TAB 2: USERS & WALLETS */}
      {activeTab === 'users' && (
        <div className="apple-card p-6 sm:p-8 space-y-6">
          <div className="flex items-center justify-between pb-4 border-b border-white/[0.06]">
            <div>
              <h2 className="text-base sm:text-lg font-semibold text-white tracking-tight">Клиенты и кошельки</h2>
              <p className="text-xs text-white/50 mt-0.5">
                Управление статусом счетов в WalletService
              </p>
            </div>
            <button
              onClick={loadUsers}
              className="p-2 text-white/50 hover:text-white bg-white/[0.04] hover:bg-white/[0.08] rounded-full transition-all duration-200 border border-white/5"
            >
              <RefreshCw className={`w-4 h-4 ${loadingUsers ? 'animate-spin' : ''}`} />
            </button>
          </div>

          {walletActionMsg && (
            <div
              className={`p-3.5 rounded-2xl text-xs flex items-center space-x-2.5 ${
                walletActionMsg.type === 'success'
                  ? 'bg-[#30d158]/15 text-[#30d158] border border-[#30d158]/30'
                  : 'bg-[#ff453a]/15 text-[#ff453a] border border-[#ff453a]/30'
              }`}
            >
              {walletActionMsg.type === 'success' ? (
                <CheckCircle2 className="w-4 h-4 text-[#30d158] flex-shrink-0" />
              ) : (
                <AlertCircle className="w-4 h-4 text-[#ff453a] flex-shrink-0" />
              )}
              <span className="font-medium">{walletActionMsg.text}</span>
            </div>
          )}

          <div className="border border-white/10 rounded-2xl overflow-x-auto bg-white/[0.02]">
            <div className="min-w-[500px]">
              <div className="bg-white/[0.03] px-4 py-3 text-[11px] font-medium uppercase tracking-wider text-white/40 grid grid-cols-12 gap-2 border-b border-white/[0.06]">
                <span className="col-span-4">Пользователь</span>
                <span className="col-span-4">Email</span>
                <span className="col-span-4 text-right">Управление</span>
              </div>

              <div className="divide-y divide-white/[0.04]">
                {users.length === 0 ? (
                  <div className="p-8 text-center text-xs text-white/40">Клиенты не найдены</div>
                ) : (
                  users.map((u) => (
                    <div
                      key={u.id}
                      className="px-4 py-3.5 text-xs grid grid-cols-12 gap-2 items-center hover:bg-white/[0.02] transition"
                    >
                      <div className="col-span-4">
                        <div className="font-medium text-white">
                          {u.firstName} {u.lastName}
                        </div>
                        <div className="text-[10px] text-white/40 font-mono truncate">{u.id}</div>
                      </div>

                      <div className="col-span-4 font-mono text-xs text-white/70">{u.email}</div>

                      <div className="col-span-4 flex justify-end space-x-2">
                        <button
                          onClick={() => handleBlockWallet(u.id)}
                          className="px-3 py-1.5 text-[11px] font-medium text-[#ff453a] bg-[#ff453a]/15 hover:bg-[#ff453a]/25 border border-[#ff453a]/30 rounded-full flex items-center space-x-1 transition active:scale-95"
                        >
                          <Lock className="w-3 h-3" />
                          <span>Блок</span>
                        </button>
                        <button
                          onClick={() => handleUnblockWallet(u.id)}
                          className="px-3 py-1.5 text-[11px] font-medium text-[#30d158] bg-[#30d158]/15 hover:bg-[#30d158]/25 border border-[#30d158]/30 rounded-full flex items-center space-x-1 transition active:scale-95"
                        >
                          <Unlock className="w-3 h-3" />
                          <span>Разблок</span>
                        </button>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* TAB 3: SEARCH TX */}
      {activeTab === 'search' && (
        <div className="apple-card p-6 sm:p-8 space-y-6">
          <div className="pb-4 border-b border-white/[0.06]">
            <h2 className="text-base sm:text-lg font-semibold text-white tracking-tight">Поиск чека по UUID</h2>
            <p className="text-xs text-white/50 mt-0.5">
              Административный просмотр информации о любом чеке в системе
            </p>
          </div>

          <form onSubmit={handleSearchTx} className="flex gap-2 max-w-xl">
            <input
              type="text"
              placeholder="Введите UUID транзакции..."
              value={searchTxId}
              onChange={(e) => setSearchTxId(e.target.value)}
              className="flex-1 px-4 py-2.5 bg-white/[0.04] border border-white/10 rounded-xl text-xs text-white placeholder-white/30 focus:border-[#2997ff] focus:ring-1 focus:ring-[#2997ff]/40 focus:outline-none transition"
            />
            <button
              type="submit"
              disabled={searchingTx}
              className="px-6 py-2.5 bg-white hover:bg-white/90 text-black font-semibold text-xs rounded-full flex items-center space-x-2 transition active:scale-[0.98] shadow-sm"
            >
              <Search className="w-3.5 h-3.5" />
              <span>Найти</span>
            </button>
          </form>

          {searchTxError && (
            <div className="p-3.5 bg-[#ff453a]/15 border border-[#ff453a]/30 text-[#ff453a] text-xs rounded-2xl flex items-center space-x-2">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              <span className="font-medium">{searchTxError}</span>
            </div>
          )}

          {searchedTx && (
            <div className="p-6 bg-white/[0.03] border border-white/10 rounded-2xl space-y-4 text-xs">
              <div className="flex items-center justify-between pb-3 border-b border-white/[0.06]">
                <div className="font-mono font-semibold text-white">ID: {searchedTx.id}</div>
                {searchedTx.status === 'PROCESSED' || searchedTx.status === 'HANDLED' ? (
                  <span className="px-3 py-1 bg-[#30d158]/15 text-[#30d158] border border-[#30d158]/30 font-medium rounded-full text-[11px]">
                    {searchedTx.status}
                  </span>
                ) : searchedTx.status === 'CANCELLED' ? (
                  <span className="px-3 py-1 bg-[#ff453a]/15 text-[#ff453a] border border-[#ff453a]/30 font-medium rounded-full text-[11px] flex items-center space-x-1.5">
                    <RotateCcw className="w-3 h-3" />
                    <span>АННУЛИРОВАН</span>
                  </span>
                ) : (
                  <span className="px-3 py-1 bg-[#2997ff]/15 text-[#2997ff] border border-[#2997ff]/30 font-medium rounded-full text-[11px]">
                    {searchedTx.status}
                  </span>
                )}
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
                <div>
                  <span className="text-white/40 block text-[10px] uppercase tracking-wider">Клиент (User ID):</span>
                  <span className="font-mono text-white font-medium">{searchedTx.userId}</span>
                </div>
                <div>
                  <span className="text-white/40 block text-[10px] uppercase tracking-wider">Сумма покупки:</span>
                  <span className={`font-bold text-sm ${searchedTx.status === 'CANCELLED' ? 'text-white/40 line-through' : 'text-white'}`}>
                    {searchedTx.amount} ₽
                  </span>
                </div>
                <div>
                  <span className="text-white/40 block text-[10px] uppercase tracking-wider">Дата создания:</span>
                  <span className="text-white/80">{new Date(searchedTx.createdAt).toLocaleString('ru-RU')}</span>
                </div>
              </div>

              <div>
                <span className="font-medium text-white/50 block mb-2 text-[11px] uppercase tracking-wider">
                  Позиции ({searchedTx.items?.length || 0}):
                </span>
                <div className="divide-y divide-white/[0.04] border border-white/10 rounded-2xl bg-white/[0.02] overflow-hidden">
                  {searchedTx.items?.map((it, i) => (
                    <div key={i} className="p-3 flex justify-between items-center text-xs">
                      <div>
                        <span className="font-medium text-white">{it.name}</span>
                        <span className="ml-2 text-[10px] text-white/40 font-mono">({it.category})</span>
                      </div>
                      <span className="font-semibold text-white">{it.price} ₽</span>
                    </div>
                  ))}
                </div>
              </div>

              {/* Admin Refund Action */}
              {(searchedTx.status === 'PROCESSED' || searchedTx.status === 'HANDLED') && (
                <div className="pt-4 border-t border-white/[0.08] space-y-3">
                  {cancelErrorMsg && (
                    <div className="p-3 bg-[#ff453a]/15 border border-[#ff453a]/30 text-[#ff453a] text-xs rounded-xl flex items-center space-x-2">
                      <AlertCircle className="w-4 h-4 flex-shrink-0" />
                      <span>{cancelErrorMsg}</span>
                    </div>
                  )}
                  {cancelSuccessMsg && (
                    <div className="p-3 bg-[#30d158]/15 border border-[#30d158]/30 text-[#30d158] text-xs rounded-xl flex items-center space-x-2">
                      <CheckCircle2 className="w-4 h-4 flex-shrink-0" />
                      <span>{cancelSuccessMsg}</span>
                    </div>
                  )}
                  <button
                    onClick={handleAdminCancelTx}
                    disabled={cancellingTxId === searchedTx.id}
                    className="w-full sm:w-auto px-5 py-2.5 bg-[#ff453a]/15 hover:bg-[#ff453a]/25 text-[#ff453a] border border-[#ff453a]/30 font-medium rounded-xl text-xs transition flex items-center justify-center space-x-2 active:scale-[0.98]"
                  >
                    {cancellingTxId === searchedTx.id ? (
                      <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                    ) : (
                      <>
                        <RotateCcw className="w-3.5 h-3.5" />
                        <span>Оформить возврат (отмена транзакции)</span>
                      </>
                    )}
                  </button>
                </div>
              )}

              {searchedTx.status === 'CANCELLED' && (
                <div className="p-3 bg-white/[0.03] border border-white/10 rounded-xl text-[11px] text-white/50 flex items-center space-x-2">
                  <RotateCcw className="w-3.5 h-3.5 text-[#ff453a] flex-shrink-0" />
                  <span>Транзакция аннулирована. Все начисления и списания скорректированы.</span>
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* Modal: Create Cashback Rule */}
      {showAddRuleModal && (
        <div className="fixed inset-0 bg-black/75 backdrop-blur-xl flex items-center justify-center p-4 z-50 animate-in fade-in duration-200">
          <div className="apple-card max-w-md w-full p-6 sm:p-7 border border-white/20 shadow-2xl animate-in zoom-in-95 duration-200 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-white/[0.08]">
              <div className="flex items-center space-x-2">
                <Percent className="w-5 h-5 text-[#2997ff]" />
                <h3 className="font-semibold text-white text-base">Новое правило кэшбэка</h3>
              </div>
              <button
                onClick={() => setShowAddRuleModal(false)}
                className="w-7 h-7 flex items-center justify-center rounded-full bg-white/10 hover:bg-white/20 text-white text-xs transition"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            </div>

            <form onSubmit={handleCreateRule} className="space-y-4 text-xs">
              <div>
                <label className="block text-[11px] uppercase tracking-wider font-medium text-white/50 mb-1.5">
                  Категория (category)
                </label>
                <input
                  type="text"
                  required
                  placeholder="напр. groceries, electronics, cafe"
                  value={ruleCategory}
                  onChange={(e) => setRuleCategory(e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-white/[0.04] border border-white/10 rounded-xl text-xs text-white placeholder-white/30 focus:border-[#2997ff] focus:ring-1 focus:ring-[#2997ff]/40 focus:outline-none transition"
                />
              </div>

              <div>
                <label className="block text-[11px] uppercase tracking-wider font-medium text-white/50 mb-1.5">
                  Процент кэшбэка (минимум 1%)
                </label>
                <div className="relative">
                  <input
                    type="number"
                    step="0.5"
                    min="1"
                    required
                    placeholder="5.0"
                    value={rulePercentage}
                    onChange={(e) => setRulePercentage(e.target.value)}
                    className="w-full pl-3.5 pr-8 py-2.5 bg-white/[0.04] border border-white/10 rounded-xl text-xs text-white focus:border-[#2997ff] focus:ring-1 focus:ring-[#2997ff]/40 focus:outline-none transition"
                  />
                  <span className="absolute right-3.5 top-2.5 text-white/40 font-medium">%</span>
                </div>
              </div>

              <div>
                <label className="block text-[11px] uppercase tracking-wider font-medium text-white/50 mb-1.5">
                  Дата начала действия (validFrom)
                </label>
                <input
                  type="datetime-local"
                  required
                  value={ruleValidFrom}
                  onChange={(e) => setRuleValidFrom(e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-[#16161a] border border-white/10 rounded-xl text-xs text-white focus:border-[#2997ff] focus:outline-none transition"
                />
              </div>

              <div>
                <label className="block text-[11px] uppercase tracking-wider font-medium text-white/50 mb-1.5">
                  Дата окончания действия (validTo)
                </label>
                <input
                  type="datetime-local"
                  required
                  value={ruleValidTo}
                  onChange={(e) => setRuleValidTo(e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-[#16161a] border border-white/10 rounded-xl text-xs text-white focus:border-[#2997ff] focus:outline-none transition"
                />
              </div>

              <div className="pt-3 flex space-x-3">
                <button
                  type="button"
                  onClick={() => setShowAddRuleModal(false)}
                  className="w-1/2 py-2.5 bg-white/10 hover:bg-white/15 text-white font-medium rounded-full text-xs transition"
                >
                  Отмена
                </button>
                <button
                  type="submit"
                  className="w-1/2 py-2.5 bg-white hover:bg-white/90 text-black font-semibold rounded-full text-xs transition shadow-sm"
                >
                  Создать
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
