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
      setProfileMsg({ type: 'success', text: 'Данные профиля успешно сохранены' });
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
      setPasswordMsg({ type: 'success', text: 'Пароль успешно обновлен' });
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
    <div className="min-h-screen bg-[#0d0d0f] py-8 sm:py-12 px-4 sm:px-6 pb-32 md:pb-16 max-w-4xl mx-auto space-y-8">
      {/* Header Profile Card (Apple Account Style) */}
      <div className="apple-card p-6 sm:p-8 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-5 relative overflow-hidden">
        <div className="flex items-center space-x-4">
          <div className="w-16 h-16 rounded-2xl bg-gradient-to-tr from-white/10 to-white/20 border border-white/20 flex items-center justify-center text-white font-bold text-2xl shadow-inner flex-shrink-0">
            {user?.firstName ? user.firstName[0].toUpperCase() : <User className="w-8 h-8" />}
          </div>
          <div className="min-w-0">
            <span className="text-[11px] uppercase tracking-widest text-[#2997ff] font-semibold">
              Профиль • Учетная запись
            </span>
            <h1 className="text-xl sm:text-2xl font-semibold text-white tracking-tight truncate mt-0.5">
              {user?.firstName ? `${user.firstName} ${user.lastName || ''}` : 'Личный профиль'}
            </h1>
            <p className="text-xs text-white/50 mt-0.5">{user?.email}</p>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-2 pt-2 sm:pt-0 border-t sm:border-t-0 border-white/[0.06]">
          <span className="font-mono bg-white/[0.04] text-white/60 px-3 py-1.5 rounded-full text-[11px] font-medium border border-white/10">
            ID: {userId ? `${userId.substring(0, 8)}...` : '—'}
          </span>
          <span className="bg-[#2997ff]/15 text-[#2997ff] border border-[#2997ff]/30 px-3 py-1.5 rounded-full text-[11px] font-semibold">
            {role || 'ROLE_USER'}
          </span>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Personal Info Form */}
        <div className="apple-card p-6 sm:p-7 space-y-5">
          <div className="flex items-center space-x-2.5 pb-3 border-b border-white/[0.06]">
            <div className="p-1.5 bg-white/[0.05] rounded-xl text-[#2997ff]">
              <User className="w-4 h-4" />
            </div>
            <h2 className="font-semibold text-white text-sm">Персональные данные</h2>
          </div>

          {profileMsg && (
            <div
              className={`p-3 rounded-2xl text-xs flex items-center space-x-2.5 ${
                profileMsg.type === 'success'
                  ? 'bg-[#30d158]/15 text-[#30d158] border border-[#30d158]/30'
                  : 'bg-[#ff453a]/15 text-[#ff453a] border border-[#ff453a]/30'
              }`}
            >
              {profileMsg.type === 'success' ? (
                <CheckCircle2 className="w-4 h-4 text-[#30d158] flex-shrink-0" />
              ) : (
                <AlertCircle className="w-4 h-4 text-[#ff453a] flex-shrink-0" />
              )}
              <span className="font-medium">{profileMsg.text}</span>
            </div>
          )}

          <form onSubmit={handleUpdateName} className="space-y-4 text-xs">
            <div>
              <label className="block text-white/50 text-[11px] uppercase tracking-wider font-medium mb-1.5">
                Имя
              </label>
              <input
                type="text"
                required
                value={firstName}
                onChange={(e) => setFirstName(e.target.value)}
                className="w-full px-3.5 py-2.5 bg-white/[0.04] border border-white/10 rounded-xl text-xs text-white focus:border-[#2997ff] focus:ring-1 focus:ring-[#2997ff]/40 focus:outline-none transition"
              />
            </div>

            <div>
              <label className="block text-white/50 text-[11px] uppercase tracking-wider font-medium mb-1.5">
                Фамилия
              </label>
              <input
                type="text"
                required
                value={lastName}
                onChange={(e) => setLastName(e.target.value)}
                className="w-full px-3.5 py-2.5 bg-white/[0.04] border border-white/10 rounded-xl text-xs text-white focus:border-[#2997ff] focus:ring-1 focus:ring-[#2997ff]/40 focus:outline-none transition"
              />
            </div>

            <div>
              <label className="block text-white/40 text-[11px] uppercase tracking-wider font-medium mb-1.5">
                Email (системный идентификатор)
              </label>
              <input
                type="email"
                disabled
                value={user?.email || ''}
                className="w-full px-3.5 py-2.5 bg-white/[0.02] border border-white/5 rounded-xl text-xs text-white/40 cursor-not-allowed font-mono"
              />
            </div>

            <div className="pt-2">
              <button
                type="submit"
                disabled={loadingProfile}
                className="px-6 py-2.5 bg-white hover:bg-white/90 text-black font-semibold rounded-full text-xs transition-all duration-200 active:scale-[0.98] shadow-sm disabled:opacity-50 flex items-center space-x-2"
              >
                {loadingProfile && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                <span>Сохранить изменения</span>
              </button>
            </div>
          </form>
        </div>

        {/* Change Password Form */}
        <div className="apple-card p-6 sm:p-7 space-y-5">
          <div className="flex items-center space-x-2.5 pb-3 border-b border-white/[0.06]">
            <div className="p-1.5 bg-white/[0.05] rounded-xl text-[#2997ff]">
              <KeyRound className="w-4 h-4" />
            </div>
            <h2 className="font-semibold text-white text-sm">Безопасность и пароль</h2>
          </div>

          {passwordMsg && (
            <div
              className={`p-3 rounded-2xl text-xs flex items-center space-x-2.5 ${
                passwordMsg.type === 'success'
                  ? 'bg-[#30d158]/15 text-[#30d158] border border-[#30d158]/30'
                  : 'bg-[#ff453a]/15 text-[#ff453a] border border-[#ff453a]/30'
              }`}
            >
              {passwordMsg.type === 'success' ? (
                <CheckCircle2 className="w-4 h-4 text-[#30d158] flex-shrink-0" />
              ) : (
                <AlertCircle className="w-4 h-4 text-[#ff453a] flex-shrink-0" />
              )}
              <span className="font-medium">{passwordMsg.text}</span>
            </div>
          )}

          <form onSubmit={handleUpdatePassword} className="space-y-4 text-xs">
            <div>
              <label className="block text-white/50 text-[11px] uppercase tracking-wider font-medium mb-1.5">
                Текущий пароль
              </label>
              <input
                type="password"
                required
                placeholder="••••••••"
                value={oldPassword}
                onChange={(e) => setOldPassword(e.target.value)}
                className="w-full px-3.5 py-2.5 bg-white/[0.04] border border-white/10 rounded-xl text-xs text-white focus:border-[#2997ff] focus:ring-1 focus:ring-[#2997ff]/40 focus:outline-none transition"
              />
            </div>

            <div>
              <label className="block text-white/50 text-[11px] uppercase tracking-wider font-medium mb-1.5">
                Новый пароль
              </label>
              <input
                type="password"
                required
                placeholder="Минимум 8 символов"
                minLength={8}
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                className="w-full px-3.5 py-2.5 bg-white/[0.04] border border-white/10 rounded-xl text-xs text-white focus:border-[#2997ff] focus:ring-1 focus:ring-[#2997ff]/40 focus:outline-none transition"
              />
            </div>

            <div className="p-3 bg-white/[0.03] rounded-2xl border border-white/[0.06] flex items-center space-x-2 text-[11px] text-white/50">
              <Shield className="w-4 h-4 text-[#2997ff] flex-shrink-0" />
              <span>Шифрование BCrypt на уровне UserService</span>
            </div>

            <div className="pt-2">
              <button
                type="submit"
                disabled={loadingPassword}
                className="px-6 py-2.5 bg-white/10 hover:bg-white/15 text-white font-medium rounded-full text-xs transition-all duration-200 active:scale-[0.98] border border-white/10 disabled:opacity-50 flex items-center space-x-2"
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
