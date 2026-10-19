import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { Lock, Mail, User, AlertCircle, ArrowRight, Loader2, Sparkles } from 'lucide-react';

export const RegisterPage: React.FC = () => {
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const { register } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const redirectMessage = location.state?.message;
  const selectedProduct = location.state?.selectedProduct;
  const from = location.state?.from || '/';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (password.length < 8) {
      setError('Пароль должен содержать минимум 8 символов');
      return;
    }

    setLoading(true);

    try {
      await register({
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        email: email.trim(),
        password,
      });
      if (selectedProduct) {
        navigate('/checkout', { state: { selectedProduct } });
      } else {
        navigate(from);
      }
    } catch (err: any) {
      console.error('Registration error:', err);
      const serverMsg =
        err.response?.data?.message || err.response?.data || 'Ошибка при регистрации аккаунта';
      setError(typeof serverMsg === 'string' ? serverMsg : 'Ошибка при регистрации. Проверьте данные.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#0d0d0f] flex flex-col justify-center py-12 px-4 sm:px-6 lg:px-8 pb-32">
      <div className="sm:mx-auto sm:w-full sm:max-w-md text-center">
        <div className="inline-flex p-3.5 bg-white/[0.06] backdrop-blur-xl border border-white/15 text-white rounded-2xl shadow-inner mb-4">
          <Sparkles className="w-8 h-8 text-[#ffd60a]" />
        </div>
        <span className="block text-[11px] uppercase tracking-widest text-[#2997ff] font-semibold">
          Программа привилегий • LoyaltyEngine
        </span>
        <h2 className="text-2xl sm:text-3xl font-semibold text-white tracking-tight mt-1">
          Создание аккаунта
        </h2>
        <p className="mt-1 text-xs text-white/50">
          Присоединяйтесь к программе привилегий LoyaltyEngine
        </p>
      </div>

      <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
        <div className="apple-card py-8 px-6 sm:px-10 border border-white/15 relative overflow-hidden">
          {redirectMessage && !error && (
            <div className="mb-5 p-3.5 rounded-2xl bg-[#0071e3]/15 border border-[#0071e3]/30 flex items-start space-x-2.5 text-[#2997ff]">
              <Sparkles className="w-4 h-4 flex-shrink-0 mt-0.5 text-[#2997ff]" />
              <div className="text-xs font-medium leading-relaxed">{redirectMessage}</div>
            </div>
          )}

          {error && (
            <div className="mb-5 p-3.5 rounded-2xl bg-[#ff453a]/15 border border-[#ff453a]/30 flex items-start space-x-2.5 text-[#ff453a]">
              <AlertCircle className="w-4 h-4 flex-shrink-0 mt-0.5" />
              <div className="text-xs font-medium">{error}</div>
            </div>
          )}

          <form className="space-y-4" onSubmit={handleSubmit}>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-[11px] font-medium text-white/50 uppercase tracking-wider mb-1.5">
                  Имя
                </label>
                <div className="relative rounded-xl">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-white/40">
                    <User className="w-3.5 h-3.5" />
                  </div>
                  <input
                    type="text"
                    required
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    placeholder="Иван"
                    className="block w-full pl-8 pr-3 py-2 bg-white/[0.04] border border-white/10 rounded-xl text-xs text-white placeholder-white/30 focus:border-[#2997ff] focus:ring-1 focus:ring-[#2997ff]/40 focus:outline-none transition-all duration-200"
                  />
                </div>
              </div>

              <div>
                <label className="block text-[11px] font-medium text-white/50 uppercase tracking-wider mb-1.5">
                  Фамилия
                </label>
                <div className="relative rounded-xl">
                  <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-white/40">
                    <User className="w-3.5 h-3.5" />
                  </div>
                  <input
                    type="text"
                    required
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                    placeholder="Иванов"
                    className="block w-full pl-8 pr-3 py-2 bg-white/[0.04] border border-white/10 rounded-xl text-xs text-white placeholder-white/30 focus:border-[#2997ff] focus:ring-1 focus:ring-[#2997ff]/40 focus:outline-none transition-all duration-200"
                  />
                </div>
              </div>
            </div>

            <div>
              <label className="block text-[11px] font-medium text-white/50 uppercase tracking-wider mb-1.5">
                Email
              </label>
              <div className="relative rounded-xl">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-white/40">
                  <Mail className="w-4 h-4" />
                </div>
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="name@example.com"
                  className="block w-full pl-10 pr-4 py-2.5 bg-white/[0.04] border border-white/10 rounded-xl text-xs text-white placeholder-white/30 focus:border-[#2997ff] focus:ring-1 focus:ring-[#2997ff]/40 focus:outline-none transition-all duration-200"
                />
              </div>
            </div>

            <div>
              <label className="block text-[11px] font-medium text-white/50 uppercase tracking-wider mb-1.5">
                Пароль
              </label>
              <div className="relative rounded-xl">
                <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-white/40">
                  <Lock className="w-4 h-4" />
                </div>
                <input
                  type="password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Минимум 8 символов"
                  minLength={8}
                  className="block w-full pl-10 pr-4 py-2.5 bg-white/[0.04] border border-white/10 rounded-xl text-xs text-white placeholder-white/30 focus:border-[#2997ff] focus:ring-1 focus:ring-[#2997ff]/40 focus:outline-none transition-all duration-200"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full flex justify-center items-center py-3 px-4 mt-2 bg-white hover:bg-white/90 text-black rounded-full text-xs font-semibold transition-all duration-200 active:scale-[0.98] shadow-md disabled:opacity-50"
            >
              {loading ? (
                <Loader2 className="w-4 h-4 animate-spin text-black" />
              ) : (
                <span className="flex items-center gap-1.5">
                  Зарегистрироваться <ArrowRight className="w-3.5 h-3.5" />
                </span>
              )}
            </button>
          </form>

          <div className="mt-6 pt-6 border-t border-white/[0.06] text-center">
            <p className="text-xs text-white/40">
              Уже есть аккаунт?{' '}
              <Link
                to="/login"
                state={location.state}
                className="font-medium text-[#2997ff] hover:text-[#64d2ff] transition"
              >
                Войти в систему
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
