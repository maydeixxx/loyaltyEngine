import React, { useState, useEffect, useCallback } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { rulesApi } from '../api/rules';
import type { CashbackRuleDTO } from '../types';
import {
  Sparkles,
  Search,
  Calendar,
  RefreshCw,
  ShoppingBag,
  ArrowRight,
  Shield,
  Coffee,
  ShoppingCart,
  Headphones,
  Shirt,
  Fuel,
  Pill,
  Tag,
  Calculator,
  Flame,
  AlertCircle,
  Plus,
} from 'lucide-react';

interface CategoryVisual {
  color: string;
  bgGlow: string;
  borderHover: string;
  badgeBg: string;
  icon: React.ReactNode;
}

const getCategoryVisual = (category: string): CategoryVisual => {
  const cat = category.toLowerCase().trim();
  if (cat.includes('cafe') || cat.includes('coffee') || cat.includes('ресторан') || cat.includes('кафе')) {
    return {
      color: '#ff9f0a',
      bgGlow: 'from-amber-500/15 to-transparent',
      borderHover: 'hover:border-amber-500/40',
      badgeBg: 'bg-[#ff9f0a]/15 text-[#ff9f0a] border border-[#ff9f0a]/30',
      icon: <Coffee className="w-6 h-6 text-[#ff9f0a]" />,
    };
  }
  if (cat.includes('groceries') || cat.includes('продукт') || cat.includes('супермаркет')) {
    return {
      color: '#30d158',
      bgGlow: 'from-emerald-500/15 to-transparent',
      borderHover: 'hover:border-emerald-500/40',
      badgeBg: 'bg-[#30d158]/15 text-[#30d158] border border-[#30d158]/30',
      icon: <ShoppingCart className="w-6 h-6 text-[#30d158]" />,
    };
  }
  if (cat.includes('electronics') || cat.includes('электроника')) {
    return {
      color: '#0a84ff',
      bgGlow: 'from-blue-500/15 to-transparent',
      borderHover: 'hover:border-blue-500/40',
      badgeBg: 'bg-[#0a84ff]/15 text-[#0a84ff] border border-[#0a84ff]/30',
      icon: <Headphones className="w-6 h-6 text-[#0a84ff]" />,
    };
  }
  if (cat.includes('apparel') || cat.includes('одежда')) {
    return {
      color: '#bf5af2',
      bgGlow: 'from-purple-500/15 to-transparent',
      borderHover: 'hover:border-purple-500/40',
      badgeBg: 'bg-[#bf5af2]/15 text-[#bf5af2] border border-[#bf5af2]/30',
      icon: <Shirt className="w-6 h-6 text-[#bf5af2]" />,
    };
  }
  if (cat.includes('auto') || cat.includes('fuel') || cat.includes('авто') || cat.includes('бензин')) {
    return {
      color: '#ff453a',
      bgGlow: 'from-rose-500/15 to-transparent',
      borderHover: 'hover:border-rose-500/40',
      badgeBg: 'bg-[#ff453a]/15 text-[#ff453a] border border-[#ff453a]/30',
      icon: <Fuel className="w-6 h-6 text-[#ff453a]" />,
    };
  }
  if (cat.includes('pharmacy') || cat.includes('аптека')) {
    return {
      color: '#5e5ce6',
      bgGlow: 'from-indigo-500/15 to-transparent',
      borderHover: 'hover:border-indigo-500/40',
      badgeBg: 'bg-[#5e5ce6]/15 text-[#5e5ce6] border border-[#5e5ce6]/30',
      icon: <Pill className="w-6 h-6 text-[#5e5ce6]" />,
    };
  }
  return {
    color: '#98989d',
    bgGlow: 'from-white/10 to-transparent',
    borderHover: 'hover:border-white/30',
    badgeBg: 'bg-white/10 text-white border border-white/20',
    icon: <Tag className="w-6 h-6 text-white/70" />,
  };
};

