import React, { useState, useEffect, useCallback } from 'react';
import { useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { walletApi } from '../api/wallet';
import { transactionApi } from '../api/transactions';
import { productApi } from '../api/products';
import type { TransactionDTO, CreateTransactionItem, ProductDTO } from '../types';
import {
  Plus,
  Trash2,
  CheckCircle2,
  AlertCircle,
  RefreshCw,
  Receipt,
  Calculator,
  Info,
  ChevronRight,
  Sparkles,
  CreditCard,
  Coffee,
  ShoppingCart,
  Headphones,
  Fuel,
  Shirt,
  X,
  Coins,
  RotateCcw,
  Package,
} from 'lucide-react';

const getCategoryIcon = (category: string) => {
  const cat = category.toLowerCase();
  if (cat.includes('cafe') || cat.includes('кофе') || cat.includes('кафе')) return <Coffee className="w-4 h-4 text-[#ff9f0a]" />;
  if (cat.includes('groc') || cat.includes('продукт')) return <ShoppingCart className="w-4 h-4 text-[#30d158]" />;
  if (cat.includes('elect') || cat.includes('электрон')) return <Headphones className="w-4 h-4 text-[#0a84ff]" />;
  if (cat.includes('apparel') || cat.includes('одежд')) return <Shirt className="w-4 h-4 text-[#bf5af2]" />;
  if (cat.includes('auto') || cat.includes('авто')) return <Fuel className="w-4 h-4 text-[#ff453a]" />;
  return <CreditCard className="w-4 h-4 text-[#2997ff]" />;
};

export const CheckoutPage: React.FC = () => {
  const { user, userId } = useAuth();
  const location = useLocation();

  const [balance, setBalance] = useState<number | null>(null);
  const [transactions, setTransactions] = useState<TransactionDTO[]>([]);
  const [loadingTxList, setLoadingTxList] = useState(false);

  const [products, setProducts] = useState<ProductDTO[]>([]);
  const [loadingProducts, setLoadingProducts] = useState(false);

  // Cart state (empty by default)
  const [cartItems, setCartItems] = useState<CreateTransactionItem[]>([]);

  // Form input
  const [selectedCatalogId, setSelectedCatalogId] = useState<string>('');
  const [itemName, setItemName] = useState('');
  const [itemCategory, setItemCategory] = useState('cafe');
  const [itemPrice, setItemPrice] = useState('');

  // Custom amount & auto-calculation
  const [customAmount, setCustomAmount] = useState<string>('0.00');
  const [isAutoAmount, setIsAutoAmount] = useState<boolean>(true);

  // Payment flags & points redemption
  const [useCashback, setUseCashback] = useState(false);
  const [pointsToRedeem, setPointsToRedeem] = useState<number>(0);

  const [submittingTx, setSubmittingTx] = useState(false);
  const [txSuccess, setTxSuccess] = useState<string | null>(null);
  const [txError, setTxError] = useState<string | null>(null);

  // Selected Transaction for receipt modal
  const [selectedTx, setSelectedTx] = useState<TransactionDTO | null>(null);
  const [cancellingTxId, setCancellingTxId] = useState<string | null>(null);
  const [cancelSuccessMsg, setCancelSuccessMsg] = useState<string | null>(null);
  const [cancelErrorMsg, setCancelErrorMsg] = useState<string | null>(null);

  const fetchProducts = useCallback(async () => {
    setLoadingProducts(true);
    try {
      const list = await productApi.getAllProducts();
      setProducts(list);
    } catch (err) {
      console.warn('Could not fetch products', err);
    } finally {
      setLoadingProducts(false);
    }
  }, []);

  const fetchBalance = useCallback(async () => {
    if (!userId) return;
    try {
      const bal = await walletApi.getBalance(userId);
      setBalance(bal);
    } catch (err) {
      console.warn('Could not fetch balance', err);
    }
  }, [userId]);

  const fetchTransactions = useCallback(async () => {
    if (!userId) return;
    setLoadingTxList(true);
    try {
      const list = await transactionApi.getUserTransactions(userId);
      const sorted = [...list].sort(
        (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      );
      setTransactions(sorted);
    } catch (err) {
      console.warn('Could not fetch transactions', err);
    } finally {
      setLoadingTxList(false);
    }
  }, [userId]);

  useEffect(() => {
    fetchProducts();
    if (userId) {
      fetchBalance();
      fetchTransactions();
    }
  }, [userId, fetchBalance, fetchTransactions, fetchProducts]);

  // If navigated from catalog with a pre-selected product
  useEffect(() => {
    const stateProduct = location.state?.selectedProduct as ProductDTO | undefined;
    if (stateProduct) {
      setSelectedCatalogId(stateProduct.productId);
      setItemName(stateProduct.title);
      setItemCategory(stateProduct.category);
      setItemPrice(stateProduct.price.toString());

      setCartItems((prev) => {
        if (prev.length === 0) {
          const newItem: CreateTransactionItem = {
            name: stateProduct.title,
            category: stateProduct.category.toLowerCase(),
            price: stateProduct.price,
          };
          setCustomAmount(stateProduct.price.toFixed(2));
          return [newItem];
        }
        return prev;
      });
    }
  }, [location.state]);

  const totalCartAmount = cartItems.reduce((acc, item) => acc + item.price, 0);
  const availableBalance = balance !== null ? balance : 0;
  // Backend requires cash amount >= 0.01
  const maxPossiblePoints = Math.max(0, Math.min(availableBalance, Number((totalCartAmount - 0.01).toFixed(2))));

  const selectProduct = (p: ProductDTO) => {
    setSelectedCatalogId(p.productId);
    setItemName(p.title);
    setItemCategory(p.category);
    setItemPrice(p.price.toString());
  };

  const handleAddItemToCart = (e: React.FormEvent) => {
    e.preventDefault();
    const priceNum = parseFloat(itemPrice);
    if (!itemName.trim() || isNaN(priceNum) || priceNum <= 0) {
      setTxError('Укажите корректное наименование и цену (> 0 ₽)');
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

    const newTotal = newCart.reduce((sum, it) => sum + it.price, 0);
    if (useCashback) {
      const newMax = Math.max(0, Math.min(availableBalance, Number((newTotal - 0.01).toFixed(2))));
      const newPts = Math.min(pointsToRedeem, newMax);
      setPointsToRedeem(newPts);
      setCustomAmount(Math.max(0.01, Number((newTotal - newPts).toFixed(2))).toFixed(2));
    } else if (isAutoAmount) {
      setCustomAmount(newTotal.toFixed(2));
    }
  };

  const removeItem = (index: number) => {
    const newCart = cartItems.filter((_, i) => i !== index);
    setCartItems(newCart);
    const newTotal = newCart.reduce((sum, it) => sum + it.price, 0);

    if (useCashback) {
      const newMax = Math.max(0, Math.min(availableBalance, Number((newTotal - 0.01).toFixed(2))));
      const newPts = Math.min(pointsToRedeem, newMax);
      setPointsToRedeem(newPts);
      setCustomAmount(Math.max(0.01, Number((newTotal - newPts).toFixed(2))).toFixed(2));
    } else if (isAutoAmount) {
      setCustomAmount(newTotal.toFixed(2));
    }
  };

  const handleUpdateItemPrice = (index: number, newPriceStr: string) => {
    const priceNum = parseFloat(newPriceStr);
    const newCart = cartItems.map((it, idx) =>
      idx === index ? { ...it, price: isNaN(priceNum) ? 0 : priceNum } : it
    );
    setCartItems(newCart);
    const newTotal = newCart.reduce((sum, it) => sum + it.price, 0);

    if (useCashback) {
      const newMax = Math.max(0, Math.min(availableBalance, Number((newTotal - 0.01).toFixed(2))));
      const newPts = Math.min(pointsToRedeem, newMax);
      setPointsToRedeem(newPts);
      setCustomAmount(Math.max(0.01, Number((newTotal - newPts).toFixed(2))).toFixed(2));
    } else if (isAutoAmount) {
      setCustomAmount(newTotal.toFixed(2));
    }
  };

  // Toggle Cashback switch
  const handleToggleCashback = (checked: boolean) => {
    if (checked) {
      if (availableBalance <= 0) {
        setTxError('На вашем балансе нет бонусных баллов для списания');
        return;
      }
      setTxError(null);
      setUseCashback(true);
      const defaultDeduct = maxPossiblePoints;
      setPointsToRedeem(defaultDeduct);
      const cashToPay = Math.max(0.01, Number((totalCartAmount - defaultDeduct).toFixed(2)));
      setCustomAmount(cashToPay.toFixed(2));
      setIsAutoAmount(true);
    } else {
      setUseCashback(false);
      setPointsToRedeem(0);
      setCustomAmount(totalCartAmount.toFixed(2));
      setIsAutoAmount(true);
    }
  };

  // Adjust redeemed points manually
  const handlePointsChange = (newPts: number) => {
    const clamped = Math.max(0, Math.min(newPts, maxPossiblePoints));
    setPointsToRedeem(clamped);
    const cash = Math.max(0.01, Number((totalCartAmount - clamped).toFixed(2)));
    setCustomAmount(cash.toFixed(2));
  };

  const handleSyncAmountWithItems = () => {
    setUseCashback(false);
    setPointsToRedeem(0);
    setIsAutoAmount(true);
    setCustomAmount(totalCartAmount.toFixed(2));
  };

  const handleCustomAmountChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setIsAutoAmount(false);
    const val = e.target.value;
    setCustomAmount(val);
    const parsed = parseFloat(val);
    if (!isNaN(parsed) && parsed > 0 && parsed < totalCartAmount) {
      const diff = Number((totalCartAmount - parsed).toFixed(2));
      if (availableBalance > 0) {
        setUseCashback(true);
        setPointsToRedeem(Math.min(diff, maxPossiblePoints));
      }
    } else if (!isNaN(parsed) && parsed >= totalCartAmount) {
      setUseCashback(false);
      setPointsToRedeem(0);
    }
  };

  const parsedCustomAmount = parseFloat(customAmount);
  const finalTransactionAmount =
    !isNaN(parsedCustomAmount) && parsedCustomAmount > 0 ? parsedCustomAmount : totalCartAmount;

  const actualPointsDeducted = useCashback
    ? Number((totalCartAmount - finalTransactionAmount).toFixed(2))
    : 0;

  const isAmountGreaterThanItems = finalTransactionAmount > totalCartAmount;
  const isAmountLessThanItems = finalTransactionAmount < totalCartAmount;

  const handleCreateTransaction = async () => {
    if (cartItems.length === 0) {
      setTxError('Добавьте хотя бы один товар в чек');
      return;
    }

    if (isNaN(parsedCustomAmount) || parsedCustomAmount < 0.01) {
      setTxError('Сумма транзакции деньгами должна быть не менее 0.01 ₽');
      return;
    }

    if (isAmountGreaterThanItems) {
      setTxError('Сумма оплаты не может превышать суммарную стоимость товаров');
      return;
    }

    if (isAmountLessThanItems && !useCashback) {
      setTxError('Сумма оплаты меньше стоимости товаров. Включите списание баллов для покрытия разницы.');
      return;
    }

    if (useCashback && actualPointsDeducted > 0) {
      if (balance !== null && balance < actualPointsDeducted) {
        setTxError(
          `Недостаточно баллов на балансе! Требуется списать: ${actualPointsDeducted.toFixed(2)} б., доступно: ${balance.toFixed(2)} б.`
        );
        return;
      }
    }

    setSubmittingTx(true);
    setTxError(null);
    setTxSuccess(null);

    try {
      const response = await transactionApi.createTransaction(
        {
          amount: finalTransactionAmount,
          items: cartItems,
          useCashbackBalance: useCashback && actualPointsDeducted > 0,
        },
        userId || undefined
      );

      const successMsg = actualPointsDeducted > 0
        ? `Оплата #${response.id ? response.id.substring(0, 8) : ''} на сумму ${finalTransactionAmount.toFixed(2)} ₽ успешно завершена. Списано ${actualPointsDeducted.toFixed(2)} баллов!`
        : `Оплата #${response.id ? response.id.substring(0, 8) : ''} на сумму ${finalTransactionAmount.toFixed(2)} ₽ успешно завершена. Начислен кэшбэк!`;

      setTxSuccess(successMsg);

      setTimeout(() => {
        fetchBalance();
        fetchTransactions();
      }, 1000);
    } catch (err: any) {
      console.error('Failed to create transaction:', err);
      const msg = err.response?.data?.message || err.response?.data || 'Ошибка проведения транзакции';
      setTxError(typeof msg === 'string' ? msg : 'Ошибка при оформлении покупки');
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
      setCancelSuccessMsg('Возврат успешно оформлен! Чек аннулирован.');
      if (selectedTx && selectedTx.id === txId) {
        setSelectedTx({ ...selectedTx, status: 'CANCELLED' });
      }
      setTransactions((prev) =>
        prev.map((t) => (t.id === txId ? { ...t, status: 'CANCELLED' } : t))
      );
      setTimeout(() => {
        fetchBalance();
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

  const getCategoryColor = (category: string) => {
    const cat = category.toLowerCase();
    if (cat.includes('cafe')) return 'text-[#ff9f0a] bg-[#ff9f0a]/10 border-[#ff9f0a]/20';
    if (cat.includes('groceries')) return 'text-[#30d158] bg-[#30d158]/10 border-[#30d158]/20';
    if (cat.includes('electronics')) return 'text-[#0a84ff] bg-[#0a84ff]/10 border-[#0a84ff]/20';
    if (cat.includes('apparel')) return 'text-[#bf5af2] bg-[#bf5af2]/10 border-[#bf5af2]/20';
    if (cat.includes('auto')) return 'text-[#ff453a] bg-[#ff453a]/10 border-[#ff453a]/20';
    return 'text-white/60 bg-white/5 border-white/10';
  };

  return (
    <div className="min-h-screen bg-[#0d0d0f] py-8 sm:py-12 px-4 sm:px-6 lg:px-8 pb-32 md:pb-16 max-w-5xl mx-auto space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <span className="text-[11px] uppercase tracking-widest text-[#2997ff] font-semibold flex items-center space-x-1.5">
            <CreditCard className="w-3.5 h-3.5" />
            <span>Касса & Оплата заказов</span>
          </span>
          <h1 className="text-2xl sm:text-3xl font-semibold text-white tracking-tight mt-0.5">
            Оформление чека
          </h1>
        </div>

        {balance !== null && (
          <div className="flex items-center space-x-2.5 bg-white/[0.04] backdrop-blur-xl px-4 py-2 rounded-full border border-white/10">
            <Sparkles className="w-4 h-4 text-[#ffd60a]" />
            <span className="text-xs text-white/50">Баланс баллов:</span>
            <span className="text-sm font-semibold text-white">{balance.toFixed(2)} б.</span>
          </div>
        )}
      </div>

      {/* Alert Notices */}
      {txSuccess && (
        <div className="p-4 rounded-2xl bg-[#30d158]/10 border border-[#30d158]/25 text-[#30d158] text-xs flex items-start space-x-3 backdrop-blur-xl animate-in fade-in duration-300">
          <CheckCircle2 className="w-5 h-5 flex-shrink-0 mt-0.5" />
          <div className="font-medium text-sm leading-snug">{txSuccess}</div>
        </div>
      )}

      {txError && (
        <div className="p-4 rounded-2xl bg-[#ff453a]/10 border border-[#ff453a]/25 text-[#ff453a] text-xs flex items-start space-x-3 backdrop-blur-xl animate-in fade-in duration-300">
          <AlertCircle className="w-5 h-5 flex-shrink-0 mt-0.5" />
          <div className="font-medium text-sm leading-snug">{txError}</div>
        </div>
      )}

      {/* Main Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Left Column: Product Selection & Cart */}
        <div className="lg:col-span-7 space-y-6">
          {/* Step 1: Add Item Card */}
          <div className="apple-card p-6 sm:p-7 space-y-5">
            <div className="flex items-center space-x-3 pb-3 border-b border-white/[0.06]">
              <span className="w-6 h-6 rounded-full bg-[#2997ff] text-white flex items-center justify-center font-bold text-xs shadow-sm">
                1
              </span>
              <div>
                <h2 className="font-semibold text-sm sm:text-base text-white">Выбор товара в чек</h2>
                <p className="text-[11px] text-white/50">Быстрый каталог или ручной ввод позиции</p>
              </div>
            </div>

            {/* Quick Catalog Chips from ProductService */}
            <div>
              <div className="flex items-center justify-between mb-2">
                <span className="text-[11px] font-medium text-white/40 uppercase tracking-wider flex items-center space-x-1.5">
                  <Package className="w-3.5 h-3.5 text-[#2997ff]" />
                  <span>Каталог товаров (ProductService):</span>
                </span>
                {loadingProducts && (
                  <span className="text-[10px] text-white/40 flex items-center space-x-1">
                    <RefreshCw className="w-3 h-3 animate-spin text-[#2997ff]" />
                    <span>Загрузка...</span>
                  </span>
                )}
              </div>

              {products.filter((p) => p.status === 'ACTIVE').length === 0 ? (
                <div className="p-3.5 rounded-xl bg-white/[0.02] border border-white/5 text-center">
                  <p className="text-xs text-white/60">
                    {loadingProducts ? 'Загрузка списка товаров...' : 'В каталоге пока нет активных товаров.'}
                  </p>
                  <p className="text-[11px] text-white/40 mt-0.5">
                    Вы можете ввести название и цену вручную ниже или добавить товары во вкладке управления.
                  </p>
                </div>
              ) : (
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
                  {products
                    .filter((p) => p.status === 'ACTIVE')
                    .map((p) => {
                      const isSelected = selectedCatalogId === p.productId;
                      return (
                        <button
                          key={p.productId}
                          type="button"
                          onClick={() => selectProduct(p)}
                          className={`text-left p-2.5 rounded-xl border text-xs transition-all duration-200 flex flex-col justify-between ${
                            isSelected
                              ? 'bg-white/10 border-white/40 shadow-sm'
                              : 'bg-white/[0.02] border-white/5 hover:bg-white/[0.06] hover:border-white/15'
                          }`}
                        >
                          <div className="flex items-center justify-between mb-1">
                            {getCategoryIcon(p.category)}
                            <span className="text-[10px] font-semibold text-white/80">{p.price} ₽</span>
                          </div>
                          <span className="text-[11px] font-medium text-white/90 line-clamp-1" title={p.title}>
                            {p.title}
                          </span>
                          <span className="text-[9px] text-white/40 capitalize">{p.category}</span>
                        </button>
                      );
                    })}
                </div>
              )}
            </div>

            {/* Custom Input Form */}
            <form onSubmit={handleAddItemToCart} className="space-y-3.5 pt-2">
              <div className="grid grid-cols-3 gap-3">
                <div className="col-span-2">
                  <label className="block text-[10px] font-medium text-white/40 uppercase tracking-wider mb-1">
                    Наименование
                  </label>
                  <input
                    type="text"
                    required
                    value={itemName}
                    onChange={(e) => {
                      setItemName(e.target.value);
                      setSelectedCatalogId('custom');
                    }}
                    placeholder="Например, Кофе"
                    className="w-full px-3.5 py-2.5 bg-white/[0.04] border border-white/10 rounded-xl text-xs text-white placeholder-white/30 focus:border-[#2997ff] focus:ring-1 focus:ring-[#2997ff]/40 focus:outline-none transition"
                  />
                </div>

                <div>
                  <label className="block text-[10px] font-medium text-white/40 uppercase tracking-wider mb-1">
                    Категория
                  </label>
                  <select
                    value={itemCategory}
                    onChange={(e) => {
                      setItemCategory(e.target.value);
                      setSelectedCatalogId('custom');
                    }}
                    className="w-full px-3 py-2.5 bg-[#16161a] border border-white/10 rounded-xl text-xs text-white focus:border-[#2997ff] focus:outline-none transition"
                  >
                    <option value="cafe">Кафе (cafe)</option>
                    <option value="groceries">Продукты (groceries)</option>
                    <option value="electronics">Электроника (electronics)</option>
                    <option value="apparel">Одежда (apparel)</option>
                    <option value="auto">Авто / АЗС (auto)</option>
                    <option value="pharmacy">Аптеки (pharmacy)</option>
                    <option value="general">Общее (general)</option>
                  </select>
                </div>
              </div>

              <div className="flex items-center space-x-2 pt-1">
                <div className="relative flex-1">
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    required
                    value={itemPrice}
                    onChange={(e) => setItemPrice(e.target.value)}
                    placeholder="Цена"
                    className="w-full pl-3.5 pr-8 py-2.5 bg-white/[0.04] border border-white/10 rounded-xl text-xs font-semibold text-white placeholder-white/30 focus:border-[#2997ff] focus:ring-1 focus:ring-[#2997ff]/40 focus:outline-none transition"
                  />
                  <span className="absolute right-3 top-2.5 text-xs text-white/40 font-medium">₽</span>
                </div>

                <button
                  type="submit"
                  className="px-5 py-2.5 bg-white hover:bg-white/90 active:scale-95 text-black text-xs font-semibold rounded-full flex items-center space-x-1.5 transition-all duration-200 shadow-sm"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>В чек</span>
                </button>
              </div>
            </form>
          </div>

          {/* Step 2: Cart Items List */}
          <div className="apple-card p-6 sm:p-7 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-white/[0.06]">
              <div className="flex items-center space-x-3">
                <span className="w-6 h-6 rounded-full bg-white/10 text-white flex items-center justify-center font-bold text-xs">
                  2
                </span>
                <h2 className="font-semibold text-sm sm:text-base text-white">
                  Позиции в чеке ({cartItems.length})
                </h2>
              </div>
              <span className="text-[11px] text-white/40">Редактирование цены на лету</span>
            </div>

            <div className="divide-y divide-white/[0.04]">
              {cartItems.length === 0 ? (
                <div className="py-10 text-center text-xs text-white/40">
                  Чек пуст. Добавьте товары из каталога выше.
                </div>
              ) : (
                cartItems.map((item, idx) => (
                  <div
                    key={idx}
                    className="py-3.5 flex items-center justify-between gap-3 px-2 -mx-2 rounded-xl hover:bg-white/[0.02] transition"
                  >
                    <div className="flex-1 min-w-0">
                      <div className="font-medium text-white text-xs sm:text-sm truncate">
                        {item.name}
                      </div>
                      <span
                        className={`inline-block mt-0.5 text-[9px] uppercase font-mono px-2 py-0.5 rounded-full border ${getCategoryColor(
                          item.category
                        )}`}
                      >
                        {item.category}
                      </span>
                    </div>

                    <div className="flex items-center space-x-2 flex-shrink-0">
                      <div className="relative">
                        <input
                          type="number"
                          step="0.01"
                          min="0.01"
                          value={item.price}
                          onChange={(e) => handleUpdateItemPrice(idx, e.target.value)}
                          className="w-20 sm:w-24 px-2 py-1 bg-white/[0.04] border border-white/10 rounded-lg text-right font-semibold text-xs text-white focus:border-[#2997ff] focus:outline-none"
                        />
                      </div>
                      <span className="text-xs text-white/40 font-medium">₽</span>

                      <button
                        onClick={() => removeItem(idx)}
                        title="Удалить позицию"
                        className="p-1.5 text-white/40 hover:text-[#ff453a] transition-colors"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>

            {cartItems.length > 0 && (
              <div className="pt-3 border-t border-white/[0.06] flex justify-between items-center text-xs">
                <span className="font-medium text-white/50">Стоимость товаров:</span>
                <span className="font-bold text-white text-base">
                  {totalCartAmount.toFixed(2)} ₽
                </span>
              </div>
            )}
          </div>
        </div>

        {/* Right Column: Apple Pay Terminal & Payment */}
        <div className="lg:col-span-5 space-y-6">
          {/* Apple Pay Terminal Box */}
          <div className="apple-card p-6 sm:p-7 space-y-5 relative overflow-hidden">
            {/* Ambient specular highlight */}
            <div className="absolute top-0 right-0 w-36 h-36 bg-[#2997ff]/10 rounded-full blur-3xl pointer-events-none" />

            <div className="flex items-center justify-between pb-3 border-b border-white/[0.06]">
              <span className="text-xs font-semibold uppercase tracking-wider text-white/90 flex items-center space-x-2">
                <Calculator className="w-4 h-4 text-[#2997ff]" />
                <span>3. Оплата & Списание</span>
              </span>
              <button
                type="button"
                onClick={handleSyncAmountWithItems}
                className="text-[11px] font-medium text-[#2997ff] hover:text-[#64d2ff] flex items-center space-x-1 transition"
              >
                <RefreshCw className="w-3 h-3" />
                <span>Сбросить скидку</span>
              </button>
            </div>

            {/* Miniature Loyalty Card Preview */}
            <div className="apple-card-iridescent p-4 rounded-2xl border border-white/15 space-y-3 relative overflow-hidden">
              <div className="flex justify-between items-start">
                <div className="apple-emv-chip" />
                <span className="text-[10px] tracking-widest uppercase font-semibold text-white/50">
                  LoyaltyEngine
                </span>
              </div>
              <div className="flex justify-between items-end pt-2">
                <div>
                  <div className="text-[9px] uppercase tracking-wider text-white/40">Владелец</div>
                  <div className="text-xs font-medium text-white tracking-wide">
                    {user?.firstName ? `${user.firstName} ${user.lastName || ''}`.toUpperCase() : 'LOYALTY CLIENT'}
                  </div>
                </div>
                <div className="text-right">
                  <div className="text-[9px] uppercase tracking-wider text-white/40">Тип</div>
                  <div className="text-xs font-semibold text-white">Loyalty Card</div>
                </div>
              </div>
            </div>

            {/* Apple iOS Switch: Cashback deduction */}
            <div className="bg-white/[0.03] p-4 rounded-2xl border border-white/[0.06] space-y-3">
              <label className="flex items-start justify-between cursor-pointer select-none">
                <div>
                  <span className="text-xs font-semibold text-white block leading-tight flex items-center gap-1.5">
                    <Coins className="w-4 h-4 text-[#ffd60a]" />
                    <span>Оплатить бонусами кэшбэка</span>
                  </span>
                  <span className="text-[10px] text-white/40 block mt-0.5">
                    {availableBalance > 0
                      ? `Доступно: ${availableBalance.toFixed(2)} б. (списание до ${(maxPossiblePoints).toFixed(2)} б.)`
                      : 'На балансе нет баллов для списания'}
                  </span>
                </div>
                <div className="relative inline-flex items-center cursor-pointer ml-3 flex-shrink-0">
                  <input
                    type="checkbox"
                    disabled={availableBalance <= 0}
                    checked={useCashback}
                    onChange={(e) => handleToggleCashback(e.target.checked)}
                    className="sr-only peer"
                  />
                  <div className="w-11 h-6 bg-white/20 peer-focus:outline-none rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-[#30d158] peer-disabled:opacity-40 peer-disabled:cursor-not-allowed"></div>
                </div>
              </label>

              {useCashback && maxPossiblePoints > 0 && (
                <div className="pt-2.5 border-t border-white/[0.06] space-y-2">
                  <div className="flex justify-between items-center text-xs">
                    <span className="text-white/60">Списать баллов:</span>
                    <div className="flex items-center space-x-1.5">
                      <input
                        type="number"
                        min="0.01"
                        max={maxPossiblePoints}
                        step="1"
                        value={pointsToRedeem}
                        onChange={(e) => handlePointsChange(parseFloat(e.target.value) || 0)}
                        className="w-24 px-2 py-1 bg-white/[0.06] border border-white/10 rounded-lg text-right font-bold text-xs text-[#30d158] focus:border-[#30d158] focus:outline-none"
                      />
                      <span className="text-xs text-white/50">б.</span>
                    </div>
                  </div>

                  <div className="flex gap-1.5 pt-1">
                    <button
                      type="button"
                      onClick={() => handlePointsChange(maxPossiblePoints)}
                      className="flex-1 py-1 px-2 rounded-lg bg-white/5 hover:bg-white/10 border border-white/5 text-[10px] text-white/70 transition"
                    >
                      Максимум ({maxPossiblePoints.toFixed(2)} б.)
                    </button>
                    {maxPossiblePoints >= 100 && (
                      <button
                        type="button"
                        onClick={() => handlePointsChange(100)}
                        className="py-1 px-2.5 rounded-lg bg-white/5 hover:bg-white/10 border border-white/5 text-[10px] text-white/70 transition"
                      >
                        100 б.
                      </button>
                    )}
                  </div>
                </div>
              )}
            </div>

            {/* Custom Payment Amount Input */}
            <div>
              <div className="flex items-center justify-between mb-1.5">
                <label className="text-[11px] uppercase tracking-wider font-medium text-white/50">
                  К оплате деньгами (₽):
                </label>
                {useCashback && pointsToRedeem > 0 && (
                  <span className="text-[10px] text-[#30d158] font-medium">
                    Учтена скидка: -{pointsToRedeem.toFixed(2)} ₽
                  </span>
                )}
              </div>

              <div className="relative">
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  value={customAmount}
                  onChange={handleCustomAmountChange}
                  className="w-full pl-4 pr-10 py-3 bg-white/[0.04] border border-white/15 focus:border-[#2997ff] focus:ring-1 focus:ring-[#2997ff]/40 rounded-2xl text-2xl font-bold text-white focus:outline-none transition-all duration-200"
                  placeholder="0.00"
                />
                <span className="absolute right-4 top-3.5 text-white/40 font-medium text-lg">₽</span>
              </div>

              <div className="flex items-center justify-between text-[11px] text-white/40 mt-1.5">
                <span>
                  {isAutoAmount ? (
                    <span className="text-[#30d158] font-medium flex items-center space-x-1">
                      <span className="w-1.5 h-1.5 rounded-full bg-[#30d158]" />
                      <span>{useCashback ? 'Рассчитано с бонусами' : 'Рассчитано по товарам'}</span>
                    </span>
                  ) : (
                    <span className="text-[#ffd60a] font-medium flex items-center space-x-1">
                      <span className="w-1.5 h-1.5 rounded-full bg-[#ffd60a]" />
                      <span>Ручной ввод суммы</span>
                    </span>
                  )}
                </span>
                <span>Товары: {totalCartAmount.toFixed(2)} ₽</span>
              </div>
            </div>

            {/* Financial Summary */}
            <div className="space-y-2 text-xs p-4 rounded-2xl bg-white/[0.02] border border-white/[0.05]">
              <div className="flex justify-between text-white/60">
                <span>Сумма товаров:</span>
                <span className="font-medium text-white">{totalCartAmount.toFixed(2)} ₽</span>
              </div>

              {useCashback && actualPointsDeducted > 0 && (
                <div className="flex justify-between text-[#30d158] bg-[#30d158]/10 px-2.5 py-1.5 rounded-xl font-medium">
                  <span>Списание бонусами с баланса:</span>
                  <span>-{actualPointsDeducted.toFixed(2)} б.</span>
                </div>
              )}

              <div className="flex justify-between text-white/80 pt-1 border-t border-white/[0.06] font-semibold">
                <span>Итого к списанию с карты:</span>
                <span className="text-white text-sm">{finalTransactionAmount.toFixed(2)} ₽</span>
              </div>

              {isAmountGreaterThanItems && (
                <div className="flex items-center space-x-1.5 text-[#ff453a] text-[10px] pt-1">
                  <AlertCircle className="w-3.5 h-3.5 flex-shrink-0" />
                  <span>Сумма не может превышать стоимость товаров.</span>
                </div>
              )}

              {isAmountLessThanItems && !useCashback && (
                <div className="flex items-center space-x-1.5 text-[#ffd60a] text-[10px] pt-1">
                  <Info className="w-3.5 h-3.5 flex-shrink-0" />
                  <span>Включите списание баллов выше для покрытия разницы.</span>
                </div>
              )}
            </div>

            {/* Primary Payment Button */}
            <button
              type="button"
              onClick={handleCreateTransaction}
              disabled={submittingTx || isAmountGreaterThanItems || (isAmountLessThanItems && !useCashback)}
              className="w-full py-4 bg-white hover:bg-white/95 active:scale-[0.98] text-black font-semibold text-sm rounded-full transition-all duration-200 shadow-[0_4px_24px_rgba(255,255,255,0.15)] disabled:opacity-40 disabled:cursor-not-allowed flex items-center justify-center space-x-2"
            >
              {submittingTx ? (
                <RefreshCw className="w-4 h-4 animate-spin text-black" />
              ) : (
                <>
                  <CreditCard className="w-4 h-4 text-black" />
                  <span>Оплатить {finalTransactionAmount.toFixed(2)} ₽</span>
                </>
              )}
            </button>
          </div>

          {/* Recent Receipts List */}
          <div className="apple-card p-6 sm:p-7 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-white/[0.06]">
              <div className="flex items-center space-x-2">
                <Receipt className="w-4 h-4 text-[#2997ff]" />
                <h3 className="font-semibold text-white text-sm">
                  История чеков ({transactions.length})
                </h3>
              </div>
              <button
                onClick={fetchTransactions}
                title="Обновить"
                className="p-1 text-white/40 hover:text-white transition"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${loadingTxList ? 'animate-spin' : ''}`} />
              </button>
            </div>

            <div className="divide-y divide-white/[0.04] max-h-72 overflow-y-auto">
              {transactions.length === 0 ? (
                <div className="py-6 text-center text-xs text-white/40">Чеков пока нет</div>
              ) : (
                transactions.map((tx) => (
                  <div
                    key={tx.id}
                    onClick={() => {
                      setSelectedTx(tx);
                      setCancelErrorMsg(null);
                      setCancelSuccessMsg(null);
                    }}
                    className="py-2.5 px-2 -mx-2 rounded-xl hover:bg-white/[0.04] cursor-pointer flex items-center justify-between text-xs transition"
                  >
                    <div className="min-w-0">
                      <div className="font-medium text-white truncate text-xs flex items-center space-x-1.5">
                        <span>Чек #{tx.id.substring(0, 8)}</span>
                        {tx.status === 'PROCESSED' || tx.status === 'HANDLED' ? (
                          <span className="text-[9px] px-1.5 py-0.5 rounded-md bg-[#30d158]/20 text-[#30d158] font-medium border border-[#30d158]/30">
                            Оплачен
                          </span>
                        ) : tx.status === 'CANCELLED' ? (
                          <span className="text-[9px] px-1.5 py-0.5 rounded-md bg-[#ff453a]/20 text-[#ff453a] font-medium border border-[#ff453a]/30">
                            Возврат
                          </span>
                        ) : tx.status === 'REJECTED' || tx.status === 'FAILED' ? (
                          <span className="text-[9px] px-1.5 py-0.5 rounded-md bg-[#ff453a]/20 text-[#ff453a] font-medium border border-[#ff453a]/30">
                            Отклонён
                          </span>
                        ) : (
                          <span className="text-[9px] px-1.5 py-0.5 rounded-md bg-white/10 text-white/70 font-medium">
                            {tx.status}
                          </span>
                        )}
                      </div>
                      <div className="text-[10px] text-white/40">
                        {new Date(tx.createdAt).toLocaleDateString('ru-RU', {
                          day: 'numeric',
                          month: 'short',
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                      </div>
                    </div>
                    <div className="text-right flex items-center space-x-1.5 flex-shrink-0">
                      <span className={`font-semibold ${tx.status === 'CANCELLED' ? 'text-white/40 line-through' : 'text-white'}`}>
                        {Number(tx.amount).toFixed(2)} ₽
                      </span>
                      <ChevronRight className="w-3.5 h-3.5 text-white/30" />
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Apple Sheet Receipt Modal */}
      {selectedTx && (
        <div className="fixed inset-0 bg-black/75 backdrop-blur-xl flex items-center justify-center p-4 z-50 animate-in fade-in duration-200">
          <div className="apple-card max-w-sm w-full p-6 space-y-4 border border-white/20 shadow-2xl animate-in zoom-in-95 duration-200">
            <div className="flex items-center justify-between pb-3 border-b border-white/[0.08]">
              <div className="flex items-center space-x-2">
                <Receipt className="w-4 h-4 text-[#2997ff]" />
                <h3 className="font-semibold text-white text-sm">Детали чека</h3>
              </div>
              <button
                onClick={() => setSelectedTx(null)}
                className="w-7 h-7 flex items-center justify-center rounded-full bg-white/10 hover:bg-white/20 text-white text-xs transition"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            </div>

            <div className="space-y-3.5 text-xs">
              <div className="bg-white/[0.04] p-3 rounded-2xl space-y-1 font-mono text-[10px] border border-white/10">
                <div className="flex justify-between text-white/50">
                  <span>ID транзакции:</span>
                  <span className="text-white font-medium">{selectedTx.id.substring(0, 16)}...</span>
                </div>
                <div className="flex justify-between items-center text-white/50">
                  <span>Статус:</span>
                  {selectedTx.status === 'PROCESSED' || selectedTx.status === 'HANDLED' ? (
                    <span className="font-semibold text-[#30d158] bg-[#30d158]/10 px-2 py-0.5 rounded-md border border-[#30d158]/20 text-[10px]">
                      ОПЛАЧЕН
                    </span>
                  ) : selectedTx.status === 'CANCELLED' ? (
                    <span className="font-semibold text-[#ff453a] bg-[#ff453a]/10 px-2 py-0.5 rounded-md border border-[#ff453a]/20 text-[10px] flex items-center space-x-1">
                      <RotateCcw className="w-2.5 h-2.5" />
                      <span>АННУЛИРОВАН</span>
                    </span>
                  ) : (
                    <span className="font-semibold text-white/70">{selectedTx.status}</span>
                  )}
                </div>
              </div>

              <div>
                <span className="font-medium text-white/50 block mb-1.5 text-[11px] uppercase tracking-wider">
                  Позиции ({selectedTx.items?.length || 0}):
                </span>
                <div className="divide-y divide-white/[0.04] border border-white/10 rounded-2xl overflow-hidden max-h-44 overflow-y-auto">
                  {selectedTx.items?.map((item, idx) => (
                    <div key={idx} className="p-2.5 flex justify-between items-center text-[11px]">
                      <div>
                        <div className="font-medium text-white">{item.name}</div>
                        <div className="text-[9px] text-white/40 font-mono">{item.category}</div>
                      </div>
                      <div className="font-semibold text-white">{Number(item.price).toFixed(2)} ₽</div>
                    </div>
                  ))}
                </div>
              </div>

              <div className="pt-3 border-t border-white/[0.08] flex justify-between items-baseline font-semibold text-white">
                <span className="text-white/60">Итого:</span>
                <span className={`text-lg ${selectedTx.status === 'CANCELLED' ? 'text-white/40 line-through' : 'text-[#2997ff]'}`}>
                  {Number(selectedTx.amount).toFixed(2)} ₽
                </span>
              </div>

              {/* Refund Action and Feedback */}
              {(selectedTx.status === 'PROCESSED' || selectedTx.status === 'HANDLED') && (
                <div className="space-y-2 pt-2 border-t border-white/[0.08]">
                  {cancelErrorMsg && (
                    <div className="p-2.5 rounded-xl bg-[#ff453a]/15 text-[#ff453a] text-[11px] flex items-center space-x-1.5 border border-[#ff453a]/20">
                      <AlertCircle className="w-3.5 h-3.5 flex-shrink-0" />
                      <span>{cancelErrorMsg}</span>
                    </div>
                  )}
                  {cancelSuccessMsg && (
                    <div className="p-2.5 rounded-xl bg-[#30d158]/15 text-[#30d158] text-[11px] flex items-center space-x-1.5 border border-[#30d158]/20">
                      <CheckCircle2 className="w-3.5 h-3.5 flex-shrink-0" />
                      <span>{cancelSuccessMsg}</span>
                    </div>
                  )}
                  <button
                    onClick={() => handleCancelTransaction(selectedTx.id)}
                    disabled={cancellingTxId === selectedTx.id}
                    className="w-full py-2.5 bg-[#ff453a]/15 hover:bg-[#ff453a]/25 active:scale-[0.98] text-[#ff453a] font-medium rounded-full text-xs transition border border-[#ff453a]/30 flex items-center justify-center space-x-2"
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
                <div className="p-2.5 bg-white/[0.04] rounded-xl text-[10px] text-white/50 flex items-center space-x-1.5 border border-white/10">
                  <RotateCcw className="w-3.5 h-3.5 flex-shrink-0 text-[#ff453a]" />
                  <span>Чек аннулирован. Кэшбэк отозван или баллы возвращены на счет.</span>
                </div>
              )}
            </div>

            <div className="pt-2">
              <button
                onClick={() => setSelectedTx(null)}
                className="w-full py-2.5 bg-white/10 hover:bg-white/15 text-white font-medium rounded-full text-xs transition"
              >
                Закрыть
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
