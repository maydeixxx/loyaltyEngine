import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Wallet, Percent, ShoppingBag, Shield, User, LogOut, Sparkles, Package } from 'lucide-react';

export const Navbar: React.FC = () => {
  const { user, isAdmin, logout, isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    const isCatalog =
      location.pathname === '/' ||
      location.pathname === '/catalog' ||
      location.pathname === '/products';

    return (
      <nav className="sticky top-0 z-40 apple-glass transition-all">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            {/* Logo */}
            <Link to="/" className="flex items-center space-x-3 group">
              <div className="w-9 h-9 rounded-2xl bg-gradient-to-tr from-[#1d1d1f] to-[#2c2c2e] border border-white/15 text-white flex items-center justify-center shadow-lg group-hover:scale-105 transition-transform duration-200">
                <Sparkles className="w-5 h-5 text-[#2997ff]" />
              </div>
              <div className="flex items-baseline space-x-2">
                <span className="font-semibold text-base sm:text-lg text-white tracking-tight">
                  Loyalty<span className="text-[#2997ff]">Engine</span>
                </span>
                <span className="text-[10px] bg-white/10 text-white/70 font-medium px-2 py-0.5 rounded-full border border-white/10">
                  Store
                </span>
              </div>
            </Link>

            {/* Desktop Navigation */}
            <div className="hidden sm:flex items-center space-x-1 bg-white/[0.06] p-1 rounded-full border border-white/[0.08] backdrop-blur-xl">
              <Link
                to="/"
                className={`flex items-center space-x-2 px-4 py-1.5 rounded-full text-xs font-medium transition-all duration-200 ${
                  isCatalog
                    ? 'bg-white/20 text-white shadow-[inset_0_1px_0_rgba(255,255,255,0.25)] font-semibold border border-white/10'
                    : 'text-white/60 hover:text-white hover:bg-white/5'
                }`}
              >
                <Package className="w-3.5 h-3.5 text-[#2997ff]" />
                <span>Все товары</span>
              </Link>
            </div>

            {/* Auth Buttons */}
            <div className="flex items-center space-x-2 sm:space-x-3">
              <Link
                to="/login"
                className="px-4 py-1.5 rounded-full text-xs font-medium text-white/80 hover:text-white hover:bg-white/10 transition duration-200"
              >
                Войти
              </Link>
              <Link
                to="/register"
                className="px-4 py-1.5 rounded-full text-xs font-semibold bg-white hover:bg-white/90 text-black shadow-sm transition duration-200 active:scale-95"
              >
                Регистрация
              </Link>
            </div>
          </div>
        </div>
      </nav>
    );
  }

  const isActive = (path: string) => {
    if (path === '/wallet' && (location.pathname === '/wallet' || location.pathname === '/')) return true;
    return location.pathname === path;
  };

  return (
    <>
      {/* Top Header (macOS / VisionOS Frosted Glass Bar) */}
      <nav className="sticky top-0 z-40 apple-glass transition-all">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-16">
            {/* Logo: Titanium Apple Loyalty Engine */}
            <Link to="/" className="flex items-center space-x-3 group">
              <div className="w-9 h-9 rounded-2xl bg-gradient-to-tr from-[#1d1d1f] to-[#2c2c2e] border border-white/15 text-white flex items-center justify-center shadow-lg group-hover:scale-105 transition-transform duration-200">
                <Sparkles className="w-5 h-5 text-[#2997ff]" />
              </div>
              <div className="flex items-baseline space-x-2">
                <span className="font-semibold text-base sm:text-lg text-white tracking-tight">
                  Loyalty<span className="text-[#2997ff]">Engine</span>
                </span>
                <span className="text-[10px] bg-white/10 text-white/70 font-medium px-2 py-0.5 rounded-full border border-white/10">
                  Wallet
                </span>
              </div>
            </Link>

            {/* Desktop Navigation Tabs (Apple Fluid Segmented Bar) */}
            <div className="hidden md:flex items-center space-x-1 bg-white/[0.06] p-1 rounded-full border border-white/[0.08] backdrop-blur-xl">
              <Link
                to="/"
                className={`flex items-center space-x-2 px-4 py-1.5 rounded-full text-xs font-medium transition-all duration-200 ${
                  isActive('/')
                    ? 'bg-white/20 text-white shadow-[inset_0_1px_0_rgba(255,255,255,0.25)] font-semibold border border-white/10'
                    : 'text-white/60 hover:text-white hover:bg-white/5'
                }`}
              >
                <Wallet className="w-3.5 h-3.5" />
                <span>1. Кошелек</span>
              </Link>

              <Link
                to="/rules"
                className={`flex items-center space-x-2 px-4 py-1.5 rounded-full text-xs font-medium transition-all duration-200 ${
                  isActive('/rules')
                    ? 'bg-white/20 text-white shadow-[inset_0_1px_0_rgba(255,255,255,0.25)] font-semibold border border-white/10'
                    : 'text-white/60 hover:text-white hover:bg-white/5'
                }`}
              >
                <Percent className="w-3.5 h-3.5" />
                <span>2. Правила кэшбэка</span>
              </Link>

              <Link
                to="/catalog"
                className={`flex items-center space-x-2 px-4 py-1.5 rounded-full text-xs font-medium transition-all duration-200 ${
                  isActive('/catalog')
                    ? 'bg-white/20 text-white shadow-[inset_0_1px_0_rgba(255,255,255,0.25)] font-semibold border border-white/10'
                    : 'text-white/60 hover:text-white hover:bg-white/5'
                }`}
              >
                <Package className="w-3.5 h-3.5" />
                <span>3. Каталог</span>
              </Link>

              <Link
                to="/checkout"
                className={`flex items-center space-x-2 px-4 py-1.5 rounded-full text-xs font-medium transition-all duration-200 ${
                  isActive('/checkout')
                    ? 'bg-white/20 text-white shadow-[inset_0_1px_0_rgba(255,255,255,0.25)] font-semibold border border-white/10'
                    : 'text-white/60 hover:text-white hover:bg-white/5'
                }`}
              >
                <ShoppingBag className="w-3.5 h-3.5" />
                <span>4. Касса & Оплата</span>
              </Link>

              {isAdmin && (
                <Link
                  to="/admin"
                  className={`flex items-center space-x-2 px-4 py-1.5 rounded-full text-xs font-medium transition-all duration-200 ${
                    isActive('/admin')
                      ? 'bg-[#ff9500]/25 text-[#ff9500] font-semibold border border-[#ff9500]/30'
                      : 'text-white/60 hover:text-[#ff9500] hover:bg-white/5'
                  }`}
                >
                  <Shield className="w-3.5 h-3.5" />
                  <span>Управление</span>
                </Link>
              )}
            </div>

            {/* User Profile & Logout */}
            <div className="flex items-center space-x-2.5">
              <Link
                to="/profile"
                className={`flex items-center space-x-2 px-3 py-1.5 rounded-full text-xs transition-all duration-200 ${
                  isActive('/profile')
                    ? 'bg-white/15 text-white border border-white/10'
                    : 'text-white/60 hover:text-white hover:bg-white/5'
                }`}
              >
                <div className="w-6 h-6 rounded-full bg-gradient-to-tr from-blue-500 to-purple-600 text-white flex items-center justify-center font-semibold text-[11px] shadow-sm">
                  {user?.firstName ? user.firstName[0].toUpperCase() : <User className="w-3.5 h-3.5" />}
                </div>
                <div className="hidden sm:block text-left">
                  <div className="text-xs font-medium text-white leading-tight">
                    {user?.firstName ? `${user.firstName} ${user.lastName || ''}` : user?.email}
                  </div>
                  <div className="text-[10px] text-white/50">
                    {isAdmin ? <span className="text-[#ff9500]">Администратор</span> : 'Клиент'}
                  </div>
                </div>
              </Link>

              <button
                onClick={logout}
                title="Выйти"
                className="p-2 text-white/40 hover:text-[#ff453a] hover:bg-[#ff453a]/10 rounded-full transition-all duration-200 active:scale-95"
              >
                <LogOut className="w-4 h-4" />
              </button>
            </div>
          </div>
        </div>
      </nav>

      {/* Mobile Floating iOS Glass Dock */}
      <div className="md:hidden fixed bottom-3 left-3 right-3 z-50 bg-[#18181b]/85 backdrop-blur-2xl border border-white/15 rounded-full px-4 py-2 flex justify-around items-center shadow-2xl">
        <Link
          to="/"
          className={`flex flex-col items-center py-1 px-3 rounded-full transition-all duration-200 ${
            isActive('/') ? 'text-[#2997ff] font-semibold scale-105' : 'text-white/50 hover:text-white'
          }`}
        >
          <Wallet className="w-4 h-4 mb-0.5" />
          <span className="text-[9px]">Кошелек</span>
        </Link>

        <Link
          to="/rules"
          className={`flex flex-col items-center py-1 px-3 rounded-full transition-all duration-200 ${
            isActive('/rules') ? 'text-[#2997ff] font-semibold scale-105' : 'text-white/50 hover:text-white'
          }`}
        >
          <Percent className="w-4 h-4 mb-0.5" />
          <span className="text-[9px]">Правила</span>
        </Link>

        <Link
          to="/catalog"
          className={`flex flex-col items-center py-1 px-3 rounded-full transition-all duration-200 ${
            isActive('/catalog') ? 'text-[#2997ff] font-semibold scale-105' : 'text-white/50 hover:text-white'
          }`}
        >
          <Package className="w-4 h-4 mb-0.5" />
          <span className="text-[9px]">Каталог</span>
        </Link>

        <Link
          to="/checkout"
          className={`flex flex-col items-center py-1 px-3 rounded-full transition-all duration-200 ${
            isActive('/checkout') ? 'text-[#2997ff] font-semibold scale-105' : 'text-white/50 hover:text-white'
          }`}
        >
          <ShoppingBag className="w-4 h-4 mb-0.5" />
          <span className="text-[9px]">Оплата</span>
        </Link>

        {isAdmin && (
          <Link
            to="/admin"
            className={`flex flex-col items-center py-1 px-3 rounded-full transition-all duration-200 ${
              isActive('/admin') ? 'text-[#ff9500] font-semibold scale-105' : 'text-white/50 hover:text-white'
            }`}
          >
            <Shield className="w-4 h-4 mb-0.5" />
            <span className="text-[9px]">Админка</span>
          </Link>
        )}

        <Link
          to="/profile"
          className={`flex flex-col items-center py-1 px-3 rounded-full transition-all duration-200 ${
            isActive('/profile') ? 'text-[#2997ff] font-semibold scale-105' : 'text-white/50 hover:text-white'
          }`}
        >
          <User className="w-4 h-4 mb-0.5" />
          <span className="text-[9px]">Профиль</span>
        </Link>
      </div>
    </>
  );
};