export const RulesPage: React.FC = () => {
  const { isAdmin } = useAuth();
  const [rules, setRules] = useState<CashbackRuleDTO[]>([]);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [searchTerm, setSearchTerm] = useState('');

  // Interactive Benefit Calculator
  const [calcCategory, setCalcCategory] = useState<string>('base');
  const [calcAmount, setCalcAmount] = useState<string>('1500');

  const loadRules = useCallback(async () => {
    setLoading(true);
    setLoadError(null);
    try {
      const data = await rulesApi.getAllRules();
      const loadedRules = Array.isArray(data) ? data : [];
      setRules(loadedRules);
      if (loadedRules.length > 0 && calcCategory === 'base') {
        setCalcCategory(loadedRules[0].category);
      }
    } catch (err: any) {
      console.error('Failed to load real rules', err);
      const msg = err.response?.data?.message || err.message || 'Ошибка загрузки правил из RuleEngineService';
      setLoadError(typeof msg === 'string' ? msg : 'Ошибка подключения к сервису правил');
      setRules([]); // Never substitute fake mock rules!
    } finally {
      setLoading(false);
    }
  }, [calcCategory]);

  useEffect(() => {
    loadRules();
  }, [loadRules]);

  const filteredRules = rules.filter((r) =>
    r.category.toLowerCase().includes(searchTerm.toLowerCase().trim())
  );

  // Calculate simulated cashback
  const selectedRule = rules.find((r) => r.category.toLowerCase() === calcCategory.toLowerCase());
  const activeRate = selectedRule ? Number(selectedRule.percentage) : 1.0;
  const parsedSpend = parseFloat(calcAmount) || 0;
  const calculatedBonus = (parsedSpend * activeRate) / 100;

  return (
    <div className="min-h-screen bg-[#0d0d0f] py-8 sm:py-12 px-4 sm:px-6 lg:px-8 pb-32 md:pb-16 max-w-5xl mx-auto space-y-8">
      {/* Rules Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <span className="text-[11px] uppercase tracking-widest text-[#ff9f0a] font-semibold flex items-center gap-1.5">
            <Flame className="w-3.5 h-3.5" />
            <span>Каталог привилегий</span>
          </span>
          <h1 className="text-2xl sm:text-3xl font-semibold text-white tracking-tight mt-0.5">
            Процентные ставки кэшбэка
          </h1>
        </div>

        <div className="flex items-center space-x-2">
          <button
            onClick={loadRules}
            className="p-2 bg-white/[0.06] hover:bg-white/[0.12] text-white/60 hover:text-white rounded-full border border-white/10 transition active:scale-95"
            title="Обновить правила"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
          </button>

          {isAdmin && (
            <Link
              to="/admin"
              className="px-4 py-2 bg-[#ff9f0a]/20 hover:bg-[#ff9f0a]/30 text-[#ff9f0a] rounded-full text-xs font-medium border border-[#ff9f0a]/30 flex items-center space-x-1.5 transition"
            >
              <Shield className="w-3.5 h-3.5" />
              <span>Админ-панель</span>
            </Link>
          )}
        </div>
      </div>

      {loadError && (
        <div className="p-4 rounded-2xl bg-[#ff453a]/15 border border-[#ff453a]/30 text-[#ff453a] text-xs flex items-center justify-between">
          <div className="flex items-center space-x-2.5">
            <AlertCircle className="w-4 h-4 flex-shrink-0" />
            <span>{loadError}</span>
          </div>
          <button
            onClick={loadRules}
            className="px-3 py-1 bg-white/10 hover:bg-white/20 text-white rounded-full text-[11px] font-medium transition"
          >
            Повторить
          </button>
        </div>
      )}

      {/* Hero Showcase: Guaranteed Base Cashback */}
      <div className="apple-card-iridescent rounded-3xl p-7 sm:p-9 text-white border border-white/15 relative overflow-hidden flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div className="space-y-2 relative z-10 max-w-xl">
          <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-white/10 backdrop-blur-md border border-white/10 text-xs font-medium text-white/90">
            <Sparkles className="w-3.5 h-3.5 text-[#ff9f0a]" />
            <span>Неограниченный базовый тариф</span>
          </div>
          <h2 className="text-3xl sm:text-4xl font-bold tracking-tight text-white font-sans">
            1.00% кэшбэка на абсолютно всё
          </h2>
          <p className="text-xs sm:text-sm text-white/60 leading-relaxed">
            Если на категорию или товар нет отдельного специального правила, система автоматически начислит вам гарантированный базовый кэшбэк 1.00% без скрытых условий и ограничений.
          </p>
        </div>

        <div className="relative z-10 flex-shrink-0">
          <Link
            to="/checkout"
            className="inline-flex items-center space-x-2 px-6 py-3 bg-white text-[#0d0d0f] hover:bg-white/90 active:scale-95 font-semibold text-xs rounded-full shadow-lg transition-all duration-200"
          >
            <ShoppingBag className="w-4 h-4 text-[#0071e3]" />
            <span>В кассу покупок</span>
            <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>
      </div>

      {/* Interactive Apple-style Cashback Benefit Simulator */}
      <div className="apple-card p-6 sm:p-8 space-y-5 border border-white/10">
        <div className="flex items-center justify-between pb-3 border-b border-white/10">
          <div className="flex items-center space-x-2.5">
            <div className="p-2 bg-[#2997ff]/15 text-[#2997ff] rounded-2xl">
              <Calculator className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-sm sm:text-base font-semibold text-white">Калькулятор выгоды кэшбэка</h3>
              <p className="text-[11px] text-white/50">Проверьте, сколько бонусов вернется за покупку</p>
            </div>
          </div>
          <span className="text-[11px] font-mono text-[#30d158] font-semibold bg-[#30d158]/15 px-2.5 py-1 rounded-full border border-[#30d158]/30">
            {activeRate.toFixed(1)}% ставка
          </span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-1">
          <div>
            <label className="block text-[11px] font-medium text-white/50 uppercase tracking-wider mb-1.5">
              Категория покупки
            </label>
            <select
              value={calcCategory}
              onChange={(e) => setCalcCategory(e.target.value)}
              className="w-full px-3.5 py-2.5 bg-[#16161a] border border-white/10 rounded-xl text-xs text-white focus:border-[#2997ff] focus:outline-none transition"
            >
              <option value="base">Базовый тариф (все категории) — 1.0%</option>
              {rules.map((r, i) => (
                <option key={i} value={r.category}>
                  {r.category} — {r.percentage}%
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-[11px] font-medium text-white/50 uppercase tracking-wider mb-1.5">
              Сумма покупки (₽)
            </label>
            <div className="relative">
              <input
                type="number"
                min="1"
                step="100"
                value={calcAmount}
                onChange={(e) => setCalcAmount(e.target.value)}
                className="w-full pl-3.5 pr-8 py-2.5 bg-white/[0.04] border border-white/10 rounded-xl text-xs text-white font-semibold focus:border-[#2997ff] focus:ring-1 focus:ring-[#2997ff]/40 focus:outline-none transition"
                placeholder="1500"
              />
              <span className="absolute right-3.5 top-2.5 text-xs text-white/40 font-medium">₽</span>
            </div>
          </div>
        </div>

        <div className="bg-white/[0.03] p-4 rounded-2xl border border-white/10 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-center space-x-2 text-xs text-white/60">
            <Sparkles className="w-4 h-4 text-[#ffd60a]" />
            <span>При покупке на {parsedSpend.toLocaleString('ru-RU')} ₽ вы получите:</span>
          </div>

          <div className="flex items-baseline space-x-1.5">
            <span className="text-2xl font-bold text-[#30d158] font-sans">
              +{calculatedBonus.toFixed(2)}
            </span>
            <span className="text-xs font-semibold text-white/50">бонусных баллов</span>
          </div>
        </div>
      </div>

      {/* Search & Categories Grid */}
      <div className="space-y-5">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <div className="text-base font-semibold text-white flex items-center space-x-2">
            <span>Категории с повышенным возвратом</span>
            <span className="text-xs bg-white/10 text-white/70 px-2.5 py-0.5 rounded-full font-mono">
              {filteredRules.length}
            </span>
          </div>

          {rules.length > 0 && (
            <div className="relative sm:w-64">
              <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-white/40">
                <Search className="w-3.5 h-3.5" />
              </div>
              <input
                type="text"
                placeholder="Поиск по категориям..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full pl-9 pr-3.5 py-2 bg-white/5 border border-white/10 rounded-full text-xs text-white focus:outline-none focus:border-[#2997ff] transition placeholder:text-white/30"
              />
            </div>
          )}
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {filteredRules.length === 0 ? (
            <div className="col-span-full apple-card p-10 text-center space-y-3">
              <div className="w-12 h-12 rounded-2xl bg-white/[0.04] border border-white/10 flex items-center justify-center mx-auto text-white/40">
                <Tag className="w-6 h-6" />
              </div>
              <div className="text-sm font-semibold text-white">
                {searchTerm ? 'Категорий по вашему запросу не найдено' : 'Категории повышенного кэшбэка пока не настроены'}
              </div>
              <p className="text-xs text-white/40 max-w-md mx-auto">
                {searchTerm
                  ? 'Попробуйте изменить поисковый запрос или сбросить фильтр.'
                  : 'В RuleEngineService пока нет действующих специальных правил. Ко всем покупкам автоматически применяется базовый тариф 1.00%.'}
              </p>
              {isAdmin && !searchTerm && (
                <div className="pt-2">
                  <Link
                    to="/admin"
                    className="inline-flex items-center space-x-1.5 px-4 py-2 bg-white hover:bg-white/90 text-black text-xs font-semibold rounded-full shadow-sm transition"
                  >
                    <Plus className="w-3.5 h-3.5" />
                    <span>Добавить правило в админ-панели</span>
                  </Link>
                </div>
              )}
            </div>
          ) : (
            filteredRules.map((rule, idx) => {
              const visual = getCategoryVisual(rule.category);
              return (
                <div
                  key={idx}
                  className={`apple-card apple-card-hover p-6 flex flex-col justify-between space-y-5 relative overflow-hidden bg-gradient-to-b ${visual.bgGlow} ${visual.borderHover} transition-all duration-300`}
                >
                  <div className="flex items-start justify-between relative z-10">
                    <div className="flex items-center space-x-3.5">
                      <div className="w-12 h-12 rounded-2xl bg-white/10 backdrop-blur-md flex items-center justify-center border border-white/10 shadow-sm">
                        {visual.icon}
                      </div>
                      <div>
                        <h3 className="font-semibold text-base text-white capitalize tracking-tight">
                          {rule.category}
                        </h3>
                        <span className="text-[11px] text-white/50 flex items-center space-x-1">
                          <span className="w-1.5 h-1.5 rounded-full bg-[#30d158]" />
                          <span>Активно</span>
                        </span>
                      </div>
                    </div>

                    <div className={`px-3 py-1 rounded-full text-sm font-bold font-sans ${visual.badgeBg}`}>
                      {rule.percentage}%
                    </div>
                  </div>

                  <div className="pt-4 border-t border-white/10 space-y-3 relative z-10">
                    <div className="text-[11px] text-white/50 flex items-center space-x-1.5">
                      <Calendar className="w-3.5 h-3.5 text-white/40" />
                      <span>
                        Действует до {rule.validTo ? new Date(rule.validTo).toLocaleDateString('ru-RU') : 'бессрочно'}
                      </span>
                    </div>

                    <Link
                      to="/checkout"
                      className="w-full py-2.5 bg-white/10 hover:bg-white/15 text-white font-medium text-xs rounded-full flex items-center justify-center space-x-1.5 transition active:scale-95 border border-white/10"
                    >
                      <ShoppingBag className="w-3.5 h-3.5 text-[#2997ff]" />
                      <span>Купить с {rule.percentage}% кэшбэком</span>
                    </Link>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>
    </div>
  );
};
