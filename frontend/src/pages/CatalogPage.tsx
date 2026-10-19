import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { productApi } from '../api/products';
import type { ProductDTO } from '../types';
import {
  Package,
  ShoppingBag,
  CreditCard,
  Sparkles,
  Coffee,
  ShoppingCart,
  Headphones,
  Shirt,
  Fuel,
  RefreshCw,
  AlertCircle,
  Search,
  ArrowRight,
  ShieldCheck,
  CheckCircle2,
  LogIn,
  UserPlus,
  Plus,
  Edit2,
  Trash2,
  Power,
  X,
  User,
} from 'lucide-react';

const getCategoryIcon = (category: string) => {
  const cat = category.toLowerCase();
  if (cat.includes('cafe') || cat.includes('кофе') || cat.includes('кафе')) {
    return <Coffee className="w-4 h-4 text-[#ff9f0a]" />;
  }
  if (cat.includes('groc') || cat.includes('продукт')) {
    return <ShoppingCart className="w-4 h-4 text-[#30d158]" />;
  }
  if (cat.includes('elect') || cat.includes('электрон')) {
    return <Headphones className="w-4 h-4 text-[#0a84ff]" />;
  }
  if (cat.includes('apparel') || cat.includes('одежд')) {
    return <Shirt className="w-4 h-4 text-[#bf5af2]" />;
  }
  if (cat.includes('auto') || cat.includes('авто')) {
    return <Fuel className="w-4 h-4 text-[#ff453a]" />;
  }
  return <ShoppingBag className="w-4 h-4 text-[#2997ff]" />;
};

const getCategoryBadgeClass = (category: string) => {
  const cat = category.toLowerCase();
  if (cat.includes('cafe')) return 'bg-[#ff9f0a]/10 text-[#ff9f0a] border-[#ff9f0a]/20';
  if (cat.includes('groc')) return 'bg-[#30d158]/10 text-[#30d158] border-[#30d158]/20';
  if (cat.includes('elect')) return 'bg-[#0a84ff]/10 text-[#0a84ff] border-[#0a84ff]/20';
  if (cat.includes('apparel')) return 'bg-[#bf5af2]/10 text-[#bf5af2] border-[#bf5af2]/20';
  if (cat.includes('auto')) return 'bg-[#ff453a]/10 text-[#ff453a] border-[#ff453a]/20';
  return 'bg-white/10 text-white/80 border-white/10';
};

