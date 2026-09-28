import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { walletApi } from '../api/wallet';
import { transactionApi } from '../api/transactions';
import type { WalletTransactionDTO, TransactionDTO, CreateTransactionItem } from '../types';
import {
  Wallet,
  Coins,
  RefreshCw,
  Plus,
  Trash2,
  CheckCircle2,
  AlertCircle,
  ShoppingBag,
  ArrowDownLeft,
  ArrowUpRight,
  Sparkles,
  Receipt,
  Copy,
  Check,
  Sliders,
  Calculator,
  Info,
  ChevronRight,
} from 'lucide-react';

interface CatalogProduct {
  id: string;
  name: string;
  category: string;
  defaultPrice: number;
  badge: string;
}

const PRODUCT_CATALOG: CatalogProduct[] = [
  { id: 'coffee', name: 'Капучино Grande', category: 'cafe', defaultPrice: 320, badge: '☕ Кафе' },
  { id: 'croissant', name: 'Французский круассан', category: 'cafe', defaultPrice: 180, badge: '🥐 Кафе' },
  { id: 'lunch', name: 'Бизнес-ланч', category: 'cafe', defaultPrice: 480, badge: '🥗 Кафе' },
  { id: 'groceries_basket', name: 'Корзина в супермаркете', category: 'groceries', defaultPrice: 1850, badge: '🛒 Продукты' },
  { id: 'milk', name: 'Фермерское молоко 1л', category: 'groceries', defaultPrice: 115, badge: '🥛 Продукты' },
  { id: 'meat', name: 'Стейк из говядины', category: 'groceries', defaultPrice: 790, badge: '🥩 Продукты' },
  { id: 'headphones', name: 'Беспроводные наушники ANC', category: 'electronics', defaultPrice: 4500, badge: '🎧 Электроника' },
  { id: 'smartphone', name: 'Смартфон 128GB', category: 'electronics', defaultPrice: 24990, badge: '📱 Электроника' },
  { id: 'powerbank', name: 'PowerBank 20000 mAh', category: 'electronics', defaultPrice: 1990, badge: '🔋 Электроника' },
  { id: 'sneakers', name: 'Кроссовки', category: 'apparel', defaultPrice: 5900, badge: '👟 Одежда' },
  { id: 'hoodie', name: 'Худи', category: 'apparel', defaultPrice: 3200, badge: '👕 Одежда' },
  { id: 'fuel', name: 'Бак АИ-95 (40 л)', category: 'auto', defaultPrice: 2400, badge: '⛽ Авто' },
  { id: 'pharmacy', name: 'Витамины и минералы', category: 'pharmacy', defaultPrice: 1150, badge: '💊 Аптека' },
];

