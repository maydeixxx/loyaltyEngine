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
  RotateCcw,
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

  // Cancellation state
  const [cancellingTxId, setCancellingTxId] = useState<string | null>(null);
  const [cancelSuccessMsg, setCancelSuccessMsg] = useState<string | null>(null);
  const [cancelErrorMsg, setCancelErrorMsg] = useState<string | null>(null);

  // Open modal and reset msgs
  const openTxModal = (tx: TransactionDTO) => {
    setSelectedTx(tx);
    setCancelSuccessMsg(null);
    setCancelErrorMsg(null);
  };

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

  // Fetch points history
  const fetchHistory = useCallback(async () => {
    if (!userId) return;
    setLoadingHistory(true);
    try {
      const hist = await walletApi.getHistory(userId);
      setWalletHistory(hist);
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
      const txList = await transactionApi.getUserTransactions(userId);
      setTransactions(txList);
    } catch (err) {
      console.warn('Could not fetch transactions yet', err);
    } finally {
      setLoadingTxList(false);
    }
  }, [userId]);

  useEffect(() => {
    if (userId) {
      fetchBalance();
      fetchHistory();
      fetchTransactions();
    }
  }, [userId, fetchBalance, fetchHistory, fetchTransactions]);

  // Copy User ID
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
      setTxError('Сумма транзакции к списанию не может превышать суммарную стоимость товаров');
      return;
    }

    if (isAmountLessThanItems && !useCashback) {
      setTxError('Сумма оплаты меньше суммы товаров. Включите оплату баллами для покрытия разницы.');
      return;
    }

    if (useCashback && isAmountLessThanItems) {
      const neededPoints = amountDifference;
      if (balance !== null && balance < neededPoints) {
        setTxError(`Недостаточно баллов на балансе! Требуется: ${neededPoints.toFixed(2)} б., доступно: ${balance.toFixed(2)} б.`);
        return;
      }
    }

    setSubmittingTx(true);
    setTxError(null);
    setTxSuccess(null);

    try {
      const response = await transactionApi.createTransaction({
        amount: finalTransactionAmount,
        items: cartItems,
        useCashbackBalance: useCashback,
      });

      setTxSuccess(
        `Транзакция #${response.id ? response.id.substring(0, 8) : 'создана'} на сумму ${finalTransactionAmount.toFixed(2)} ₽ успешно проведена!`
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

  // Handle Cancel / Refund of transaction
  const handleCancelTransaction = async (txId: string) => {
    if (!userId) return;
    if (!window.confirm('Оформить возврат по чеку? Начисленный кэшбэк будет аннулирован, а списанные баллы возвращены на счет.')) {
      return;
    }

    setCancellingTxId(txId);
    setCancelErrorMsg(null);
    setCancelSuccessMsg(null);

    try {
      await transactionApi.cancelTransaction(txId, userId);
      setCancelSuccessMsg('Возврат успешно оформлен!');
      if (selectedTx && selectedTx.id === txId) {
        setSelectedTx({ ...selectedTx, status: 'CANCELLED' });
      }
      setTimeout(() => {
        fetchBalance();
        fetchHistory();
        fetchTransactions();
      }, 1000);
    } catch (err: any) {
      console.error('Failed to cancel transaction:', err);
      const msg = err.response?.data?.message || err.message || 'Ошибка при оформлении возврата';
      setCancelErrorMsg(typeof msg === 'string' ? msg : 'Не удалось отменить транзакцию');
    } finally {
      setCancellingTxId(null);
    }
  };

  return (
    <div className="min-h-screen bg-[#f5f5f7] py-4 sm:py-8 px-4 sm:px-6 lg:px-8 pb-28 md:pb-14 max-w-7xl mx-auto overflow-x-hidden">
      <div className="space-y-6">
        {/* Apple-style Greeting Card */}
        <div className="bg-white p-5 sm:p-7 rounded-3xl border border-black/[0.06] shadow-[0_4px_24px_rgba(0,0,0,0.03)] flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-xl sm:text-2xl font-semibold text-[#1d1d1f] tracking-tight flex items-center gap-2">
              <span>Привет, {user?.firstName || 'друг'}</span>
              <span className="text-xl">👋</span>
            </h1>
            <p className="text-xs text-[#86868b] mt-0.5">
              Программа лояльности и привилегий LoyaltyEngine
            </p>
          </div>

          <div className="flex items-center justify-between sm:justify-end space-x-2 bg-[#f5f5f7] px-3.5 py-1.5 rounded-full border border-black/[0.04] text-xs">
            <span className="text-[#86868b] font-medium">ID:</span>
            <code className="text-[#1d1d1f] font-mono text-[11px] font-semibold truncate max-w-[150px] sm:max-w-none">
              {userId || '...'}
            </code>
            <button
              onClick={copyUserId}
              title="Скопировать ID"
              className="p-1 text-[#86868b] hover:text-[#0071e3] transition flex-shrink-0"
            >
              {copiedId ? <Check className="w-3.5 h-3.5 text-[#34c759]" /> : <Copy className="w-3.5 h-3.5" />}
            </button>
          </div>
        </div>

        {/* Balance Card: Apple Card / Apple Wallet Aesthetic */}
        <div className="bg-gradient-to-br from-[#1d1d1f] via-[#151516] to-[#0a0a0c] rounded-3xl p-6 sm:p-8 text-white border border-white/[0.08] shadow-[0_16px_40px_rgba(0,0,0,0.18)] relative overflow-hidden">
          {/* Subtle Titanium & Iridescent Glow */}
          <div className="absolute -top-24 -right-24 w-80 h-80 bg-gradient-to-br from-blue-500/20 via-purple-500/15 to-transparent rounded-full blur-3xl pointer-events-none" />
          <div className="absolute -bottom-20 -left-20 w-60 h-60 bg-gradient-to-tr from-amber-500/10 to-transparent rounded-full blur-2xl pointer-events-none" />

          <div className="relative z-10 flex items-center justify-between">
            <div className="flex items-center space-x-2 text-white/60 text-xs font-medium">
              <Wallet className="w-4 h-4 text-white/80" />
              <span>Баланс привилегий</span>
            </div>
            <button
              onClick={() => {
                fetchBalance();
                fetchHistory();
              }}
              title="Обновить баланс"
              className="p-1.5 hover:bg-white/10 rounded-full text-white/60 hover:text-white transition-all active:scale-95"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${loadingBalance ? 'animate-spin' : ''}`} />
            </button>
          </div>

          <div className="relative z-10 mt-5 flex items-baseline space-x-3">
            <span className="text-4xl sm:text-6xl font-semibold tracking-tight text-white">
              {balance !== null ? balance.toLocaleString('ru-RU', { minimumFractionDigits: 2 }) : '0.00'}
            </span>
            <span className="text-lg sm:text-2xl font-normal text-white/50">баллов</span>
          </div>

          <div className="relative z-10 mt-6 pt-5 border-t border-white/[0.08] flex items-center justify-between text-xs text-white/70">
            <div className="flex items-center space-x-2">
              <span className="w-2 h-2 bg-[#34c759] rounded-full animate-pulse shadow-sm shadow-emerald-400"></span>
              <span>1 балл = 1 ₽</span>
            </div>
            <div className="flex items-center space-x-1.5 bg-white/10 backdrop-blur-md px-3 py-1 rounded-full text-white/90 text-[11px] font-medium border border-white/[0.06]">
              <Sparkles className="w-3 h-3 text-[#ff9500]" />
              <span>Кэшбэк до 20%</span>
            </div>
          </div>
        </div>

        {/* TRANSACTION CONFIGURATOR */}
        <div className="bg-white rounded-3xl p-5 sm:p-7 border border-black/[0.06] shadow-[0_4px_24px_rgba(0,0,0,0.03)] space-y-6">
          <div className="flex items-center space-x-3 pb-4 border-b border-black/[0.06]">
            <div className="p-2 bg-[#f5f5f7] text-[#0071e3] rounded-2xl flex-shrink-0">
              <Sliders className="w-5 h-5" />
            </div>
            <div>
              <h2 className="text-base sm:text-lg font-semibold text-[#1d1d1f] tracking-tight">Конструктор покупки</h2>
              <p className="text-xs text-[#86868b]">
                Выбирайте товары из каталога и настраивайте позиции чека
              </p>
            </div>
          </div>

          {txSuccess && (
            <div className="p-3.5 rounded-2xl bg-[#34c759]/10 border border-[#34c759]/20 text-[#34c759] text-xs flex items-start space-x-2.5">
              <CheckCircle2 className="w-4 h-4 flex-shrink-0 mt-0.5" />
              <div className="font-medium">{txSuccess}</div>
            </div>
          )}

          {txError && (
            <div className="p-3.5 rounded-2xl bg-[#ff3b30]/10 border border-[#ff3b30]/20 text-[#ff3b30] text-xs flex items-start space-x-2.5">
              <AlertCircle className="w-4 h-4 flex-shrink-0 mt-0.5" />
              <div className="font-medium">{txError}</div>
            </div>
          )}

          {/* STEP 1: Add Item */}
          <div className="bg-[#f5f5f7] p-4 sm:p-5 rounded-2xl border border-black/[0.04] space-y-3.5">
            <span className="text-xs font-semibold uppercase tracking-wider text-[#1d1d1f] flex items-center space-x-1.5">
              <ShoppingBag className="w-3.5 h-3.5 text-[#0071e3]" />
              <span>1. Добавление товара в чек</span>
            </span>

            <form onSubmit={handleAddItemToCart} className="space-y-3">
              <div>
                <label className="block text-[11px] font-medium text-[#86868b] mb-1">
                  Товар из каталога:
                </label>
                <select
                  value={selectedCatalogId}
                  onChange={handleCatalogChange}
                  className="w-full px-3.5 py-2.5 bg-white border border-black/[0.08] rounded-xl text-xs font-medium text-[#1d1d1f] focus:border-[#0071e3] focus:ring-4 focus:ring-[#0071e3]/15 focus:outline-none transition-all duration-200"
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

              <div className="grid grid-cols-1 sm:grid-cols-3 gap-2.5">
                <div className="sm:col-span-2">
                  <label className="block text-[10px] font-medium text-[#86868b] mb-1">Название:</label>
                  <input
                    type="text"
                    required
                    value={itemName}
                    onChange={(e) => setItemName(e.target.value)}
                    className="w-full px-3.5 py-2 bg-white border border-black/[0.08] rounded-xl text-xs text-[#1d1d1f] focus:border-[#0071e3] focus:ring-4 focus:ring-[#0071e3]/15 focus:outline-none transition-all duration-200"
                    placeholder="Название товара"
                  />
                </div>

                <div>
                  <label className="block text-[10px] font-medium text-[#86868b] mb-1">Категория:</label>
                  <select
                    value={itemCategory}
                    onChange={(e) => setItemCategory(e.target.value)}
                    className="w-full px-3.5 py-2 bg-white border border-black/[0.08] rounded-xl text-xs text-[#1d1d1f] focus:border-[#0071e3] focus:ring-4 focus:ring-[#0071e3]/15 focus:outline-none transition-all duration-200"
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
                <div className="relative flex-1 sm:w-44 sm:flex-none">
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    required
                    value={itemPrice}
                    onChange={(e) => setItemPrice(e.target.value)}
                    className="w-full pl-3.5 pr-7 py-2 bg-white border border-black/[0.08] rounded-xl text-xs font-semibold text-[#1d1d1f] focus:border-[#0071e3] focus:ring-4 focus:ring-[#0071e3]/15 focus:outline-none transition-all duration-200"
                    placeholder="Цена"
                  />
                  <span className="absolute right-3 top-2 text-xs text-[#86868b] font-medium">₽</span>
                </div>

                <button
                  type="submit"
                  className="flex-1 sm:flex-none px-5 py-2 bg-[#0071e3] hover:bg-[#0077ed] active:scale-95 text-white text-xs font-medium rounded-full flex items-center justify-center space-x-1.5 transition-all duration-200 shadow-sm"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>В чек</span>
                </button>
              </div>
            </form>
          </div>

          {/* STEP 2: Items in Cart (Grouped iOS List) */}
          <div className="border border-black/[0.06] rounded-2xl overflow-hidden bg-white shadow-xs">
            <div className="bg-[#f5f5f7] px-4 py-2.5 text-xs font-medium text-[#1d1d1f] flex justify-between items-center border-b border-black/[0.04]">
              <span>Товары в чеке ({cartItems.length}):</span>
              <span className="text-[11px] text-[#86868b]">Цену можно изменить</span>
            </div>

            <div className="divide-y divide-black/[0.04]">
              {cartItems.length === 0 ? (
                <div className="p-6 text-center text-xs text-[#86868b]">
                  Чек пуст. Добавьте товар из списка выше.
                </div>
              ) : (
                cartItems.map((item, idx) => (
                  <div key={idx} className="p-3 sm:px-4 sm:py-3 hover:bg-black/[0.01] transition flex items-center justify-between gap-3">
                    <div className="flex-1 min-w-0">
                      <div className="font-medium text-[#1d1d1f] text-xs truncate">
                        {item.name}
                      </div>
                      <div className="flex items-center space-x-1.5 mt-0.5">
                        <span className="font-mono text-[9px] bg-[#f5f5f7] text-[#86868b] px-2 py-0.5 rounded-full border border-black/[0.04]">
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
                          className="w-20 sm:w-24 px-2 py-1 bg-[#f5f5f7] border border-black/[0.08] rounded-lg text-right font-semibold text-xs text-[#1d1d1f] focus:bg-white focus:border-[#0071e3] focus:outline-none"
                        />
                      </div>
                      <span className="text-xs text-[#86868b] font-medium">₽</span>

                      <button
                        onClick={() => removeItem(idx)}
                        title="Удалить"
                        className="p-1.5 text-[#86868b] hover:text-[#ff3b30] transition-colors"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>

            {cartItems.length > 0 && (
              <div className="bg-[#f5f5f7] px-4 py-2.5 border-t border-black/[0.04] flex justify-between items-center text-xs">
                <span className="font-medium text-[#86868b]">Сумма товаров:</span>
                <span className="font-semibold text-[#1d1d1f] text-sm font-sans">{totalCartAmount.toFixed(2)} ₽</span>
              </div>
            )}
          </div>

          {/* STEP 3: Customize Transaction Amount & Payment */}
          {cartItems.length > 0 && (
            <div className="bg-[#f5f5f7] p-5 sm:p-6 rounded-2xl border border-black/[0.06] space-y-4">
              <div className="flex items-center justify-between pb-3 border-b border-black/[0.06]">
                <span className="text-xs font-semibold uppercase tracking-wider text-[#1d1d1f] flex items-center space-x-1.5">
                  <Calculator className="w-3.5 h-3.5 text-[#0071e3]" />
                  <span>2. Сумма к списанию</span>
                </span>
                <button
                  type="button"
                  onClick={handleSyncAmountWithItems}
                  className="text-[11px] font-medium text-[#0071e3] hover:underline flex items-center space-x-1 transition"
                >
                  <RefreshCw className="w-2.5 h-2.5" />
                  <span>Рассчитать по товарам</span>
                </button>
              </div>

              <div>
                <label className="block text-[11px] font-medium text-[#1d1d1f] mb-1.5">
                  Сумма транзакции к оплате (₽):
                </label>
                <div className="relative">
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    value={customAmount}
                    onChange={handleCustomAmountChange}
                    className="w-full pl-4 pr-9 py-3 bg-white border border-black/[0.12] focus:border-[#0071e3] focus:ring-4 focus:ring-[#0071e3]/15 rounded-2xl text-lg sm:text-xl font-semibold text-[#1d1d1f] focus:outline-none transition-all duration-200"
                    placeholder="0.00"
                  />
                  <span className="absolute right-3.5 top-3 text-[#86868b] font-medium text-base">₽</span>
                </div>

                <div className="flex items-center justify-between text-[11px] text-[#86868b] mt-1.5">
                  <span>
                    {isAutoAmount ? (
                      <span className="text-[#34c759] font-medium">● По сумме товаров</span>
                    ) : (
                      <span className="text-[#ff9500] font-medium">● Ручной ввод</span>
                    )}
                  </span>
                  <span>Товары: {totalCartAmount.toFixed(2)} ₽</span>
                </div>
              </div>

              {/* Cashback toggle */}
              <div className="bg-white p-3.5 rounded-2xl border border-black/[0.06] space-y-2">
                <label className="flex items-start space-x-3 cursor-pointer select-none">
                  <input
                    type="checkbox"
                    checked={useCashback}
                    onChange={(e) => setUseCashback(e.target.checked)}
                    className="w-4 h-4 mt-0.5 text-[#0071e3] rounded border-black/[0.15] focus:ring-[#0071e3]"
                  />
                  <div>
                    <span className="text-xs font-semibold text-[#1d1d1f] block leading-tight">
                      Оплатить баллами кэшбэка
                    </span>
                    <span className="text-[10px] text-[#86868b] block mt-0.5">
                      Списать баллы для покрытия разницы
                    </span>
                  </div>
                </label>

                {balance !== null && (
                  <div className="pt-2 border-t border-black/[0.04] flex justify-between items-center text-[10px]">
                    <span className="text-[#86868b]">Доступный баланс:</span>
                    <span className="font-semibold text-[#0071e3]">{balance.toFixed(2)} б.</span>
                  </div>
                )}
              </div>

              {/* Warnings / Calculations */}
              <div className="text-xs space-y-1.5 p-3.5 rounded-2xl bg-white border border-black/[0.06]">
                <div className="flex justify-between text-[#86868b]">
                  <span>К оплате деньгами:</span>
                  <span className="font-semibold text-[#1d1d1f]">{finalTransactionAmount.toFixed(2)} ₽</span>
                </div>

                {useCashback && isAmountLessThanItems && (
                  <div className="flex justify-between text-[#34c759] bg-[#34c759]/10 px-2.5 py-1 rounded-xl">
                    <span>Списание баллов:</span>
                    <span className="font-semibold">-{amountDifference.toFixed(2)} б.</span>
                  </div>
                )}

                {isAmountGreaterThanItems && (
                  <div className="flex items-center space-x-1.5 text-[#ff3b30] text-[10px]">
                    <AlertCircle className="w-3.5 h-3.5 flex-shrink-0" />
                    <span>Сумма транзакции не может быть больше суммы товаров.</span>
                  </div>
                )}

                {isAmountLessThanItems && !useCashback && (
                  <div className="flex items-center space-x-1.5 text-[#ff9500] text-[10px]">
                    <Info className="w-3.5 h-3.5 flex-shrink-0" />
                    <span>Включите списание кэшбэка выше, чтобы покрыть разницу.</span>
                  </div>
                )}
              </div>

              {/* Apple Pay / Big CTA button */}
              <button
                type="button"
                onClick={handleCreateTransaction}
                disabled={submittingTx || isAmountGreaterThanItems || (isAmountLessThanItems && !useCashback)}
                className="w-full py-3.5 bg-[#0071e3] hover:bg-[#0077ed] active:scale-[0.98] text-white font-medium text-sm rounded-full transition-all duration-200 shadow-sm disabled:opacity-50 flex items-center justify-center space-x-2"
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
        <div className="md:hidden flex bg-black/[0.05] p-1 rounded-full border border-black/[0.04]">
          <button
            onClick={() => setHistoryTab('points')}
            className={`flex-1 py-1.5 text-xs font-medium rounded-full transition-all duration-200 ${
              historyTab === 'points' ? 'bg-white text-[#1d1d1f] shadow-sm font-semibold' : 'text-[#86868b]'
            }`}
          >
            Баллы
          </button>
          <button
            onClick={() => setHistoryTab('receipts')}
            className={`flex-1 py-1.5 text-xs font-medium rounded-full transition-all duration-200 ${
              historyTab === 'receipts' ? 'bg-white text-[#1d1d1f] shadow-sm font-semibold' : 'text-[#86868b]'
            }`}
          >
            Чеки покупок
          </button>
        </div>

        {/* History Tables Section */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Wallet Points History */}
          <div className={`${historyTab === 'points' ? 'block' : 'hidden md:block'} bg-white rounded-3xl p-5 sm:p-7 border border-black/[0.06] shadow-[0_4px_24px_rgba(0,0,0,0.03)]`}>
            <div className="flex items-center justify-between mb-4 pb-3 border-b border-black/[0.06]">
              <div className="flex items-center space-x-2.5">
                <div className="p-1.5 bg-[#f5f5f7] text-[#0071e3] rounded-xl">
                  <Coins className="w-4 h-4" />
                </div>
                <h3 className="font-semibold text-[#1d1d1f] text-sm sm:text-base">Баллы кошелька</h3>
              </div>
              <button onClick={fetchHistory} className="p-1 text-[#86868b] hover:text-[#1d1d1f] transition">
                <RefreshCw className={`w-3.5 h-3.5 ${loadingHistory ? 'animate-spin' : ''}`} />
              </button>
            </div>

            <div className="divide-y divide-black/[0.04] max-h-80 overflow-y-auto">
              {walletHistory.length === 0 ? (
                <div className="py-8 text-center text-xs text-[#86868b]">Операций пока нет</div>
              ) : (
                walletHistory.map((item) => (
                  <div key={item.id} className="py-3 flex items-center justify-between text-xs">
                    <div className="flex items-center space-x-3 min-w-0">
                      <div
                        className={`w-7 h-7 rounded-full flex items-center justify-center flex-shrink-0 ${
                          item.type === 'CREDIT'
                            ? 'bg-[#34c759]/10 text-[#34c759]'
                            : item.type === 'CANCEL'
                            ? 'bg-[#ff9500]/10 text-[#ff9500]'
                            : 'bg-[#ff3b30]/10 text-[#ff3b30]'
                        }`}
                      >
                        {item.type === 'CREDIT' ? (
                          <ArrowDownLeft className="w-3.5 h-3.5" />
                        ) : item.type === 'CANCEL' ? (
                          <RotateCcw className="w-3.5 h-3.5" />
                        ) : (
                          <ArrowUpRight className="w-3.5 h-3.5" />
                        )}
                      </div>
                      <div className="min-w-0">
                        <div className="font-medium text-[#1d1d1f] truncate text-[11px] sm:text-xs">
                          {item.description || (item.type === 'CREDIT' ? 'Кэшбэк' : item.type === 'CANCEL' ? 'Возврат / Корректировка' : 'Списание')}
                        </div>
                        <div className="text-[10px] text-[#86868b]">
                          {new Date(item.createdAt).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })} • {new Date(item.createdAt).toLocaleDateString('ru-RU')}
                        </div>
                      </div>
                    </div>

                    <div
                      className={`font-semibold text-xs sm:text-sm flex-shrink-0 pl-2 ${
                        item.type === 'CREDIT'
                          ? 'text-[#34c759]'
                          : item.type === 'CANCEL'
                          ? 'text-[#ff9500]'
                          : 'text-[#ff3b30]'
                      }`}
                    >
                      {item.type === 'CREDIT' ? '+' : item.type === 'CANCEL' ? '↺ ' : '-'}
                      {Number(item.amount).toFixed(2)}
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>

          {/* Purchases / Receipts History */}
          <div className={`${historyTab === 'receipts' ? 'block' : 'hidden md:block'} bg-white rounded-3xl p-5 sm:p-7 border border-black/[0.06] shadow-[0_4px_24px_rgba(0,0,0,0.03)]`}>
            <div className="flex items-center justify-between mb-4 pb-3 border-b border-black/[0.06]">
              <div className="flex items-center space-x-2.5">
                <div className="p-1.5 bg-[#f5f5f7] text-[#0071e3] rounded-xl">
                  <Receipt className="w-4 h-4" />
                </div>
                <h3 className="font-semibold text-[#1d1d1f] text-sm sm:text-base">Чеки покупок</h3>
              </div>
              <button onClick={fetchTransactions} className="p-1 text-[#86868b] hover:text-[#1d1d1f] transition">
                <RefreshCw className={`w-3.5 h-3.5 ${loadingTxList ? 'animate-spin' : ''}`} />
              </button>
            </div>

            <div className="divide-y divide-black/[0.04] max-h-80 overflow-y-auto">
              {transactions.length === 0 ? (
                <div className="py-8 text-center text-xs text-[#86868b]">Чеков пока нет</div>
              ) : (
                transactions.map((tx) => (
                  <div
                    key={tx.id}
                    onClick={() => openTxModal(tx)}
                    className="py-3 px-2 -mx-2 rounded-2xl hover:bg-black/[0.02] cursor-pointer flex items-center justify-between text-xs transition-colors duration-150"
                  >
                    <div className="flex items-center space-x-3 min-w-0">
                      <div className="w-7 h-7 rounded-full bg-[#f5f5f7] flex items-center justify-center text-[#1d1d1f] flex-shrink-0">
                        <Receipt className="w-3.5 h-3.5" />
                      </div>
                      <div className="min-w-0">
                        <div className="font-medium text-[#1d1d1f] text-[11px] sm:text-xs truncate">
                          Чек #{tx.id.substring(0, 8)} ({tx.items?.length || 0} поз.)
                        </div>
                        <div className="text-[10px] text-[#86868b]">
                          {new Date(tx.createdAt).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit' })} • {new Date(tx.createdAt).toLocaleDateString('ru-RU')}
                        </div>
                      </div>
                    </div>

                    <div className="text-right flex items-center space-x-2 flex-shrink-0">
                      <div>
                        <div className="font-semibold text-[#1d1d1f] text-xs font-sans">{Number(tx.amount).toFixed(2)} ₽</div>
                        <span
                          className={`inline-block px-2 py-0.5 rounded-full text-[9px] font-medium ${
                            tx.status === 'HANDLED' || tx.status === 'PROCESSED'
                              ? 'bg-[#34c759]/10 text-[#34c759]'
                              : tx.status === 'CANCELLED'
                              ? 'bg-black/[0.06] text-[#86868b]'
                              : tx.status === 'FAILED' || tx.status === 'REJECTED'
                              ? 'bg-[#ff3b30]/10 text-[#ff3b30]'
                              : 'bg-[#ff9500]/10 text-[#ff9500]'
                          }`}
                        >
                          {tx.status === 'PROCESSED' || tx.status === 'HANDLED'
                            ? 'Оплачен'
                            : tx.status === 'CANCELLED'
                            ? 'Возврат'
                            : tx.status === 'REJECTED'
                            ? 'Отклонён'
                            : tx.status}
                        </span>
                      </div>
                      <ChevronRight className="w-3.5 h-3.5 text-[#86868b]" />
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>

        {/* Receipt Modal (Apple Sheet style) */}
        {selectedTx && (
          <div className="fixed inset-0 bg-black/40 backdrop-blur-md flex items-center justify-center p-4 z-50 transition-opacity">
            <div className="bg-white max-w-sm w-full rounded-3xl p-6 shadow-2xl border border-black/[0.08] animate-in fade-in zoom-in-95 duration-200">
              <div className="flex items-center justify-between pb-3 border-b border-black/[0.06]">
                <div className="flex items-center space-x-2">
                  <Receipt className="w-4 h-4 text-[#0071e3]" />
                  <h3 className="font-semibold text-[#1d1d1f] text-sm">Детали чека</h3>
                </div>
                <button
                  onClick={() => setSelectedTx(null)}
                  className="w-7 h-7 flex items-center justify-center rounded-full bg-[#f5f5f7] text-[#86868b] hover:text-[#1d1d1f] text-xs font-bold transition"
                >
                  ✕
                </button>
              </div>

              <div className="mt-4 space-y-3.5 text-xs">
                <div className="bg-[#f5f5f7] p-3 rounded-2xl space-y-1 font-mono text-[10px] border border-black/[0.04]">
                  <div className="flex justify-between text-[#86868b]">
                    <span>ID:</span>
                    <span className="text-[#1d1d1f] font-medium">{selectedTx.id.substring(0, 16)}...</span>
                  </div>
                  <div className="flex justify-between text-[#86868b] items-center">
                    <span>Статус:</span>
                    <span
                      className={`px-2 py-0.5 rounded-full text-[9px] font-semibold ${
                        selectedTx.status === 'PROCESSED' || selectedTx.status === 'HANDLED'
                          ? 'bg-[#34c759]/15 text-[#34c759]'
                          : selectedTx.status === 'CANCELLED'
                          ? 'bg-black/[0.08] text-[#86868b]'
                          : selectedTx.status === 'REJECTED'
                          ? 'bg-[#ff3b30]/15 text-[#ff3b30]'
                          : 'bg-[#ff9500]/15 text-[#ff9500]'
                      }`}
                    >
                      {selectedTx.status === 'PROCESSED' || selectedTx.status === 'HANDLED'
                        ? 'Оплачен'
                        : selectedTx.status === 'CANCELLED'
                        ? 'Возврат оформлен'
                        : selectedTx.status === 'REJECTED'
                        ? 'Отклонён'
                        : selectedTx.status}
                    </span>
                  </div>
                </div>

                <div>
                  <span className="font-medium text-[#86868b] block mb-1.5 text-[11px]">Позиции:</span>
                  <div className="divide-y divide-black/[0.04] border border-black/[0.06] rounded-2xl overflow-hidden max-h-40 overflow-y-auto">
                    {selectedTx.items?.map((item, idx) => (
                      <div key={idx} className="p-2.5 flex justify-between items-center text-[11px]">
                        <div>
                          <div className="font-medium text-[#1d1d1f]">{item.name}</div>
                          <div className="text-[9px] text-[#86868b] font-mono">{item.category}</div>
                        </div>
                        <div className="font-semibold text-[#1d1d1f]">{Number(item.price).toFixed(2)} ₽</div>
                      </div>
                    ))}
                  </div>
                </div>

                <div className="pt-3 border-t border-black/[0.06] flex justify-between items-baseline font-semibold text-[#1d1d1f]">
                  <span>Итого к оплате:</span>
                  <span className="text-base text-[#0071e3]">{Number(selectedTx.amount).toFixed(2)} ₽</span>
                </div>

                {/* Refund Messages and Button */}
                {(selectedTx.status === 'PROCESSED' || selectedTx.status === 'HANDLED') && (
                  <div className="space-y-2 pt-2 border-t border-black/[0.04]">
                    {cancelErrorMsg && (
                      <div className="p-2.5 rounded-xl bg-[#ff3b30]/10 text-[#ff3b30] text-[11px] flex items-center space-x-1.5">
                        <AlertCircle className="w-3.5 h-3.5 flex-shrink-0" />
                        <span>{cancelErrorMsg}</span>
                      </div>
                    )}
                    {cancelSuccessMsg && (
                      <div className="p-2.5 rounded-xl bg-[#34c759]/10 text-[#34c759] text-[11px] flex items-center space-x-1.5">
                        <CheckCircle2 className="w-3.5 h-3.5 flex-shrink-0" />
                        <span>{cancelSuccessMsg}</span>
                      </div>
                    )}
                    <button
                      onClick={() => handleCancelTransaction(selectedTx.id)}
                      disabled={cancellingTxId === selectedTx.id}
                      className="w-full py-2.5 bg-[#ff3b30]/10 hover:bg-[#ff3b30]/15 active:scale-[0.98] text-[#ff3b30] font-medium rounded-full text-xs transition-all duration-200 flex items-center justify-center space-x-2"
                    >
                      {cancellingTxId === selectedTx.id ? (
                        <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                      ) : (
                        <>
                          <RotateCcw className="w-3.5 h-3.5" />
                          <span>Оформить возврат (отмена чека)</span>
                        </>
                      )}
                    </button>
                  </div>
                )}

                {selectedTx.status === 'CANCELLED' && (
                  <div className="p-2.5 bg-black/[0.03] rounded-xl text-[10px] text-[#86868b] flex items-center space-x-1.5 border border-black/[0.04]">
                    <RotateCcw className="w-3.5 h-3.5 flex-shrink-0 text-[#86868b]" />
                    <span>Чек аннулирован. Кэшбэк отозван или баллы возвращены на счет.</span>
                  </div>
                )}
              </div>

              <div className="mt-5">
                <button
                  onClick={() => setSelectedTx(null)}
                  className="w-full py-2.5 bg-[#f5f5f7] hover:bg-[#e8e8ed] text-[#1d1d1f] font-medium rounded-full text-xs transition-all duration-200"
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
