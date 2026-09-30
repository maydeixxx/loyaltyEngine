import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { walletApi } from '../api/wallet';
import type { WalletTransactionDTO } from '../types';
import {
  Coins,
  RefreshCw,
  ArrowDownLeft,
  ArrowUpRight,
  Copy,
  Check,
  ShoppingBag,
  Percent,
  Wifi,
  ChevronRight,
  Sparkles,
  TrendingUp,
} from 'lucide-react';

export const WalletPage: React.FC = () => {
  const { user, userId } = useAuth();

  const [balance, setBalance] = useState<number | null>(null);
  const [walletHistory, setWalletHistory] = useState<WalletTransactionDTO[]>([]);
  const [loadingBalance, setLoadingBalance] = useState(false);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [copiedId, setCopiedId] = useState(false);

  const fetchBalance = useCallback(async () => {
    if (!userId) return;
    setLoadingBalance(true);
    try {
      const bal = await walletApi.getBalance(userId);
      setBalance(bal);
    } catch (err) {
      console.warn('Could not fetch balance', err);
    } finally {
      setLoadingBalance(false);
    }
  }, [userId]);

  const fetchHistory = useCallback(async () => {
    if (!userId) return;
    setLoadingHistory(true);
    try {
      const hist = await walletApi.getHistory(userId);
      setWalletHistory(hist);
    } catch (err) {
      console.warn('Could not fetch wallet history', err);
    } finally {
      setLoadingHistory(false);
    }
  }, [userId]);

  useEffect(() => {
    if (userId) {
      fetchBalance();
      fetchHistory();
    }
  }, [userId, fetchBalance, fetchHistory]);

  const copyUserId = () => {
    if (userId) {
      navigator.clipboard.writeText(userId);
      setCopiedId(true);
      setTimeout(() => setCopiedId(false), 2000);
    }
  };

  const totalEarned = walletHistory
    .filter((h) => h.type === 'CREDIT')
    .reduce((sum, h) => sum + Number(h.amount), 0);

  const totalSpent = walletHistory
    .filter((h) => h.type === 'DEBIT')
    .reduce((sum, h) => sum + Number(h.amount), 0);

  const cardholderName = user?.firstName
    ? `${user.firstName} ${user.lastName || ''}`.toUpperCase()
    : 'LOYALTY CLIENT';

  return (
    <div className="min-h-screen bg-[#0d0d0f] py-8 sm:py-12 px-4 sm:px-6 lg:px-8 pb-32 md:pb-16 max-w-4xl mx-auto space-y-8">
      {/* Wallet Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <span className="text-[11px] uppercase tracking-widest text-[#2997ff] font-semibold">
            Кошелек • LoyaltyEngine
          </span>
          <h1 className="text-2xl sm:text-3xl font-semibold text-white tracking-tight mt-0.5">
            Бонусный баланс
          </h1>
        </div>

        <div className="flex items-center space-x-2">
          <div className="flex items-center space-x-2 bg-white/[0.06] backdrop-blur-xl px-3.5 py-1.5 rounded-full border border-white/10 text-xs">
            <span className="text-white/40 font-medium">ID:</span>
            <code className="text-white font-mono text-[11px] font-semibold truncate max-w-[140px] sm:max-w-none">
              {userId ? `${userId.substring(0, 14)}...` : '—'}
            </code>
            <button
              onClick={copyUserId}
              title="Скопировать ID"
              className="p-1 text-white/50 hover:text-[#2997ff] transition"
            >
              {copiedId ? <Check className="w-3.5 h-3.5 text-[#30d158]" /> : <Copy className="w-3.5 h-3.5" />}
            </button>
          </div>

          <button
            onClick={() => {
              fetchBalance();
              fetchHistory();
            }}
            title="Обновить данные"
            className="p-2 bg-white/[0.06] hover:bg-white/[0.12] text-white/60 hover:text-white rounded-full border border-white/10 transition active:scale-95"
          >
            <RefreshCw className={`w-4 h-4 ${loadingBalance ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* THE ICONIC PHYSICAL APPLE CARD (Holographic Titanium Aesthetic) */}
      <div className="apple-card-iridescent rounded-3xl p-7 sm:p-9 text-white border border-white/15 relative overflow-hidden group">
        <div className="relative z-10 flex items-start justify-between">
          <div className="space-y-4">
            {/* Gold EMV Chip & Contactless Glyph */}
            <div className="flex items-center space-x-4">
              <div className="apple-emv-chip"></div>
              <Wifi className="w-5 h-5 text-white/60 rotate-90" />
            </div>

            <div className="pt-2">
              <span className="text-[11px] uppercase tracking-widest text-white/50 block font-medium">
                Текущие накопления
              </span>
              <div className="flex items-baseline space-x-3 mt-1">
                <span className="text-4xl sm:text-6xl font-bold tracking-tight text-white font-sans drop-shadow-md">
                  {balance !== null ? balance.toLocaleString('ru-RU', { minimumFractionDigits: 2 }) : '0.00'}
                </span>
                <span className="text-xl sm:text-2xl font-light text-white/60">баллов</span>
              </div>
            </div>
          </div>

          {/* Titanium Engraved Logo */}
          <div className="text-right">
            <div className="inline-flex items-center space-x-1.5 px-3 py-1 rounded-full bg-white/10 backdrop-blur-md border border-white/15 text-[11px] font-medium text-white/90">
              <Sparkles className="w-3 h-3 text-[#ff9f0a]" />
              <span>Loyalty Pass</span>
            </div>
            <div className="mt-2 text-[10px] tracking-widest uppercase text-white/40 font-mono">
              Premium
            </div>
          </div>
        </div>

        {/* Card Footer: Holder name & Rate */}
        <div className="relative z-10 mt-10 pt-5 border-t border-white/10 flex items-center justify-between text-xs">
          <div>
            <span className="text-[9px] uppercase tracking-widest text-white/40 block">Владелец карты</span>
            <span className="font-mono text-sm tracking-wider font-semibold text-white/90">
              {cardholderName}
            </span>
          </div>

          <div className="flex items-center space-x-2">
            <span className="w-2.5 h-2.5 bg-[#30d158] rounded-full animate-pulse shadow-[0_0_8px_#30d158]"></span>
            <span className="font-medium text-white/80">1 балл = 1 ₽</span>
          </div>
        </div>
      </div>

      {/* Apple Quick Action Pills */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
        <Link
          to="/checkout"
          className="p-4 rounded-3xl bg-white/[0.05] hover:bg-white/[0.09] border border-white/10 flex flex-col items-center justify-center space-y-2 group transition-all duration-200 active:scale-95"
        >
          <div className="w-10 h-10 rounded-2xl bg-[#0071e3]/20 text-[#2997ff] flex items-center justify-center group-hover:scale-110 transition-transform">
            <ShoppingBag className="w-5 h-5" />
          </div>
          <span className="text-xs font-medium text-white">В магазин</span>
        </Link>

        <Link
          to="/rules"
          className="p-4 rounded-3xl bg-white/[0.05] hover:bg-white/[0.09] border border-white/10 flex flex-col items-center justify-center space-y-2 group transition-all duration-200 active:scale-95"
        >
          <div className="w-10 h-10 rounded-2xl bg-[#ff9f0a]/20 text-[#ff9f0a] flex items-center justify-center group-hover:scale-110 transition-transform">
            <Percent className="w-5 h-5" />
          </div>
          <span className="text-xs font-medium text-white">Ставки кэшбэка</span>
        </Link>

        <div className="p-4 rounded-3xl bg-white/[0.05] border border-white/10 flex flex-col items-center justify-center space-y-2">
          <div className="w-10 h-10 rounded-2xl bg-[#30d158]/20 text-[#30d158] flex items-center justify-center">
            <TrendingUp className="w-5 h-5" />
          </div>
          <div className="text-center">
            <span className="text-[10px] text-white/50 block">Начислено</span>
            <span className="text-xs font-semibold text-[#30d158]">+{totalEarned.toFixed(2)} ₽</span>
          </div>
        </div>

        <div className="p-4 rounded-3xl bg-white/[0.05] border border-white/10 flex flex-col items-center justify-center space-y-2">
          <div className="w-10 h-10 rounded-2xl bg-[#ff453a]/20 text-[#ff453a] flex items-center justify-center">
            <ArrowUpRight className="w-5 h-5" />
          </div>
          <div className="text-center">
            <span className="text-[10px] text-white/50 block">Потрачено</span>
            <span className="text-xs font-semibold text-[#ff453a]">-{totalSpent.toFixed(2)} ₽</span>
          </div>
        </div>
      </div>

      {/* Apple Wallet Category Spending Breakdown Bar */}
      <div className="apple-card p-6 sm:p-7 space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h3 className="text-sm font-semibold text-white">Категории начислений</h3>
            <p className="text-[11px] text-white/50">Динамика бонусной активности по группам</p>
          </div>
          <span className="text-xs text-[#2997ff] font-medium">Активная программа</span>
        </div>

        {/* Multi-color category bar */}
        <div className="h-3 w-full rounded-full bg-white/10 overflow-hidden flex">
          <div className="h-full bg-[#ff9f0a] w-[45%]" title="Кафе: 45%" />
          <div className="h-full bg-[#0a84ff] w-[30%]" title="Электроника: 30%" />
          <div className="h-full bg-[#30d158] w-[25%]" title="Супермаркеты: 25%" />
        </div>

        <div className="flex flex-wrap items-center gap-4 text-xs pt-1">
          <div className="flex items-center space-x-1.5">
            <span className="w-2.5 h-2.5 rounded-full bg-[#ff9f0a]" />
            <span className="text-white/70">Кафе и рестораны (45%)</span>
          </div>
          <div className="flex items-center space-x-1.5">
            <span className="w-2.5 h-2.5 rounded-full bg-[#0a84ff]" />
            <span className="text-white/70">Электроника (30%)</span>
          </div>
          <div className="flex items-center space-x-1.5">
            <span className="w-2.5 h-2.5 rounded-full bg-[#30d158]" />
            <span className="text-white/70">Продукты (25%)</span>
          </div>
        </div>
      </div>

      {/* Transaction History (Apple Wallet Grouped Style) */}
      <div className="apple-card p-6 sm:p-8 space-y-5">
        <div className="flex items-center justify-between pb-4 border-b border-white/10">
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-white/10 rounded-2xl text-[#2997ff]">
              <Coins className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base sm:text-lg font-semibold text-white">
                Последние транзакции
              </h2>
              <p className="text-xs text-white/50">История начислений кэшбэка и списаний</p>
            </div>
          </div>

          <button
            onClick={fetchHistory}
            className="p-2 text-white/40 hover:text-white bg-white/5 hover:bg-white/10 rounded-full transition"
          >
            <RefreshCw className={`w-4 h-4 ${loadingHistory ? 'animate-spin' : ''}`} />
          </button>
        </div>

        <div className="divide-y divide-white/[0.06]">
          {walletHistory.length === 0 ? (
            <div className="py-12 text-center text-xs text-white/40 space-y-2">
              <p>Транзакций по кошельку пока нет.</p>
              <Link
                to="/checkout"
                className="inline-flex items-center space-x-1.5 text-[#2997ff] hover:underline font-medium"
              >
                <span>Оплатить товар и получить кэшбэк</span>
                <ChevronRight className="w-3.5 h-3.5" />
              </Link>
            </div>
          ) : (
            walletHistory.map((item) => (
              <div
                key={item.id}
                className="py-4 flex items-center justify-between gap-4 hover:bg-white/[0.02] px-2 -mx-2 rounded-2xl transition"
              >
                <div className="flex items-center space-x-3.5 min-w-0">
                  <div
                    className={`w-10 h-10 rounded-2xl flex items-center justify-center flex-shrink-0 ${
                      item.type === 'CREDIT'
                        ? 'bg-[#30d158]/15 text-[#30d158] border border-[#30d158]/20'
                        : 'bg-[#ff453a]/15 text-[#ff453a] border border-[#ff453a]/20'
                    }`}
                  >
                    {item.type === 'CREDIT' ? <ArrowDownLeft className="w-5 h-5" /> : <ArrowUpRight className="w-5 h-5" />}
                  </div>
                  <div className="min-w-0">
                    <div className="font-medium text-white text-xs sm:text-sm truncate">
                      {item.description || (item.type === 'CREDIT' ? 'Начисление кэшбэка' : 'Оплата баллами')}
                    </div>
                    <div className="text-[11px] text-white/40 mt-0.5">
                      {new Date(item.createdAt).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })} • {new Date(item.createdAt).toLocaleDateString('ru-RU')}
                    </div>
                  </div>
                </div>

                <div
                  className={`font-semibold text-sm sm:text-base flex-shrink-0 pl-2 font-mono ${
                    item.type === 'CREDIT' ? 'text-[#30d158]' : 'text-white/80'
                  }`}
                >
                  {item.type === 'CREDIT' ? '+' : '-'}
                  {Number(item.amount).toFixed(2)}
                  <span className="text-xs font-normal text-white/50 ml-1">₽</span>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
};
