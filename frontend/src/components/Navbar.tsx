import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Coins, User, Shield, LogOut, LayoutDashboard } from 'lucide-react';

export const Navbar: React.FC = () => {
  const { user, isAdmin, logout, isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) return null;

  const isActive = (path: string) => location.pathname === path;

  return (
    <>
      {/* Top Header (Mobile & Desktop) */}
      <nav className="bg-white border-b border-slate-200/80 sticky top-0 z-40 backdrop-blur-md bg-white/95">
        <div className="max-w-7xl mx-auto px-3 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center h-14 sm:h-16">
            {/* Logo */}
            <Link to="/" className="flex items-center space-x-2.5 group">
              <div className="p-1.5 sm:p-2 bg-indigo-600 rounded-xl text-white group-hover:bg-indigo-700 transition shadow-sm">
                <Coins className="w-5 h-5 sm:w-6 sm:h-6" />
              </div>
              <div className="flex items-baseline space-x-1.5">
                <span className="font-bold text-base sm:text-lg text-slate-900 tracking-tight">LoyaltyEngine</span>
                <span className="text-[10px] bg-indigo-50 text-indigo-700 font-semibold px-1.5 py-0.5 rounded-full">
                  App
                </span>
              </div>
            </Link>

            {/* Desktop Navigation Links */}
            <div className="hidden md:flex items-center space-x-2">
              <Link
                to="/"
                className={`flex items-center space-x-2 px-3 py-2 rounded-xl text-sm font-medium transition ${
                  isActive('/')
                    ? 'bg-indigo-50 text-indigo-700 font-semibold'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                }`}
              >
                <LayoutDashboard className="w-4 h-4" />
                <span>Дашборд</span>
              </Link>

              {isAdmin && (
                <Link
                  to="/admin"
                  className={`flex items-center space-x-2 px-3 py-2 rounded-xl text-sm font-medium transition ${
                    isActive('/admin')
                      ? 'bg-amber-50 text-amber-700 font-semibold'
                      : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                  }`}
                >
                  <Shield className="w-4 h-4 text-amber-600" />
                  <span>Админ-панель</span>
                </Link>
              )}
            </div>

            {/* User Profile & Logout */}
            <div className="flex items-center space-x-2 sm:space-x-3">
              <Link
                to="/profile"
                className={`flex items-center space-x-2 p-1 sm:px-3 sm:py-1.5 rounded-xl text-sm transition ${
                  isActive('/profile')
                    ? 'bg-slate-100 text-slate-900'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-50'
                }`}
              >
                <div className="w-7 h-7 sm:w-8 sm:h-8 rounded-full bg-indigo-100 text-indigo-700 flex items-center justify-center font-bold text-xs">
                  {user?.firstName ? user.firstName[0].toUpperCase() : <User className="w-3.5 h-3.5" />}
                </div>
                <div className="hidden sm:block text-left">
                  <div className="text-xs font-semibold text-slate-900 leading-tight">
                    {user?.firstName ? `${user.firstName} ${user.lastName || ''}` : user?.email}
                  </div>
                  <div className="text-[10px] text-slate-500">
                    {isAdmin ? (
                      <span className="text-amber-600 font-medium">Администратор</span>
                    ) : (
                      'Клиент'
                    )}
                  </div>
                </div>
              </Link>

              <button
                onClick={logout}
                title="Выйти"
                className="p-1.5 sm:p-2 text-slate-400 hover:text-rose-600 hover:bg-rose-50 rounded-xl transition"
              >
                <LogOut className="w-4 h-4 sm:w-5 sm:h-5" />
              </button>
            </div>
          </div>
        </div>
      </nav>

      {/* Mobile Bottom Navigation Bar (Fixed for native app feel) */}
      <div className="md:hidden fixed bottom-0 left-0 right-0 z-50 bg-white/95 backdrop-blur-lg border-t border-slate-200/80 px-4 py-2 flex justify-around items-center shadow-lg safe-area-pb">
        <Link
          to="/"
          className={`flex flex-col items-center py-1 px-3 rounded-xl transition ${
            isActive('/') ? 'text-indigo-600 font-semibold' : 'text-slate-500 hover:text-slate-900'
          }`}
        >
          <LayoutDashboard className="w-5 h-5 mb-0.5" />
          <span className="text-[10px]">Дашборд</span>
        </Link>

        {isAdmin && (
          <Link
            to="/admin"
            className={`flex flex-col items-center py-1 px-3 rounded-xl transition ${
              isActive('/admin') ? 'text-amber-600 font-semibold' : 'text-slate-500 hover:text-slate-900'
            }`}
          >
            <Shield className="w-5 h-5 mb-0.5" />
            <span className="text-[10px]">Админка</span>
          </Link>
        )}

        <Link
          to="/profile"
          className={`flex flex-col items-center py-1 px-3 rounded-xl transition ${
            isActive('/profile') ? 'text-indigo-600 font-semibold' : 'text-slate-500 hover:text-slate-900'
          }`}
        >
          <User className="w-5 h-5 mb-0.5" />
          <span className="text-[10px]">Профиль</span>
        </Link>
      </div>
    </>
  );
};
