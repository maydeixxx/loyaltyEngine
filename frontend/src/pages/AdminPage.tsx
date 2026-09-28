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
      // Format datetime to ISO string without millis/timezone if needed by Spring
      const fromIso = ruleValidFrom ? new Date(ruleValidFrom).toISOString().slice(0, 19) : '';
      const toIso = ruleValidTo ? new Date(ruleValidTo).toISOString().slice(0, 19) : '';

      await rulesApi.createRule({
        category: ruleCategory.trim().toLowerCase(),
        percentage: pct,
        validFrom: fromIso,
        validTo: toIso,
      });

      setRuleActionMsg({ type: 'success', text: `Правило для "${ruleCategory}" успешно создано!` });
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

  return (
    <div className="min-h-screen bg-slate-50 py-3 sm:py-6 px-3 sm:px-6 lg:px-8 pb-24 md:pb-12 max-w-7xl mx-auto overflow-x-hidden">
      <div className="space-y-4 sm:space-y-6">
        {/* Header */}
        <div className="bg-white p-4 sm:p-6 rounded-2xl sm:rounded-3xl border border-slate-200/80 shadow-xs flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <div className="flex items-center space-x-3">
            <div className="p-2.5 sm:p-3 bg-amber-500 rounded-2xl text-white shadow-md shadow-amber-200 flex-shrink-0">
              <Shield className="w-5 h-5 sm:w-6 sm:h-6" />
            </div>
            <div>
              <h1 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight">
                Панель администратора
              </h1>
              <p className="text-[11px] sm:text-xs text-slate-500">
                Управление правилами начисления кэшбэка и кошельками
              </p>
            </div>
          </div>

          {/* Tab Navigation */}
          <div className="flex bg-slate-100 p-1 rounded-xl w-full sm:w-auto overflow-x-auto">
            <button
              onClick={() => setActiveTab('rules')}
              className={`flex-1 sm:flex-none flex items-center justify-center space-x-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
                activeTab === 'rules' ? 'bg-white text-slate-900 shadow-xs' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <Percent className="w-3.5 h-3.5" />
              <span>Правила</span>
            </button>
            <button
              onClick={() => setActiveTab('users')}
              className={`flex-1 sm:flex-none flex items-center justify-center space-x-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
                activeTab === 'users' ? 'bg-white text-slate-900 shadow-xs' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <Users className="w-3.5 h-3.5" />
              <span>Юзеры</span>
            </button>
            <button
              onClick={() => setActiveTab('search')}
              className={`flex-1 sm:flex-none flex items-center justify-center space-x-1.5 px-3 py-1.5 rounded-lg text-xs font-semibold transition ${
                activeTab === 'search' ? 'bg-white text-slate-900 shadow-xs' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <Search className="w-3.5 h-3.5" />
              <span>Поиск</span>
            </button>
          </div>
        </div>

        {/* TAB 1: RULES */}
        {activeTab === 'rules' && (
          <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-200/80 shadow-sm space-y-6">
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
              <div>
                <h2 className="text-lg font-bold text-slate-900">Правила начисления кэшбэка</h2>
                <p className="text-xs text-slate-500">
                  Базовый кэшбэк по умолчанию равен 1%. Для настроенных категорий применяется указанный процент.
                </p>
              </div>

              <div className="flex space-x-2">
                <button
                  onClick={loadRules}
                  className="p-2 text-slate-400 hover:text-slate-700 bg-slate-50 hover:bg-slate-100 rounded-xl transition"
                  title="Обновить список"
                >
                  <RefreshCw className={`w-4 h-4 ${loadingRules ? 'animate-spin' : ''}`} />
                </button>
                <button
                  onClick={() => setShowAddRuleModal(true)}
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-semibold flex items-center space-x-1.5 transition shadow-sm shadow-indigo-200"
                >
                  <Plus className="w-4 h-4" />
                  <span>Добавить правило</span>
                </button>
              </div>
            </div>

            {ruleActionMsg && (
              <div
                className={`p-4 rounded-xl text-xs flex items-center space-x-2 ${
                  ruleActionMsg.type === 'success'
                    ? 'bg-emerald-50 text-emerald-800 border border-emerald-100'
                    : 'bg-rose-50 text-rose-800 border border-rose-100'
                }`}
              >
                {ruleActionMsg.type === 'success' ? (
                  <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                ) : (
                  <AlertCircle className="w-4 h-4 text-rose-600" />
                )}
                <span>{ruleActionMsg.text}</span>
              </div>
            )}

            {/* Rules Table */}
            <div className="border border-slate-200 rounded-xl sm:rounded-2xl overflow-x-auto shadow-xs">
              <div className="min-w-[550px]">
                <div className="bg-slate-50 px-4 py-3 text-xs font-semibold text-slate-600 grid grid-cols-12 gap-2">
                <span className="col-span-3">Категория</span>
                <span className="col-span-2">Кэшбэк</span>
                <span className="col-span-3">Действует с</span>
                <span className="col-span-3">Действует до</span>
                <span className="col-span-1 text-right">Действия</span>
              </div>

              <div className="divide-y divide-slate-100">
                {rules.length === 0 ? (
                  <div className="p-8 text-center text-xs text-slate-400">
                    Правил пока нет. Нажмите "Добавить правило", чтобы создать категорию кэшбэка.
                  </div>
                ) : (
                  rules.map((rule, idx) => {
                    const idStr = getRuleIdString(rule.id);
                    return (
                      <div key={idx} className="px-4 py-3 text-xs grid grid-cols-12 gap-2 items-center hover:bg-slate-50/60 transition">
                        <span className="col-span-3 font-semibold text-slate-900 flex items-center space-x-2">
                          <span className="w-2 h-2 rounded-full bg-indigo-600"></span>
                          <span className="font-mono bg-indigo-50 text-indigo-700 px-2 py-0.5 rounded-md">
                            {rule.category}
                          </span>
                        </span>
                        <span className="col-span-2 font-bold text-emerald-600 text-sm">
                          {rule.percentage}%
                        </span>
                        <span className="col-span-3 text-slate-500 font-mono text-[11px] flex items-center space-x-1">
                          <Calendar className="w-3 h-3 text-slate-400" />
                          <span>{rule.validFrom ? new Date(rule.validFrom).toLocaleDateString('ru-RU') : '—'}</span>
                        </span>
                        <span className="col-span-3 text-slate-500 font-mono text-[11px] flex items-center space-x-1">
                          <Calendar className="w-3 h-3 text-slate-400" />
                          <span>{rule.validTo ? new Date(rule.validTo).toLocaleDateString('ru-RU') : '—'}</span>
                        </span>
                        <div className="col-span-1 text-right">
                          <button
                            onClick={() => handleDeleteRule(idStr)}
                            title="Удалить правило"
                            className="p-1.5 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-lg transition"
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
          <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-200/80 shadow-sm space-y-6">
            <div className="flex items-center justify-between">
              <div>
                <h2 className="text-lg font-bold text-slate-900">Пользователи и блокировка кошельков</h2>
                <p className="text-xs text-slate-500">
                  Управление статусом кошельков в сервисе WalletService
                </p>
              </div>
              <button
                onClick={loadUsers}
                className="p-2 text-slate-400 hover:text-slate-700 bg-slate-50 hover:bg-slate-100 rounded-xl transition"
              >
                <RefreshCw className={`w-4 h-4 ${loadingUsers ? 'animate-spin' : ''}`} />
              </button>
            </div>

            {walletActionMsg && (
              <div
                className={`p-4 rounded-xl text-xs flex items-center space-x-2 ${
                  walletActionMsg.type === 'success'
                    ? 'bg-emerald-50 text-emerald-800 border border-emerald-100'
                    : 'bg-rose-50 text-rose-800 border border-rose-100'
                }`}
              >
                {walletActionMsg.type === 'success' ? (
                  <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                ) : (
                  <AlertCircle className="w-4 h-4 text-rose-600" />
                )}
                <span>{walletActionMsg.text}</span>
              </div>
            )}

            <div className="border border-slate-200 rounded-xl sm:rounded-2xl overflow-x-auto shadow-xs">
              <div className="min-w-[500px]">
                <div className="bg-slate-50 px-4 py-3 text-xs font-semibold text-slate-600 grid grid-cols-12 gap-2">
                  <span className="col-span-4">Пользователь</span>
                  <span className="col-span-4">Email</span>
                  <span className="col-span-4 text-right">Управление кошельком</span>
                </div>

                <div className="divide-y divide-slate-100">
                  {users.length === 0 ? (
                    <div className="p-8 text-center text-xs text-slate-400">Пользователи не найдены</div>
                  ) : (
                    users.map((u) => (
                      <div key={u.id} className="px-4 py-3 text-xs grid grid-cols-12 gap-2 items-center hover:bg-slate-50/60 transition">
                        <div className="col-span-4">
                          <div className="font-semibold text-slate-900">
                            {u.firstName} {u.lastName}
                          </div>
                          <div className="text-[10px] text-slate-400 font-mono">{u.id}</div>
                        </div>

                        <div className="col-span-4 font-mono text-slate-600">{u.email}</div>

                        <div className="col-span-4 flex justify-end space-x-2">
                          <button
                            onClick={() => handleBlockWallet(u.id)}
                            className="px-2.5 py-1 text-[11px] font-semibold text-rose-700 bg-rose-50 hover:bg-rose-100 border border-rose-200 rounded-lg flex items-center space-x-1 transition"
                          >
                            <Lock className="w-3 h-3" />
                            <span>Блок</span>
                          </button>
                          <button
                            onClick={() => handleUnblockWallet(u.id)}
                            className="px-2.5 py-1 text-[11px] font-semibold text-emerald-700 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 rounded-lg flex items-center space-x-1 transition"
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
          <div className="bg-white rounded-3xl p-6 sm:p-8 border border-slate-200/80 shadow-sm space-y-6">
            <div>
              <h2 className="text-lg font-bold text-slate-900">Поиск транзакции по UUID</h2>
              <p className="text-xs text-slate-500">
                Административный просмотр информации о любом чеке в системе
              </p>
            </div>

            <form onSubmit={handleSearchTx} className="flex gap-2 max-w-xl">
              <input
                type="text"
                placeholder="Введите UUID транзакции (напр. c74e954a-5633-4df4-8d99-...)"
                value={searchTxId}
                onChange={(e) => setSearchTxId(e.target.value)}
                className="flex-1 px-4 py-2.5 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-indigo-500 focus:outline-none"
              />
              <button
                type="submit"
                disabled={searchingTx}
                className="px-5 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-xs rounded-xl flex items-center space-x-2 transition"
              >
                <Search className="w-4 h-4" />
                <span>Найти</span>
              </button>
            </form>

            {searchTxError && (
              <div className="p-4 bg-rose-50 border border-rose-100 text-rose-800 text-xs rounded-xl flex items-center space-x-2">
                <AlertCircle className="w-4 h-4" />
                <span>{searchTxError}</span>
              </div>
            )}

            {searchedTx && (
              <div className="p-6 bg-slate-50 border border-slate-200 rounded-2xl space-y-4 text-xs">
                <div className="flex items-center justify-between pb-3 border-b border-slate-200">
                  <div className="font-mono font-bold text-slate-900">ID: {searchedTx.id}</div>
                  <span className="px-2 py-0.5 bg-indigo-100 text-indigo-800 font-semibold rounded-md">
                    {searchedTx.status}
                  </span>
                </div>

                <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
                  <div>
                    <span className="text-slate-400 block text-[10px]">Пользователь (User ID):</span>
                    <span className="font-mono text-slate-700">{searchedTx.userId}</span>
                  </div>
                  <div>
                    <span className="text-slate-400 block text-[10px]">Сумма покупки:</span>
                    <span className="font-bold text-slate-900">{searchedTx.amount} ₽</span>
                  </div>
                  <div>
                    <span className="text-slate-400 block text-[10px]">Дата создания:</span>
                    <span className="text-slate-700">{new Date(searchedTx.createdAt).toLocaleString('ru-RU')}</span>
                  </div>
                </div>

                <div>
                  <span className="font-semibold text-slate-700 block mb-2">Позиции ({searchedTx.items?.length || 0}):</span>
                  <div className="divide-y divide-slate-200 border border-slate-200 rounded-xl bg-white overflow-hidden">
                    {searchedTx.items?.map((it, i) => (
                      <div key={i} className="p-2.5 flex justify-between">
                        <div>
                          <span className="font-medium text-slate-800">{it.name}</span>
                          <span className="ml-2 text-[10px] text-slate-400 font-mono">({it.category})</span>
                        </div>
                        <span className="font-semibold text-slate-900">{it.price} ₽</span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Modal: Create Cashback Rule */}
        {showAddRuleModal && (
          <div className="fixed inset-0 bg-slate-900/40 backdrop-blur-xs flex items-center justify-center p-4 z-50">
            <div className="bg-white max-w-md w-full rounded-3xl p-6 shadow-2xl border border-slate-100 animate-in fade-in zoom-in-95 duration-150">
              <div className="flex items-center justify-between pb-4 border-b border-slate-100">
                <div className="flex items-center space-x-2">
                  <Percent className="w-5 h-5 text-indigo-600" />
                  <h3 className="font-bold text-slate-900 text-base">Новое правило кэшбэка</h3>
                </div>
                <button
                  onClick={() => setShowAddRuleModal(false)}
                  className="text-slate-400 hover:text-slate-600 p-1"
                >
                  ✕
                </button>
              </div>

              <form onSubmit={handleCreateRule} className="mt-4 space-y-4 text-xs">
                <div>
                  <label className="block font-semibold text-slate-700 mb-1">
                    Категория (category)
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="напр. groceries, electronics, cafe"
                    value={ruleCategory}
                    onChange={(e) => setRuleCategory(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                  />
                </div>

                <div>
                  <label className="block font-semibold text-slate-700 mb-1">
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
                      className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-indigo-500 focus:outline-none pr-8"
                    />
                    <span className="absolute right-3 top-2 text-slate-400 font-bold">%</span>
                  </div>
                </div>

                <div>
                  <label className="block font-semibold text-slate-700 mb-1">
                    Дата начала действия (validFrom)
                  </label>
                  <input
                    type="datetime-local"
                    required
                    value={ruleValidFrom}
                    onChange={(e) => setRuleValidFrom(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                  />
                </div>

                <div>
                  <label className="block font-semibold text-slate-700 mb-1">
                    Дата окончания действия (validTo)
                  </label>
                  <input
                    type="datetime-local"
                    required
                    value={ruleValidTo}
                    onChange={(e) => setRuleValidTo(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                  />
                </div>

                <div className="pt-4 flex space-x-3">
                  <button
                    type="button"
                    onClick={() => setShowAddRuleModal(false)}
                    className="w-1/2 py-2.5 bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold rounded-xl text-xs transition"
                  >
                    Отмена
                  </button>
                  <button
                    type="submit"
                    className="w-1/2 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-xl text-xs transition shadow-md shadow-indigo-200"
                  >
                    Создать
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