export const DashboardPage: React.FC = () => {
  const { user, userId } = useAuth();

  // State for data
  const [balance, setBalance] = useState<number | null>(null);
  const [walletHistory, setWalletHistory] = useState<WalletTransactionDTO[]>([]);
  const [transactions, setTransactions] = useState<TransactionDTO[]>([]);
  const [loadingBalance, setLoadingBalance] = useState(false);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [loadingTxList, setLoadingTxList] = useState(false);
  const [copiedId, setCopiedId] = useState(false);

  // Cart / Items State
  const [cartItems, setCartItems] = useState<CreateTransactionItem[]>([
    { name: 'Капучино Grande', category: 'cafe', price: 320 },
  ]);

  // Dropdown & Form State for adding items
  const [selectedCatalogId, setSelectedCatalogId] = useState<string>('coffee');
  const [itemName, setItemName] = useState('Капучино Grande');
  const [itemCategory, setItemCategory] = useState('cafe');
  const [itemPrice, setItemPrice] = useState('320');

  // Transaction Amount State (customizable by user)
  const [customAmount, setCustomAmount] = useState<string>('320.00');
  const [isAutoAmount, setIsAutoAmount] = useState<boolean>(true);

  // Payment variables
  const [useCashback, setUseCashback] = useState(false);
  const [submittingTx, setSubmittingTx] = useState(false);
  const [txSuccess, setTxSuccess] = useState<string | null>(null);
  const [txError, setTxError] = useState<string | null>(null);

  // Mobile view tab for history (Points / Receipts)
  const [historyTab, setHistoryTab] = useState<'points' | 'receipts'>('points');

  // Selected Transaction for receipt modal
  const [selectedTx, setSelectedTx] = useState<TransactionDTO | null>(null);

  // Fetch balance
  const fetchBalance = useCallback(async () => {
    if (!userId) return;
    setLoadingBalance(true);
    try {
      const bal = await walletApi.getBalance(userId);
      setBalance(bal);
    } catch (err) {
      console.warn('Could not fetch balance yet', err);
    } finally {
      setLoadingBalance(false);
    }
  }, [userId]);

  // Fetch wallet history
  const fetchHistory = useCallback(async () => {
    if (!userId) return;
    setLoadingHistory(true);
    try {
      const hist = await walletApi.getHistory(userId);
      setWalletHistory(
        hist.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      );
    } catch (err) {
      console.warn('Could not fetch wallet history yet', err);
    } finally {
      setLoadingHistory(false);
    }
  }, [userId]);

  // Fetch transactions list
  const fetchTransactions = useCallback(async () => {
    if (!userId) return;
    setLoadingTxList(true);
    try {
      const txs = await transactionApi.getUserTransactions(userId);
      setTransactions(
        txs.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      );
    } catch (err) {
      console.warn('Could not fetch transactions yet', err);
    } finally {
      setLoadingTxList(false);
    }
  }, [userId]);

  // Initial load
  useEffect(() => {
    fetchBalance();
    fetchHistory();
    fetchTransactions();
  }, [fetchBalance, fetchHistory, fetchTransactions]);

  const copyUserId = () => {
    if (userId) {
      navigator.clipboard.writeText(userId);
      setCopiedId(true);
      setTimeout(() => setCopiedId(false), 2000);
    }
  };

  // When catalog product selection changes in dropdown
  const handleCatalogChange = (e: React.ChangeEvent<HTMLSelectElement>) => {
    const catId = e.target.value;
    setSelectedCatalogId(catId);

    if (catId === 'custom') {
      setItemName('');
      setItemCategory('general');
      setItemPrice('');
    } else {
      const found = PRODUCT_CATALOG.find((p) => p.id === catId);
      if (found) {
        setItemName(found.name);
        setItemCategory(found.category);
        setItemPrice(found.defaultPrice.toString());
      }
    }
  };

  // Add Item to Cart
  const handleAddItemToCart = (e: React.FormEvent) => {
    e.preventDefault();
    const priceNum = parseFloat(itemPrice);
    if (!itemName.trim() || isNaN(priceNum) || priceNum <= 0) {
      setTxError('Укажите корректное название и цену товара (> 0)');
      return;
    }
    setTxError(null);

    const newItem: CreateTransactionItem = {
      name: itemName.trim(),
      category: itemCategory.trim().toLowerCase(),
      price: priceNum,
    };

    const newCart = [...cartItems, newItem];
    setCartItems(newCart);

    if (isAutoAmount) {
      const newTotal = newCart.reduce((sum, it) => sum + it.price, 0);
      setCustomAmount(newTotal.toFixed(2));
    }
  };

  // Remove Item from Cart
  const removeItem = (index: number) => {
    const newCart = cartItems.filter((_, i) => i !== index);
    setCartItems(newCart);
    if (isAutoAmount) {
      const newTotal = newCart.reduce((sum, it) => sum + it.price, 0);
      setCustomAmount(newTotal.toFixed(2));
    }
  };

  // Update specific item price
  const handleUpdateItemPrice = (index: number, newPriceStr: string) => {
    const priceNum = parseFloat(newPriceStr);
    const newCart = cartItems.map((it, idx) =>
      idx === index ? { ...it, price: isNaN(priceNum) ? 0 : priceNum } : it
    );
    setCartItems(newCart);
    if (isAutoAmount) {
      const newTotal = newCart.reduce((sum, it) => sum + it.price, 0);
      setCustomAmount(newTotal.toFixed(2));
    }
  };

  // Total sum of all items in cart
  const totalCartAmount = cartItems.reduce((acc, item) => acc + item.price, 0);

  // Sync Amount with items
  const handleSyncAmountWithItems = () => {
    setIsAutoAmount(true);
    setCustomAmount(totalCartAmount.toFixed(2));
  };

  // Handle manual input of transaction amount
  const handleCustomAmountChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setIsAutoAmount(false);
    setCustomAmount(e.target.value);
  };

  const parsedCustomAmount = parseFloat(customAmount);
  const finalTransactionAmount = !isNaN(parsedCustomAmount) && parsedCustomAmount > 0 ? parsedCustomAmount : totalCartAmount;

  // Validation feedback
  const amountDifference = totalCartAmount - finalTransactionAmount;
  const isAmountGreaterThanItems = finalTransactionAmount > totalCartAmount;
  const isAmountLessThanItems = finalTransactionAmount < totalCartAmount;

  // Submit transaction
  const handleCreateTransaction = async () => {
    if (cartItems.length === 0) {
      setTxError('Добавьте хотя бы один товар в чек');
      return;
    }

    if (isNaN(parsedCustomAmount) || parsedCustomAmount < 0.01) {
      setTxError('Сумма транзакции должна быть не менее 0.01 ₽');
      return;
    }

    if (isAmountGreaterThanItems) {
      setTxError(
        `Сумма транзакции (${finalTransactionAmount.toFixed(2)} ₽) не может превышать стоимость товаров (${totalCartAmount.toFixed(2)} ₽).`
      );
      return;
    }

    if (isAmountLessThanItems && !useCashback) {
      setTxError(
        `Сумма (${finalTransactionAmount.toFixed(2)} ₽) меньше стоимости товаров (${totalCartAmount.toFixed(2)} ₽). Включите списание кэшбэка.`
      );
      return;
    }

    if (useCashback && balance !== null && amountDifference > balance) {
      setTxError(
        `Недостаточно баллов (${balance.toFixed(2)}) для покрытия разницы в ${amountDifference.toFixed(2)} ₽.`
      );
      return;
    }

    setTxError(null);
    setTxSuccess(null);
    setSubmittingTx(true);

    try {
      const createdTx = await transactionApi.createTransaction(
        {
          amount: finalTransactionAmount,
          items: cartItems,
          useCashbackBalance: useCashback,
        },
        userId || undefined
      );

      setTxSuccess(
        `Чек #${createdTx.id.substring(0, 8)} на сумму ${finalTransactionAmount.toFixed(2)} ₽ успешно создан!`
      );

      setTimeout(() => {
        fetchBalance();
        fetchHistory();
        fetchTransactions();
      }, 1500);
    } catch (err: any) {
      console.error('Failed to create transaction:', err);
      const msg = err.response?.data?.message || err.response?.data || 'Не удалось создать транзакцию';
      setTxError(typeof msg === 'string' ? msg : 'Ошибка при оформлении транзакции');
    } finally {
      setSubmittingTx(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 py-3 sm:py-6 px-3 sm:px-6 lg:px-8 pb-24 md:pb-12 max-w-7xl mx-auto overflow-x-hidden">
      <div className="space-y-4 sm:space-y-6">
        {/* Mobile-Friendly Greeting Card */}
        <div className="bg-white p-4 sm:p-6 rounded-2xl sm:rounded-3xl border border-slate-200/80 shadow-xs flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
          <div>
            <h1 className="text-xl sm:text-2xl font-extrabold text-slate-900 tracking-tight flex items-center gap-2">
              <span>Привет, {user?.firstName || 'друг'}!</span>
              <span className="text-lg">👋</span>
            </h1>
            <p className="text-xs text-slate-500 mt-0.5">
              Программа лояльности и кэшбэка LoyaltyEngine
            </p>
          </div>

          <div className="flex items-center justify-between sm:justify-end space-x-2 bg-slate-50 px-3 py-1.5 rounded-xl border border-slate-200 text-xs">
            <span className="text-slate-400 font-medium">ID:</span>
            <code className="text-slate-700 font-mono text-[11px] font-semibold truncate max-w-[150px] sm:max-w-none">
              {userId || '...'}
            </code>
            <button
              onClick={copyUserId}
              title="Скопировать User ID"
              className="p-1 hover:text-indigo-600 rounded transition flex-shrink-0"
            >
              {copiedId ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5 text-slate-400" />}
            </button>
          </div>
        </div>

        {/* Balance Card (Apple Wallet / Tinkoff card style) */}
        <div className="bg-gradient-to-br from-indigo-950 via-indigo-900 to-indigo-800 rounded-2xl sm:rounded-3xl p-5 sm:p-7 text-white shadow-lg shadow-indigo-950/15 relative overflow-hidden">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2 text-indigo-200 text-xs font-medium">
              <Wallet className="w-4 h-4" />
              <span>Баланс программы лояльности</span>
            </div>
            <button
              onClick={() => {
                fetchBalance();
                fetchHistory();
              }}
              title="Обновить баланс"
              className="p-1.5 hover:bg-white/10 rounded-lg text-indigo-200 hover:text-white transition active:scale-95"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${loadingBalance ? 'animate-spin' : ''}`} />
            </button>
          </div>

          <div className="mt-3 flex items-baseline space-x-2">
            <span className="text-3xl sm:text-5xl font-extrabold tracking-tight font-mono">
              {balance !== null ? balance.toLocaleString('ru-RU', { minimumFractionDigits: 2 }) : '0.00'}
            </span>
            <span className="text-lg sm:text-2xl font-semibold text-indigo-300">баллов</span>
          </div>

          <div className="mt-4 pt-4 border-t border-indigo-700/50 flex items-center justify-between text-[11px] text-indigo-200">
            <div className="flex items-center space-x-1.5">
              <span className="w-2 h-2 bg-emerald-400 rounded-full animate-pulse"></span>
              <span>1 балл = 1 ₽</span>
            </div>
            <div className="flex items-center space-x-1 bg-indigo-800/80 px-2 py-0.5 rounded-md">
              <Sparkles className="w-3 h-3 text-amber-300" />
              <span>Кэшбэк до 20%</span>
            </div>
          </div>
        </div>

        {/* TRANSACTION CONFIGURATOR */}
        <div className="bg-white rounded-2xl sm:rounded-3xl p-4 sm:p-6 border border-slate-200/80 shadow-xs space-y-4 sm:space-y-6">
          <div className="flex items-center space-x-3 pb-3 border-b border-slate-100">
            <div className="p-2 bg-indigo-50 text-indigo-600 rounded-xl flex-shrink-0">
              <Sliders className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base sm:text-lg font-bold text-slate-900">Конструктор покупки</h2>
              <p className="text-[11px] text-slate-500">
                Выбирайте товары из каталога и настраивайте чек
              </p>
            </div>
          </div>

          {txSuccess && (
            <div className="p-3.5 rounded-xl bg-emerald-50 border border-emerald-100 text-emerald-800 text-xs flex items-start space-x-2.5">
              <CheckCircle2 className="w-4 h-4 flex-shrink-0 text-emerald-600 mt-0.5" />
              <div>{txSuccess}</div>
            </div>
          )}

          {txError && (
            <div className="p-3.5 rounded-xl bg-rose-50 border border-rose-100 text-rose-800 text-xs flex items-start space-x-2.5">
              <AlertCircle className="w-4 h-4 flex-shrink-0 text-rose-600 mt-0.5" />
              <div>{txError}</div>
            </div>
          )}

          {/* STEP 1: Add Item */}
          <div className="bg-slate-50 p-3.5 sm:p-4 rounded-xl sm:rounded-2xl border border-slate-200/80 space-y-3">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-700 flex items-center space-x-1.5">
              <ShoppingBag className="w-3.5 h-3.5 text-indigo-600" />
              <span>1. Добавление товара в чек</span>
            </span>

            <form onSubmit={handleAddItemToCart} className="space-y-3">
              <div>
                <label className="block text-[11px] font-semibold text-slate-600 mb-1">
                  Товар из каталога:
                </label>
                <select
                  value={selectedCatalogId}
                  onChange={handleCatalogChange}
                  className="w-full px-3 py-2.5 bg-white border border-slate-200 rounded-xl text-xs font-medium text-slate-900 focus:ring-2 focus:ring-indigo-500 focus:outline-none transition"
                >
                  <optgroup label="☕ Кафе и рестораны">
                    <option value="coffee">Капучино Grande (320 ₽)</option>
                    <option value="croissant">Французский круассан (180 ₽)</option>
                    <option value="lunch">Бизнес-ланч (480 ₽)</option>
                  </optgroup>
                  <optgroup label="🛒 Продукты">
                    <option value="groceries_basket">Корзина супермаркета (1850 ₽)</option>
                    <option value="milk">Фермерское молоко 1л (115 ₽)</option>
                    <option value="meat">Стейк из говядины (790 ₽)</option>
                  </optgroup>
                  <optgroup label="🎧 Электроника">
                    <option value="headphones">Наушники ANC (4500 ₽)</option>
                    <option value="smartphone">Смартфон 128GB (24990 ₽)</option>
                    <option value="powerbank">PowerBank 20000 mAh (1990 ₽)</option>
                  </optgroup>
                  <optgroup label="👟 Одежда и авто">
                    <option value="sneakers">Кроссовки (5900 ₽)</option>
                    <option value="hoodie">Худи (3200 ₽)</option>
                    <option value="fuel">Бак АИ-95 (2400 ₽)</option>
                    <option value="pharmacy">Витамины (1150 ₽)</option>
                  </optgroup>
                  <option value="custom">➕ Свой товар (ввести вручную)...</option>
                </select>
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-2 sm:gap-3">
                <div className="sm:col-span-2">
                  <label className="block text-[10px] font-semibold text-slate-500 mb-0.5">Название:</label>
                  <input
                    type="text"
                    required
                    value={itemName}
                    onChange={(e) => setItemName(e.target.value)}
                    className="w-full px-3 py-2 bg-white border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                    placeholder="Название товара"
                  />
                </div>

                <div>
                  <label className="block text-[10px] font-semibold text-slate-500 mb-0.5">Категория:</label>
                  <select
                    value={itemCategory}
                    onChange={(e) => setItemCategory(e.target.value)}
                    className="w-full px-3 py-2 bg-white border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                  >
                    <option value="cafe">cafe</option>
                    <option value="groceries">groceries</option>
                    <option value="electronics">electronics</option>
                    <option value="apparel">apparel</option>
                    <option value="auto">auto</option>
                    <option value="pharmacy">pharmacy</option>
                    <option value="general">general</option>
                  </select>
                </div>
              </div>

              <div className="flex items-center space-x-2 pt-1">
                <div className="relative flex-1 sm:w-40 sm:flex-none">
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    required
                    value={itemPrice}
                    onChange={(e) => setItemPrice(e.target.value)}
                    className="w-full pl-3 pr-7 py-2 bg-white border border-slate-200 rounded-xl text-xs font-semibold focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                    placeholder="Цена"
                  />
                  <span className="absolute right-2.5 top-2 text-xs text-slate-400 font-bold">₽</span>
                </div>

                <button
                  type="submit"
                  className="flex-1 sm:flex-none px-4 py-2 bg-indigo-600 hover:bg-indigo-700 active:scale-95 text-white text-xs font-semibold rounded-xl flex items-center justify-center space-x-1.5 transition shadow-xs"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>В чек</span>
                </button>
              </div>
            </form>
          </div>

          {/* STEP 2: Items in Cart (Mobile-friendly list) */}
          <div className="border border-slate-200 rounded-xl sm:rounded-2xl overflow-hidden shadow-xs">
            <div className="bg-slate-100/80 px-3 sm:px-4 py-2 text-xs font-semibold text-slate-600 flex justify-between items-center">
              <span>Товары в чеке ({cartItems.length}):</span>
              <span className="text-[11px] text-slate-400">Цену можно изменить</span>
            </div>

            <div className="divide-y divide-slate-100">
              {cartItems.length === 0 ? (
                <div className="p-6 text-center text-xs text-slate-400">
                  Чек пуст. Добавьте товар из списка выше.
                </div>
              ) : (
                cartItems.map((item, idx) => (
                  <div key={idx} className="p-3 sm:px-4 sm:py-3 hover:bg-slate-50/70 transition flex items-center justify-between gap-3">
                    <div className="flex-1 min-w-0">
                      <div className="font-semibold text-slate-900 text-xs truncate">
                        {item.name}
                      </div>
                      <div className="flex items-center space-x-1.5 mt-0.5">
                        <span className="font-mono text-[9px] bg-slate-100 text-slate-600 px-1.5 py-0.2 rounded">
                          {item.category}
                        </span>
                      </div>
                    </div>

                    <div className="flex items-center space-x-2 flex-shrink-0">
                      <div className="relative">
                        <input
                          type="number"
                          step="0.01"
                          min="0.01"
                          value={item.price}
                          onChange={(e) => handleUpdateItemPrice(idx, e.target.value)}
                          className="w-20 sm:w-24 px-2 py-1 bg-slate-50 border border-slate-200 rounded-lg text-right font-bold text-xs text-slate-900 focus:bg-white focus:outline-none"
                        />
                      </div>
                      <span className="text-xs font-semibold text-slate-500">₽</span>

                      <button
                        onClick={() => removeItem(idx)}
                        title="Удалить"
                        className="p-1.5 text-slate-300 hover:text-rose-600 transition"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>

            {cartItems.length > 0 && (
              <div className="bg-slate-50 px-3 sm:px-4 py-2.5 border-t border-slate-200 flex justify-between items-center text-xs">
                <span className="font-medium text-slate-600">Сумма товаров:</span>
                <span className="font-bold text-slate-900 text-sm font-mono">{totalCartAmount.toFixed(2)} ₽</span>
              </div>
            )}
          </div>

          {/* STEP 3: Customize Transaction Amount & Payment */}
          {cartItems.length > 0 && (
            <div className="bg-indigo-50/40 p-4 sm:p-5 rounded-xl sm:rounded-2xl border border-indigo-100/80 space-y-4">
              <div className="flex items-center justify-between pb-2 border-b border-indigo-100">
                <span className="text-xs font-bold uppercase tracking-wider text-indigo-950 flex items-center space-x-1.5">
                  <Calculator className="w-3.5 h-3.5 text-indigo-600" />
                  <span>2. Сумма к списанию</span>
                </span>
                <button
                  type="button"
                  onClick={handleSyncAmountWithItems}
                  className="text-[10px] font-semibold text-indigo-600 hover:text-indigo-800 flex items-center space-x-1 transition"
                >
                  <RefreshCw className="w-2.5 h-2.5" />
                  <span>Рассчитать по товарам</span>
                </button>
              </div>

              <div>
                <label className="block text-[11px] font-bold text-slate-800 mb-1">
                  Сумма транзакции к оплате (₽):
                </label>
                <div className="relative">
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    value={customAmount}
                    onChange={handleCustomAmountChange}
                    className="w-full pl-3.5 pr-8 py-2.5 bg-white border-2 border-indigo-200 focus:border-indigo-600 rounded-xl text-base sm:text-lg font-bold text-slate-900 focus:outline-none transition font-mono"
                    placeholder="0.00"
                  />
                  <span className="absolute right-3 top-2.5 text-slate-400 font-bold">₽</span>
                </div>

                <div className="flex items-center justify-between text-[10px] text-slate-500 mt-1">
                  <span>
                    {isAutoAmount ? (
                      <span className="text-emerald-600 font-medium">● По сумме товаров</span>
                    ) : (
                      <span className="text-amber-600 font-medium">● Ручной ввод</span>
                    )}
                  </span>
                  <span>Товары: {totalCartAmount.toFixed(2)} ₽</span>
                </div>
              </div>

              {/* Cashback toggle */}
              <div className="bg-white p-3 rounded-xl border border-slate-200 space-y-2">
                <label className="flex items-start space-x-2.5 cursor-pointer select-none">
                  <input
                    type="checkbox"
                    checked={useCashback}
                    onChange={(e) => setUseCashback(e.target.checked)}
                    className="w-4 h-4 mt-0.5 text-indigo-600 rounded border-slate-300 focus:ring-indigo-500"
                  />
                  <div>
                    <span className="text-xs font-bold text-slate-900 block leading-tight">
                      Оплатить баллами кэшбэка
                    </span>
                    <span className="text-[10px] text-slate-500 block mt-0.5">
                      Списать баллы для покрытия разницы
                    </span>
                  </div>
                </label>

                {balance !== null && (
                  <div className="pt-2 border-t border-slate-100 flex justify-between items-center text-[10px]">
                    <span className="text-slate-500">Доступный баланс:</span>
                    <span className="font-bold text-indigo-600 font-mono">{balance.toFixed(2)} б.</span>
                  </div>
                )}
              </div>

              {/* Warnings / Calculations */}
              <div className="text-xs space-y-1.5 p-3 rounded-xl bg-white/80 border border-slate-200">
                <div className="flex justify-between text-slate-600">
                  <span>К оплате деньгами:</span>
                  <span className="font-bold text-indigo-600 font-mono">{finalTransactionAmount.toFixed(2)} ₽</span>
                </div>

                {useCashback && isAmountLessThanItems && (
                  <div className="flex justify-between text-emerald-700 bg-emerald-50 px-2 py-1 rounded">
                    <span>Списание баллов:</span>
                    <span className="font-bold font-mono">-{amountDifference.toFixed(2)} б.</span>
                  </div>
                )}

                {isAmountGreaterThanItems && (
                  <div className="flex items-center space-x-1.5 text-rose-700 text-[10px]">
                    <AlertCircle className="w-3.5 h-3.5 flex-shrink-0" />
                    <span>Сумма транзакции не может быть больше суммы товаров.</span>
                  </div>
                )}

                {isAmountLessThanItems && !useCashback && (
                  <div className="flex items-center space-x-1.5 text-amber-700 text-[10px]">
                    <Info className="w-3.5 h-3.5 flex-shrink-0" />
                    <span>Включите списание кэшбэка выше, чтобы покрыть разницу.</span>
                  </div>
                )}
              </div>

              {/* Big CTA button */}
              <button
                type="button"
                onClick={handleCreateTransaction}
                disabled={submittingTx || isAmountGreaterThanItems || (isAmountLessThanItems && !useCashback)}
                className="w-full py-3 bg-indigo-600 hover:bg-indigo-700 active:scale-98 text-white font-bold text-xs sm:text-sm rounded-xl transition shadow-md shadow-indigo-200 disabled:opacity-50 flex items-center justify-center space-x-2"
              >
                {submittingTx ? (
                  <RefreshCw className="w-4 h-4 animate-spin" />
                ) : (
                  <>
                    <Receipt className="w-4 h-4" />
                    <span>Оплатить {finalTransactionAmount.toFixed(2)} ₽</span>
                  </>
                )}
              </button>
            </div>
          )}
        </div>

        {/* Mobile Tabs for History */}
        <div className="md:hidden flex bg-slate-200/70 p-1 rounded-xl">
          <button
            onClick={() => setHistoryTab('points')}
            className={`flex-1 py-1.5 text-xs font-semibold rounded-lg transition ${
              historyTab === 'points' ? 'bg-white text-slate-900 shadow-xs' : 'text-slate-600'
            }`}
          >
            Баллы кошелька
          </button>
          <button
            onClick={() => setHistoryTab('receipts')}
            className={`flex-1 py-1.5 text-xs font-semibold rounded-lg transition ${
              historyTab === 'receipts' ? 'bg-white text-slate-900 shadow-xs' : 'text-slate-600'
            }`}
          >
            Чеки покупок
          </button>
        </div>

        {/* History Tables Section */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 sm:gap-6">
          {/* Wallet Points History */}
          <div className={`${historyTab === 'points' ? 'block' : 'hidden md:block'} bg-white rounded-2xl sm:rounded-3xl p-4 sm:p-6 border border-slate-200/80 shadow-xs`}>
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center space-x-2">
                <div className="p-1.5 bg-indigo-50 text-indigo-600 rounded-lg">
                  <Coins className="w-4 h-4" />
                </div>
                <h3 className="font-bold text-slate-900 text-sm sm:text-base">Баллы кошелька</h3>
              </div>
              <button onClick={fetchHistory} className="p-1 text-slate-400 hover:text-slate-600">
                <RefreshCw className={`w-3.5 h-3.5 ${loadingHistory ? 'animate-spin' : ''}`} />
              </button>
            </div>

            <div className="divide-y divide-slate-100 max-h-80 overflow-y-auto">
              {walletHistory.length === 0 ? (
                <div className="py-8 text-center text-xs text-slate-400">Операций пока нет</div>
              ) : (
                walletHistory.map((item) => (
                  <div key={item.id} className="py-2.5 flex items-center justify-between text-xs">
                    <div className="flex items-center space-x-2.5 min-w-0">
                      <div
                        className={`w-7 h-7 rounded-lg flex items-center justify-center flex-shrink-0 ${
                          item.type === 'CREDIT' ? 'bg-emerald-50 text-emerald-600' : 'bg-rose-50 text-rose-600'
                        }`}
                      >
                        {item.type === 'CREDIT' ? <ArrowDownLeft className="w-3.5 h-3.5" /> : <ArrowUpRight className="w-3.5 h-3.5" />}
                      </div>
                      <div className="min-w-0">
                        <div className="font-medium text-slate-800 truncate text-[11px] sm:text-xs">
                          {item.description || (item.type === 'CREDIT' ? 'Кэшбэк' : 'Списание')}
                        </div>
                        <div className="text-[9px] text-slate-400">
                          {new Date(item.createdAt).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })} • {new Date(item.createdAt).toLocaleDateString('ru-RU')}
                        </div>
                      </div>
                    </div>

                    <div
                      className={`font-bold font-mono text-xs sm:text-sm flex-shrink-0 pl-2 ${
                        item.type === 'CREDIT' ? 'text-emerald-600' : 'text-rose-600'
                      }`}
                    >
                      {item.type === 'CREDIT' ? '+' : '-'}
                      {Number(item.amount).toFixed(2)}
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* Purchases / Receipts History */}
          <div className={`${historyTab === 'receipts' ? 'block' : 'hidden md:block'} bg-white rounded-2xl sm:rounded-3xl p-4 sm:p-6 border border-slate-200/80 shadow-xs`}>
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center space-x-2">
                <div className="p-1.5 bg-indigo-50 text-indigo-600 rounded-lg">
                  <Receipt className="w-4 h-4" />
                </div>
                <h3 className="font-bold text-slate-900 text-sm sm:text-base">Чеки покупок</h3>
              </div>
              <button onClick={fetchTransactions} className="p-1 text-slate-400 hover:text-slate-600">
                <RefreshCw className={`w-3.5 h-3.5 ${loadingTxList ? 'animate-spin' : ''}`} />
              </button>
            </div>

            <div className="divide-y divide-slate-100 max-h-80 overflow-y-auto">
              {transactions.length === 0 ? (
                <div className="py-8 text-center text-xs text-slate-400">Чеков пока нет</div>
              ) : (
                transactions.map((tx) => (
                  <div
                    key={tx.id}
                    onClick={() => setSelectedTx(tx)}
                    className="py-2.5 px-2 -mx-2 rounded-xl hover:bg-slate-50 cursor-pointer flex items-center justify-between text-xs transition"
                  >
                    <div className="flex items-center space-x-2.5 min-w-0">
                      <div className="w-7 h-7 rounded-lg bg-slate-100 flex items-center justify-center text-slate-600 flex-shrink-0">
                        <Receipt className="w-3.5 h-3.5" />
                      </div>
                      <div className="min-w-0">
                        <div className="font-medium text-slate-800 text-[11px] sm:text-xs truncate">
                          Чек #{tx.id.substring(0, 8)} ({tx.items?.length || 0} поз.)
                        </div>
                        <div className="text-[9px] text-slate-400">
                          {new Date(tx.createdAt).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })} • {new Date(tx.createdAt).toLocaleDateString('ru-RU')}
                        </div>
                      </div>
                    </div>

                    <div className="text-right flex items-center space-x-1.5 flex-shrink-0">
                      <div>
                        <div className="font-bold text-slate-900 text-xs font-mono">{Number(tx.amount).toFixed(2)} ₽</div>
                        <span
                          className={`inline-block px-1.5 py-0.2 rounded text-[9px] font-semibold ${
                            tx.status === 'HANDLED'
                              ? 'bg-emerald-50 text-emerald-700'
                              : tx.status === 'FAILED'
                              ? 'bg-rose-50 text-rose-700'
                              : 'bg-amber-50 text-amber-700'
                          }`}
                        >
                          {tx.status}
                        </span>
                      </div>
                      <ChevronRight className="w-3.5 h-3.5 text-slate-400" />
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>

        {/* Receipt Modal */}
        {selectedTx && (
          <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-3 z-50">
            <div className="bg-white max-w-sm w-full rounded-2xl p-5 shadow-xl border border-slate-100">
              <div className="flex items-center justify-between pb-3 border-b border-slate-100">
                <div className="flex items-center space-x-2">
                  <Receipt className="w-4 h-4 text-indigo-600" />
                  <h3 className="font-bold text-slate-900 text-sm">Детали чека</h3>
                </div>
                <button
                  onClick={() => setSelectedTx(null)}
                  className="text-slate-400 hover:text-slate-600 p-1 text-sm font-bold"
                >
                  ✕
                </button>
              </div>

              <div className="mt-3 space-y-3 text-xs">
                <div className="bg-slate-50 p-2.5 rounded-xl space-y-1 font-mono text-[10px]">
                  <div className="flex justify-between text-slate-500">
                    <span>ID:</span>
                    <span className="text-slate-800 font-semibold">{selectedTx.id.substring(0, 16)}...</span>
                  </div>
                  <div className="flex justify-between text-slate-500">
                    <span>Статус:</span>
                    <span className="font-semibold text-indigo-600">{selectedTx.status}</span>
                  </div>
                </div>

                <div>
                  <span className="font-semibold text-slate-700 block mb-1 text-[11px]">Позиции:</span>
                  <div className="divide-y divide-slate-100 border border-slate-100 rounded-xl overflow-hidden max-h-40 overflow-y-auto">
                    {selectedTx.items?.map((item, idx) => (
                      <div key={idx} className="p-2 flex justify-between items-center text-[11px]">
                        <div>
                          <div className="font-medium text-slate-800">{item.name}</div>
                          <div className="text-[9px] text-slate-400 font-mono">{item.category}</div>
                        </div>
                        <div className="font-semibold text-slate-900">{Number(item.price).toFixed(2)} ₽</div>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="pt-2 border-t border-slate-100 flex justify-between items-baseline font-bold text-slate-900">
                  <span>Итого к оплате:</span>
                  <span className="text-sm text-indigo-600 font-mono">{Number(selectedTx.amount).toFixed(2)} ₽</span>
                </div>
              </div>

              <div className="mt-4">
                <button
                  onClick={() => setSelectedTx(null)}
                  className="w-full py-2 bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold rounded-xl text-xs transition"
                >
                  Закрыть
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
