import React, { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import { usersApi } from '../api/users';
import { User, KeyRound, Shield, CheckCircle2, AlertCircle, Loader2 } from 'lucide-react';

export const ProfilePage: React.FC = () => {
  const { user, userId, role, refreshUser } = useAuth();

  const [firstName, setFirstName] = useState(user?.firstName || '');
  const [lastName, setLastName] = useState(user?.lastName || '');
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');

  const [loadingProfile, setLoadingProfile] = useState(false);
  const [loadingPassword, setLoadingPassword] = useState(false);
  const [profileMsg, setProfileMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null);
  const [passwordMsg, setPasswordMsg] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const handleUpdateName = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user?.email) return;
    setProfileMsg(null);
    setLoadingProfile(true);

    try {
      if (firstName.trim() !== user.firstName) {
        await usersApi.updateUser(user.email, {
          fieldToUpdate: 'firstname',
          firstName: firstName.trim(),
        });
      }
      if (lastName.trim() !== user.lastName) {
        await usersApi.updateUser(user.email, {
          fieldToUpdate: 'lastname',
          lastName: lastName.trim(),
        });
      }
      await refreshUser();
      setProfileMsg({ type: 'success', text: 'Данные профиля успешно обновлены' });
    } catch (err: any) {
      console.error(err);
      setProfileMsg({ type: 'error', text: 'Не удалось обновить данные профиля' });
    } finally {
      setLoadingProfile(false);
    }
  };

  const handleUpdatePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user?.email) return;
    setPasswordMsg(null);

    if (newPassword.length < 8) {
      setPasswordMsg({ type: 'error', text: 'Новый пароль должен содержать минимум 8 символов' });
      return;
    }

    setLoadingPassword(true);
    try {
      await usersApi.updateUser(user.email, {
        fieldToUpdate: 'password',
        oldPassword,
        newPassword,
      });
      setPasswordMsg({ type: 'success', text: 'Пароль успешно изменен' });
      setOldPassword('');
      setNewPassword('');
    } catch (err: any) {
      console.error(err);
      const msg = err.response?.data?.message || err.response?.data || 'Неверный старый пароль';
      setPasswordMsg({ type: 'error', text: typeof msg === 'string' ? msg : 'Ошибка смены пароля' });
    } finally {
      setLoadingPassword(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 py-4 sm:py-8 px-3 sm:px-6 pb-24 max-w-4xl mx-auto space-y-4 sm:space-y-6">
      {/* Header */}
      <div className="bg-white p-4 sm:p-6 rounded-2xl sm:rounded-3xl border border-slate-200/80 shadow-xs flex items-center space-x-3 sm:space-x-4">
        <div className="w-12 h-12 sm:w-14 sm:h-14 bg-indigo-600 rounded-xl sm:rounded-2xl flex items-center justify-center text-white font-bold text-lg sm:text-xl shadow-md shadow-indigo-200 flex-shrink-0">
          {user?.firstName ? user.firstName[0].toUpperCase() : <User className="w-6 h-6" />}
        </div>
        <div className="min-w-0 flex-1">
            <h1 className="text-xl font-bold text-slate-900">
              {user?.firstName ? `${user.firstName} ${user.lastName || ''}` : 'Личный профиль'}
            </h1>
            <div className="flex items-center space-x-2 text-xs text-slate-500 mt-0.5">
              <span>{user?.email}</span>
              <span>•</span>
              <span className="font-mono bg-slate-100 px-2 py-0.5 rounded text-[10px]">
                ID: {userId}
              </span>
              <span>•</span>
              <span className="bg-indigo-50 text-indigo-700 px-2 py-0.5 rounded text-[10px] font-semibold">
                {role || 'ROLE_USER'}
              </span>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Personal Info Form */}
          <div className="bg-white p-6 rounded-3xl border border-slate-200/80 shadow-sm space-y-4">
            <div className="flex items-center space-x-2">
              <User className="w-5 h-5 text-indigo-600" />
              <h2 className="font-bold text-slate-900 text-sm">Персональные данные</h2>
            </div>

            {profileMsg && (
              <div
                className={`p-3 rounded-xl text-xs flex items-center space-x-2 ${
                  profileMsg.type === 'success'
                    ? 'bg-emerald-50 text-emerald-800 border border-emerald-100'
                    : 'bg-rose-50 text-rose-800 border border-rose-100'
                }`}
              >
                {profileMsg.type === 'success' ? (
                  <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                ) : (
                  <AlertCircle className="w-4 h-4 text-rose-600" />
                )}
                <span>{profileMsg.text}</span>
              </div>
            )}

            <form onSubmit={handleUpdateName} className="space-y-4 text-xs">
              <div>
                <label className="block text-slate-600 font-semibold mb-1">Имя</label>
                <input
                  type="text"
                  required
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-slate-600 font-semibold mb-1">Фамилия</label>
                <input
                  type="text"
                  required
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-slate-600 font-semibold mb-1">Email</label>
                <input
                  type="email"
                  disabled
                  value={user?.email || ''}
                  className="w-full px-3 py-2 bg-slate-100 border border-slate-200 rounded-xl text-xs text-slate-500 cursor-not-allowed"
                />
              </div>

              <div className="pt-2">
                <button
                  type="submit"
                  disabled={loadingProfile}
                  className="px-5 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white font-semibold rounded-xl text-xs transition shadow-sm shadow-indigo-200 disabled:opacity-50 flex items-center space-x-2"
                >
                  {loadingProfile && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                  <span>Сохранить изменения</span>
                </button>
              </div>
            </form>
          </div>

          {/* Change Password Form */}
          <div className="bg-white p-6 rounded-3xl border border-slate-200/80 shadow-sm space-y-4">
            <div className="flex items-center space-x-2">
              <KeyRound className="w-5 h-5 text-indigo-600" />
              <h2 className="font-bold text-slate-900 text-sm">Безопасность и пароль</h2>
            </div>

            {passwordMsg && (
              <div
                className={`p-3 rounded-xl text-xs flex items-center space-x-2 ${
                  passwordMsg.type === 'success'
                    ? 'bg-emerald-50 text-emerald-800 border border-emerald-100'
                    : 'bg-rose-50 text-rose-800 border border-rose-100'
                }`}
              >
                {passwordMsg.type === 'success' ? (
                  <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                ) : (
                  <AlertCircle className="w-4 h-4 text-rose-600" />
                )}
                <span>{passwordMsg.text}</span>
              </div>
            )}

            <form onSubmit={handleUpdatePassword} className="space-y-4 text-xs">
              <div>
                <label className="block text-slate-600 font-semibold mb-1">Текущий пароль</label>
                <input
                  type="password"
                  required
                  placeholder="••••••••"
                  value={oldPassword}
                  onChange={(e) => setOldPassword(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-slate-600 font-semibold mb-1">Новый пароль</label>
                <input
                  type="password"
                  required
                  placeholder="Минимум 8 символов"
                  minLength={8}
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-50 border border-slate-200 rounded-xl text-xs focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                />
              </div>

              <div className="p-3 bg-slate-50 rounded-xl border border-slate-100 flex items-center space-x-2 text-[11px] text-slate-500">
                <Shield className="w-4 h-4 text-indigo-600 flex-shrink-0" />
                <span>Хэширование BCrypt с солью на бэкенде UserService</span>
              </div>

              <div className="pt-2">
                <button
                  type="submit"
                  disabled={loadingPassword}
                  className="px-5 py-2.5 bg-slate-900 hover:bg-black text-white font-semibold rounded-xl text-xs transition shadow-sm disabled:opacity-50 flex items-center space-x-2"
                >
                  {loadingPassword && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                  <span>Обновить пароль</span>
                </button>
              </div>
            </form>
          </div>
        </div>
    </div>
  );
};