export const CatalogPage: React.FC = () => {
  const { isAuthenticated, user, userId, isAdmin } = useAuth();
  const navigate = useNavigate();

  const [products, setProducts] = useState<ProductDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [actionMsg, setActionMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Search & Filters
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');
  const [onlyMyProducts, setOnlyMyProducts] = useState(false);

  // Modal State: Create Product (Available for ALL authenticated users!)
  const [showAddModal, setShowAddModal] = useState(false);
  const [newTitle, setNewTitle] = useState('');
  const [newDescription, setNewDescription] = useState('');
  const [newCategory, setNewCategory] = useState('cafe');
  const [newPrice, setNewPrice] = useState('');
  const [submittingProduct, setSubmittingProduct] = useState(false);

  // Modal State: Edit Product (For owners or admins)
  const [editingProduct, setEditingProduct] = useState<ProductDTO | null>(null);
  const [editTitle, setEditTitle] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [editPrice, setEditPrice] = useState('');
  const [savingEdit, setSavingEdit] = useState(false);

  const loadProducts = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await productApi.getAllProducts();
      setProducts(data);
    } catch (err: any) {
      console.error('Failed to load catalog products', err);
      setError('Не удалось загрузить каталог товаров. Пожалуйста, попробуйте позже.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadProducts();
  }, [loadProducts]);

  // Handle Pay / Buy Action
  const handleBuyProduct = (product: ProductDTO) => {
    if (!isAuthenticated) {
      // Redirect unauthorized user to login form with target product
      navigate('/login', {
        state: {
          from: '/checkout',
          selectedProduct: product,
          message: `Для покупки и оплаты товара «${product.title}» (${product.price} ₽) необходимо войти в систему или зарегистрироваться.`,
        },
      });
      return;
    }

    // Authenticated user: navigate to checkout with pre-selected item
    navigate('/checkout', {
      state: {
        selectedProduct: product,
      },
    });
  };

  // Create Product: Available for any authenticated user
  const handleCreateProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    setActionMsg(null);

    if (!userId) {
      setActionMsg({ type: 'error', text: 'Ошибка авторизации: выполните вход' });
      return;
    }

    if (newTitle.trim().length < 10 || newTitle.trim().length > 50) {
      setActionMsg({ type: 'error', text: 'Название товара должно содержать от 10 до 50 символов' });
      return;
    }

    const desc = newDescription.trim() || 'Качественный товар из каталога программы лояльности';
    if (desc.length < 10 || desc.length > 200) {
      setActionMsg({ type: 'error', text: 'Описание товара должно содержать от 10 до 200 символов' });
      return;
    }

    const price = parseFloat(newPrice);
    if (isNaN(price) || price < 0) {
      setActionMsg({ type: 'error', text: 'Укажите корректную положительную цену' });
      return;
    }

    setSubmittingProduct(true);
    try {
      await productApi.createProduct({
        userId,
        title: newTitle.trim(),
        description: desc,
        category: newCategory.trim().toLowerCase(),
        price,
      });

      setActionMsg({ type: 'success', text: `Товар «${newTitle}» успешно добавлен в каталог!` });
      setShowAddModal(false);
      setNewTitle('');
      setNewDescription('');
      setNewPrice('');
      setNewCategory('cafe');
      loadProducts();
    } catch (err: any) {
      console.error('Failed to create product', err);
      const msg = err.response?.data?.message || err.response?.data || 'Ошибка создания товара';
      setActionMsg({ type: 'error', text: typeof msg === 'string' ? msg : 'Не удалось создать товар' });
    } finally {
      setSubmittingProduct(false);
    }
  };

  // Toggle Status: Activate / Stop (owner or admin)
  const handleToggleStatus = async (product: ProductDTO) => {
    if (!userId) return;
    setActionMsg(null);
    try {
      if (product.status === 'ACTIVE') {
        await productApi.stopProduct({ userId, productId: product.productId });
        setActionMsg({ type: 'success', text: `Товар «${product.title}» приостановлен` });
      } else {
        await productApi.activateProduct({ userId, productId: product.productId });
        setActionMsg({ type: 'success', text: `Товар «${product.title}» активирован` });
      }
      loadProducts();
    } catch (err: any) {
      console.error('Failed to toggle status', err);
      setActionMsg({ type: 'error', text: 'Ошибка изменения статуса товара' });
    }
  };

  // Delete product (owner or admin)
  const handleDeleteProduct = async (product: ProductDTO) => {
    if (!confirm(`Вы действительно хотите удалить товар «${product.title}»?`)) return;
    setActionMsg(null);
    try {
      await productApi.deleteProduct(product.productId);
      setActionMsg({ type: 'success', text: `Товар «${product.title}» удален` });
      loadProducts();
    } catch (err: any) {
      console.error('Failed to delete product', err);
      setActionMsg({ type: 'error', text: 'Не удалось удалить товар' });
    }
  };

  // Edit product
  const openEditModal = (product: ProductDTO) => {
    setEditingProduct(product);
    setEditTitle(product.title);
    setEditDescription(product.description || '');
    setEditPrice(product.price.toString());
  };

  const handleUpdateProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingProduct || !userId) return;
    setActionMsg(null);

    if (editTitle.trim().length < 10 || editTitle.trim().length > 50) {
      setActionMsg({ type: 'error', text: 'Название товара должно содержать от 10 до 50 символов' });
      return;
    }

    const price = parseFloat(editPrice);
    if (isNaN(price) || price < 0) {
      setActionMsg({ type: 'error', text: 'Укажите корректную положительную цену' });
      return;
    }

    setSavingEdit(true);
    try {
      await productApi.updateProduct(editingProduct.productId, {
        userId,
        title: editTitle.trim(),
        description: editDescription.trim() || undefined,
        price,
      });

      setActionMsg({ type: 'success', text: `Товар «${editTitle}» успешно обновлен!` });
      setEditingProduct(null);
      loadProducts();
    } catch (err: any) {
      console.error('Failed to update product', err);
      setActionMsg({ type: 'error', text: 'Не удалось обновить товар' });
    } finally {
      setSavingEdit(false);
    }
  };

  // Count user's own products
  const myProductsCount = products.filter((p) => p.userId === userId).length;

  // Distinct categories
  const categories = [
    'ALL',
    ...Array.from(new Set(products.map((p) => p.category.toLowerCase()))),
  ];

  const filteredProducts = products.filter((p) => {
    const isOwner = userId && p.userId === userId;

    // If filtering by "Only My Products"
    if (onlyMyProducts) {
      if (!isOwner) return false;
    } else {
      // In general storefront, show active items OR user's own items
      if (p.status !== 'ACTIVE' && !isOwner && !isAdmin) return false;
    }

    const matchesCategory =
      selectedCategory === 'ALL' || p.category.toLowerCase() === selectedCategory.toLowerCase();
    const matchesSearch =
      searchQuery.trim() === '' ||
      p.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (p.description && p.description.toLowerCase().includes(searchQuery.toLowerCase())) ||
      p.category.toLowerCase().includes(searchQuery.toLowerCase());

    return matchesCategory && matchesSearch;
  });

  return (
    <div className="min-h-screen bg-[#0d0d0f] py-8 sm:py-12 px-4 sm:px-6 lg:px-8 pb-32 md:pb-16 max-w-7xl mx-auto space-y-8">
      {/* Top Hero Section */}
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-b from-white/[0.08] to-white/[0.02] border border-white/10 p-6 sm:p-10 shadow-2xl">
        <div className="relative z-10 max-w-3xl space-y-4">
          <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-white/[0.06] border border-white/10 text-xs font-medium text-white/80">
            <Sparkles className="w-3.5 h-3.5 text-[#ffd60a]" />
            <span>ProductService • Витрина товаров</span>
          </div>

          <h1 className="text-3xl sm:text-5xl font-bold tracking-tight text-white leading-tight">
            Каталог товаров <br />
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-[#2997ff] via-[#64d2ff] to-[#30d158]">
              с кэшбэком баллами
            </span>
          </h1>

          <p className="text-sm sm:text-base text-white/60 leading-relaxed max-w-2xl">
            Выбирайте любимые напитки, продукты и электронику. Любой авторизованный пользователь
            может выставить свой товар на продажу и получать баллы!
          </p>

          {/* User / Guest Banner */}
          {!isAuthenticated ? (
            <div className="pt-2 flex flex-col sm:flex-row items-start sm:items-center gap-3">
              <div className="px-4 py-2.5 rounded-2xl bg-white/[0.04] border border-white/10 text-xs text-white/70 flex items-center space-x-2.5">
                <ShieldCheck className="w-4 h-4 text-[#2997ff] flex-shrink-0" />
                <span>
                  Гостевой режим: при оплате вы сможете войти или зарегистрироваться за пару секунд
                </span>
              </div>
              <div className="flex items-center space-x-2">
                <Link
                  to="/login"
                  className="px-4 py-2 bg-white/10 hover:bg-white/20 text-white rounded-full text-xs font-semibold flex items-center space-x-1.5 transition"
                >
                  <LogIn className="w-3.5 h-3.5" />
                  <span>Войти</span>
                </Link>
                <Link
                  to="/register"
                  className="px-4 py-2 bg-white hover:bg-white/90 text-black rounded-full text-xs font-semibold flex items-center space-x-1.5 transition shadow-sm"
                >
                  <UserPlus className="w-3.5 h-3.5" />
                  <span>Регистрация</span>
                </Link>
              </div>
            </div>
          ) : (
            <div className="pt-2 flex flex-wrap items-center gap-3">
              <button
                onClick={() => setShowAddModal(true)}
                className="px-5 py-2.5 bg-white hover:bg-white/90 text-black rounded-full text-xs font-semibold flex items-center space-x-2 transition shadow-md active:scale-95"
              >
                <Plus className="w-4 h-4 text-[#0071e3]" />
                <span>+ Добавить свой товар</span>
              </button>

              <div className="flex items-center space-x-2 text-xs text-[#30d158] bg-[#30d158]/10 border border-[#30d158]/20 px-3.5 py-2 rounded-full">
                <CheckCircle2 className="w-4 h-4 flex-shrink-0" />
                <span>
                  Вы авторизованы как {user?.firstName || user?.email}
                </span>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Action Notification Message */}
      {actionMsg && (
        <div
          className={`p-4 rounded-2xl text-xs flex items-center space-x-2.5 animate-in fade-in duration-200 ${
            actionMsg.type === 'success'
              ? 'bg-[#30d158]/15 text-[#30d158] border border-[#30d158]/30'
              : 'bg-[#ff453a]/15 text-[#ff453a] border border-[#ff453a]/30'
          }`}
        >
          {actionMsg.type === 'success' ? (
            <CheckCircle2 className="w-4 h-4 flex-shrink-0" />
          ) : (
            <AlertCircle className="w-4 h-4 flex-shrink-0" />
          )}
          <span className="font-medium text-sm">{actionMsg.text}</span>
        </div>
      )}

      {/* Search & Categories Bar */}
      <div className="flex flex-col md:flex-row items-stretch md:items-center justify-between gap-4">
        {/* Search */}
        <div className="relative flex-1 max-w-md">
          <Search className="w-4 h-4 text-white/40 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder="Поиск по названию или категории..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2.5 bg-white/[0.04] border border-white/10 rounded-2xl text-xs sm:text-sm text-white placeholder-white/30 focus:border-[#2997ff] focus:ring-1 focus:ring-[#2997ff]/40 focus:outline-none transition"
          />
        </div>

        {/* Categories Pills & My Products Toggle */}
        <div className="flex items-center space-x-2 overflow-x-auto pb-1 max-w-full">
          {isAuthenticated && (
            <button
              onClick={() => setOnlyMyProducts(!onlyMyProducts)}
              className={`px-3.5 py-1.5 rounded-full text-xs font-medium whitespace-nowrap transition-all duration-200 flex items-center space-x-1.5 ${
                onlyMyProducts
                  ? 'bg-[#2997ff] text-white font-semibold shadow-sm'
                  : 'bg-white/[0.04] text-white/60 hover:text-white hover:bg-white/[0.08] border border-white/10'
              }`}
            >
              <User className="w-3.5 h-3.5" />
              <span>Мои товары ({myProductsCount})</span>
            </button>
          )}

          {categories.map((cat) => {
            const isSelected = !onlyMyProducts && selectedCategory === cat;
            return (
              <button
                key={cat}
                onClick={() => {
                  setOnlyMyProducts(false);
                  setSelectedCategory(cat);
                }}
                className={`px-3.5 py-1.5 rounded-full text-xs font-medium whitespace-nowrap transition-all duration-200 ${
                  isSelected
                    ? 'bg-white text-black font-semibold shadow-sm'
                    : 'bg-white/[0.04] text-white/60 hover:text-white hover:bg-white/[0.08] border border-white/5'
                }`}
              >
                {cat === 'ALL' ? 'Все товары' : cat}
              </button>
            );
          })}

          <button
            onClick={loadProducts}
            title="Обновить каталог"
            className="p-2 bg-white/[0.04] hover:bg-white/[0.08] border border-white/5 text-white/60 hover:text-white rounded-full transition flex-shrink-0"
          >
            <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          </button>
        </div>
      </div>

      {/* Error Notice */}
      {error && (
        <div className="p-4 rounded-2xl bg-[#ff453a]/15 border border-[#ff453a]/30 text-[#ff453a] text-xs flex items-center space-x-2.5">
          <AlertCircle className="w-4 h-4 flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}

      {/* Products Grid */}
      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {[1, 2, 3, 4, 5, 6].map((i) => (
            <div
              key={i}
              className="apple-card p-6 border border-white/10 space-y-4 animate-pulse"
            >
              <div className="h-4 bg-white/10 rounded w-1/3" />
              <div className="h-6 bg-white/10 rounded w-3/4" />
              <div className="h-12 bg-white/5 rounded" />
              <div className="h-8 bg-white/10 rounded-full w-full" />
            </div>
          ))}
        </div>
      ) : filteredProducts.length === 0 ? (
        <div className="apple-card p-12 text-center space-y-3">
          <div className="w-12 h-12 rounded-2xl bg-white/5 border border-white/10 text-white/40 flex items-center justify-center mx-auto">
            <Package className="w-6 h-6" />
          </div>
          <h3 className="text-base font-semibold text-white">
            {onlyMyProducts ? 'У вас пока нет созданных товаров' : 'Товары не найдены'}
          </h3>
          <p className="text-xs text-white/50 max-w-sm mx-auto">
            {onlyMyProducts ? (
              'Вы можете выставить свой товар на витрину прямо сейчас!'
            ) : products.length === 0 ? (
              'В данный момент каталог пуст. Вы можете добавить первую позицию.'
            ) : (
              'По вашему запросу ничего не нашлось. Попробуйте изменить параметры поиска.'
            )}
          </p>
          {isAuthenticated && (
            <button
              onClick={() => setShowAddModal(true)}
              className="mt-2 px-5 py-2 bg-white hover:bg-white/90 text-black font-semibold rounded-full text-xs transition"
            >
              + Добавить товар
            </button>
          )}
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredProducts.map((product) => {
            const isOwner = userId && product.userId === userId;
            const canManage = isOwner || isAdmin;
            const isActive = product.status === 'ACTIVE';

            return (
              <div
                key={product.productId}
                className="apple-card p-6 sm:p-7 border border-white/10 flex flex-col justify-between hover:border-white/20 transition-all duration-200 group relative overflow-hidden"
              >
                {/* Glow accent */}
                <div className="absolute -top-12 -right-12 w-28 h-28 bg-[#2997ff]/10 rounded-full blur-2xl group-hover:bg-[#2997ff]/20 transition-all duration-300" />

                <div className="space-y-3.5 relative z-10">
                  {/* Category Pill and Owner badges */}
                  <div className="flex items-center justify-between">
                    <span
                      className={`inline-flex items-center space-x-1.5 px-3 py-1 rounded-full text-[11px] font-medium border ${getCategoryBadgeClass(
                        product.category
                      )}`}
                    >
                      {getCategoryIcon(product.category)}
                      <span className="capitalize">{product.category}</span>
                    </span>

                    <div className="flex items-center space-x-1.5">
                      {isOwner && (
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-medium bg-[#2997ff]/20 text-[#2997ff] border border-[#2997ff]/30">
                          Ваш товар
                        </span>
                      )}
                      {!isActive && (
                        <span className="px-2 py-0.5 rounded-full text-[10px] font-medium bg-[#ff9f0a]/20 text-[#ff9f0a] border border-[#ff9f0a]/30">
                          Стоп
                        </span>
                      )}
                      {isActive && (
                        <span className="text-[10px] text-[#30d158] font-medium flex items-center space-x-1">
                          <Sparkles className="w-3 h-3" />
                          <span>кэшбэк</span>
                        </span>
                      )}
                    </div>
                  </div>

                  {/* Title & Description */}
                  <div>
                    <h3 className="text-lg font-semibold text-white tracking-tight group-hover:text-[#2997ff] transition-colors duration-200">
                      {product.title}
                    </h3>
                    {product.description && (
                      <p className="text-xs text-white/50 line-clamp-2 mt-1.5 leading-relaxed">
                        {product.description}
                      </p>
                    )}
                  </div>
                </div>

                {/* Price & Action Button */}
                <div className="pt-6 mt-4 border-t border-white/[0.06] flex items-center justify-between relative z-10">
                  <div>
                    <span className="text-[10px] uppercase tracking-wider text-white/40 block">
                      Стоимость
                    </span>
                    <span className="text-xl sm:text-2xl font-bold text-white tracking-tight">
                      {product.price.toLocaleString('ru-RU')} ₽
                    </span>
                  </div>

                  {/* Action: Owner Management OR Pay Button */}
                  {canManage ? (
                    <div className="flex items-center space-x-1.5">
                      <button
                        onClick={() => handleToggleStatus(product)}
                        className={`p-2 rounded-xl border text-xs transition active:scale-95 ${
                          isActive
                            ? 'bg-[#ff9f0a]/10 hover:bg-[#ff9f0a]/20 text-[#ff9f0a] border-[#ff9f0a]/20'
                            : 'bg-[#30d158]/10 hover:bg-[#30d158]/20 text-[#30d158] border-[#30d158]/20'
                        }`}
                        title={isActive ? 'Остановить товар' : 'Активировать товар'}
                      >
                        <Power className="w-3.5 h-3.5" />
                      </button>

                      <button
                        onClick={() => openEditModal(product)}
                        className="p-2 rounded-xl border border-white/10 bg-white/[0.04] hover:bg-white/[0.08] text-white/70 hover:text-white transition active:scale-95"
                        title="Редактировать товар"
                      >
                        <Edit2 className="w-3.5 h-3.5" />
                      </button>

                      <button
                        onClick={() => handleDeleteProduct(product)}
                        className="p-2 rounded-xl border border-[#ff453a]/20 bg-[#ff453a]/10 hover:bg-[#ff453a]/20 text-[#ff453a] transition active:scale-95"
                        title="Удалить товар"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  ) : (
                    <button
                      onClick={() => handleBuyProduct(product)}
                      className="px-4 py-2.5 bg-white hover:bg-white/90 text-black rounded-full text-xs font-semibold flex items-center space-x-1.5 transition-all duration-200 active:scale-[0.97] shadow-md group-hover:shadow-lg"
                    >
                      <CreditCard className="w-3.5 h-3.5" />
                      <span>Оплатить</span>
                      <ArrowRight className="w-3 h-3 text-black/60 group-hover:translate-x-0.5 transition-transform" />
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Modal: Create Product (Any authenticated user!) */}
      {showAddModal && (
        <div className="fixed inset-0 bg-black/75 backdrop-blur-xl flex items-center justify-center p-4 z-50 animate-in fade-in duration-200">
          <div className="apple-card max-w-md w-full p-6 sm:p-7 border border-white/20 shadow-2xl animate-in zoom-in-95 duration-200 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-white/[0.08]">
              <div className="flex items-center space-x-2">
                <Package className="w-5 h-5 text-[#2997ff]" />
                <h3 className="font-semibold text-white text-base">Добавить новый товар</h3>
              </div>
              <button
                onClick={() => setShowAddModal(false)}
                className="w-7 h-7 flex items-center justify-center rounded-full bg-white/10 hover:bg-white/20 text-white text-xs transition"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            </div>

            <form onSubmit={handleCreateProduct} className="space-y-4 text-xs">
              <div>
                <label className="block text-[11px] uppercase tracking-wider font-medium text-white/50 mb-1.5">
                  Название товара (от 10 до 50 символов)
                </label>
                <input
                  type="text"
                  required
                  minLength={10}
                  maxLength={50}
                  placeholder="напр. Капучино Grande 400мл"
                  value={newTitle}
                  onChange={(e) => setNewTitle(e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-white/[0.04] border border-white/10 rounded-xl text-xs text-white placeholder-white/30 focus:border-[#2997ff] focus:ring-1 focus:ring-[#2997ff]/40 focus:outline-none transition"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-[11px] uppercase tracking-wider font-medium text-white/50 mb-1.5">
                    Категория
                  </label>
                  <select
                    value={newCategory}
                    onChange={(e) => setNewCategory(e.target.value)}
                    className="w-full px-3 py-2.5 bg-[#16161a] border border-white/10 rounded-xl text-xs text-white focus:border-[#2997ff] focus:outline-none transition"
                  >
                    <option value="cafe">cafe (Кофе / Рестораны)</option>
                    <option value="groceries">groceries (Продукты)</option>
                    <option value="electronics">electronics (Электроника)</option>
                    <option value="apparel">apparel (Одежда)</option>
                    <option value="auto">auto (Авто / АЗС)</option>
                  </select>
                </div>

                <div>
                  <label className="block text-[11px] uppercase tracking-wider font-medium text-white/50 mb-1.5">
                    Цена (₽)
                  </label>
                  <div className="relative">
                    <input
                      type="number"
                      step="1"
                      min="0"
                      required
                      placeholder="350"
                      value={newPrice}
                      onChange={(e) => setNewPrice(e.target.value)}
                      className="w-full pl-3.5 pr-8 py-2.5 bg-white/[0.04] border border-white/10 rounded-xl text-xs text-white focus:border-[#2997ff] focus:ring-1 focus:ring-[#2997ff]/40 focus:outline-none transition"
                    />
                    <span className="absolute right-3.5 top-2.5 text-white/40 font-medium">₽</span>
                  </div>
                </div>
              </div>

              <div>
                <label className="block text-[11px] uppercase tracking-wider font-medium text-white/50 mb-1.5">
                  Описание товара (от 10 до 200 символов)
                </label>
                <textarea
                  rows={3}
                  minLength={10}
                  maxLength={200}
                  placeholder="напр. Свежесваренный авторский кофе из отборных зерен арабики"
                  value={newDescription}
                  onChange={(e) => setNewDescription(e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-white/[0.04] border border-white/10 rounded-xl text-xs text-white placeholder-white/30 focus:border-[#2997ff] focus:ring-1 focus:ring-[#2997ff]/40 focus:outline-none transition resize-none"
                />
              </div>

              <div className="pt-3 flex space-x-3">
                <button
                  type="button"
                  onClick={() => setShowAddModal(false)}
                  className="w-1/2 py-2.5 bg-white/10 hover:bg-white/15 text-white font-medium rounded-full text-xs transition"
                >
                  Отмена
                </button>
                <button
                  type="submit"
                  disabled={submittingProduct}
                  className="w-1/2 py-2.5 bg-white hover:bg-white/90 text-black font-semibold rounded-full text-xs transition shadow-sm disabled:opacity-50"
                >
                  {submittingProduct ? 'Создание...' : 'Создать товар'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal: Edit Product */}
      {editingProduct && (
        <div className="fixed inset-0 bg-black/75 backdrop-blur-xl flex items-center justify-center p-4 z-50 animate-in fade-in duration-200">
          <div className="apple-card max-w-md w-full p-6 sm:p-7 border border-white/20 shadow-2xl animate-in zoom-in-95 duration-200 space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-white/[0.08]">
              <div className="flex items-center space-x-2">
                <Edit2 className="w-5 h-5 text-[#2997ff]" />
                <h3 className="font-semibold text-white text-base">Редактирование товара</h3>
              </div>
              <button
                onClick={() => setEditingProduct(null)}
                className="w-7 h-7 flex items-center justify-center rounded-full bg-white/10 hover:bg-white/20 text-white text-xs transition"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            </div>

            <form onSubmit={handleUpdateProduct} className="space-y-4 text-xs">
              <div>
                <label className="block text-[11px] uppercase tracking-wider font-medium text-white/50 mb-1.5">
                  Название товара (от 10 до 50 символов)
                </label>
                <input
                  type="text"
                  required
                  minLength={10}
                  maxLength={50}
                  value={editTitle}
                  onChange={(e) => setEditTitle(e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-white/[0.04] border border-white/10 rounded-xl text-xs text-white placeholder-white/30 focus:border-[#2997ff] focus:ring-1 focus:ring-[#2997ff]/40 focus:outline-none transition"
                />
              </div>

              <div>
                <label className="block text-[11px] uppercase tracking-wider font-medium text-white/50 mb-1.5">
                  Цена (₽)
                </label>
                <div className="relative">
                  <input
                    type="number"
                    step="1"
                    min="0"
                    required
                    value={editPrice}
                    onChange={(e) => setEditPrice(e.target.value)}
                    className="w-full pl-3.5 pr-8 py-2.5 bg-white/[0.04] border border-white/10 rounded-xl text-xs text-white focus:border-[#2997ff] focus:ring-1 focus:ring-[#2997ff]/40 focus:outline-none transition"
                  />
                  <span className="absolute right-3.5 top-2.5 text-white/40 font-medium">₽</span>
                </div>
              </div>

              <div>
                <label className="block text-[11px] uppercase tracking-wider font-medium text-white/50 mb-1.5">
                  Описание товара (от 10 до 200 символов)
                </label>
                <textarea
                  rows={3}
                  minLength={10}
                  maxLength={200}
                  value={editDescription}
                  onChange={(e) => setEditDescription(e.target.value)}
                  className="w-full px-3.5 py-2.5 bg-white/[0.04] border border-white/10 rounded-xl text-xs text-white placeholder-white/30 focus:border-[#2997ff] focus:ring-1 focus:ring-[#2997ff]/40 focus:outline-none transition resize-none"
                />
              </div>

              <div className="pt-3 flex space-x-3">
                <button
                  type="button"
                  onClick={() => setEditingProduct(null)}
                  className="w-1/2 py-2.5 bg-white/10 hover:bg-white/15 text-white font-medium rounded-full text-xs transition"
                >
                  Отмена
                </button>
                <button
                  type="submit"
                  disabled={savingEdit}
                  className="w-1/2 py-2.5 bg-white hover:bg-white/90 text-black font-semibold rounded-full text-xs transition shadow-sm disabled:opacity-50"
                >
                  {savingEdit ? 'Сохранение...' : 'Сохранить'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
