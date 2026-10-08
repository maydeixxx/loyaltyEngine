import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Navbar } from './components/Navbar';
import { ProtectedRoute } from './components/ProtectedRoute';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { WalletPage } from './pages/WalletPage';
import { RulesPage } from './pages/RulesPage';
import { CheckoutPage } from './pages/CheckoutPage';
import { AdminPage } from './pages/AdminPage';
import { ProfilePage } from './pages/ProfilePage';
import { CatalogPage } from './pages/CatalogPage';

const RootRoute: React.FC = () => {
  const { isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-[#0d0d0f]">
        <div className="flex flex-col items-center space-y-3">
          <div className="w-8 h-8 rounded-full border-2 border-[#2997ff] border-t-transparent animate-spin" />
          <span className="text-xs text-white/40 font-medium">Загрузка каталога...</span>
        </div>
      </div>
    );
  }

  // Default page for unauthorized users is all products
  if (!isAuthenticated) {
    return <CatalogPage />;
  }

  // Authorized user landing
  return <WalletPage />;
};

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <Router>
        <div className="min-h-screen flex flex-col bg-[#0d0d0f] text-[#f5f5f7] selection:bg-[#0071e3]/30 selection:text-white">
          <Navbar />
          <main className="flex-1">
            <Routes>
              {/* Public Storefront Routes */}
              <Route path="/" element={<RootRoute />} />
              <Route path="/catalog" element={<CatalogPage />} />
              <Route path="/products" element={<CatalogPage />} />

              {/* Public Auth Routes */}
              <Route path="/login" element={<LoginPage />} />
              <Route path="/register" element={<RegisterPage />} />

              {/* Dedicated Core User Tabs (Protected) */}
              <Route
                path="/wallet"
                element={
                  <ProtectedRoute>
                    <WalletPage />
                  </ProtectedRoute>
                }
              />

              <Route
                path="/rules"
                element={
                  <ProtectedRoute>
                    <RulesPage />
                  </ProtectedRoute>
                }
              />

              <Route
                path="/checkout"
                element={
                  <ProtectedRoute>
                    <CheckoutPage />
                  </ProtectedRoute>
                }
              />

              {/* User Profile */}
              <Route
                path="/profile"
                element={
                  <ProtectedRoute>
                    <ProfilePage />
                  </ProtectedRoute>
                }
              />

              {/* Protected Admin Routes */}
              <Route
                path="/admin"
                element={
                  <ProtectedRoute requireAdmin>
                    <AdminPage />
                  </ProtectedRoute>
                }
              />

              {/* Catch-all redirect */}
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </main>
        </div>
      </Router>
    </AuthProvider>
  );
};

export default App;
