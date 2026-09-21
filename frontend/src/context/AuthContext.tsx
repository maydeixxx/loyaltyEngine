import React, { createContext, useContext, useState, useEffect } from 'react';
import type { ReactNode } from 'react';
import type { JWTPayload, UserDTO } from '../types';
import { authApi } from '../api/auth';
import type { AuthRequest, RegisterRequest } from '../api/auth';

export interface AuthContextType {
  token: string | null;
  user: UserDTO | null;
  userId: string | null;
  role: string | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  isLoading: boolean;
  login: (credentials: AuthRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => void;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

function parseJwt(token: string): JWTPayload | null {
  try {
    const base64Url = token.split('.')[1];
    if (!base64Url) return null;
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      window
        .atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch (e) {
    console.error('Failed to parse JWT', e);
    return null;
  }
}

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('loyalty_jwt'));
  const [user, setUser] = useState<UserDTO | null>(null);
  const [userId, setUserId] = useState<string | null>(null);
  const [role, setRole] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  // Initialize auth state from stored token
  useEffect(() => {
    const initAuth = async () => {
      if (token) {
        const payload = parseJwt(token);
        if (payload && payload.exp * 1000 > Date.now()) {
          setUserId(payload.User_id);
          setRole(payload.Role);

          try {
            const selfUser = await authApi.getSelf();
            setUser(selfUser);
          } catch (err) {
            console.warn('Could not fetch user profile with current token', err);
            // Fallback user from JWT
            setUser({
              id: payload.User_id,
              email: payload.sub,
              firstName: 'Пользователь',
              lastName: '',
            });
          }
        } else {
          // Token expired
          localStorage.removeItem('loyalty_jwt');
          setToken(null);
          setUser(null);
          setUserId(null);
          setRole(null);
        }
      }
      setIsLoading(false);
    };

    initAuth();
  }, [token]);

  const login = async (credentials: AuthRequest) => {
    const jwtToken = await authApi.login(credentials);
    const cleanToken = jwtToken.trim().replace(/^"|"$/g, ''); // Handle possible quoted string
    localStorage.setItem('loyalty_jwt', cleanToken);
    setToken(cleanToken);

    const payload = parseJwt(cleanToken);
    if (payload) {
      setUserId(payload.User_id);
      setRole(payload.Role);
      try {
        const selfUser = await authApi.getSelf();
        setUser(selfUser);
      } catch {
        setUser({
          id: payload.User_id,
          email: payload.sub,
          firstName: '',
          lastName: '',
        });
      }
    }
  };

  const register = async (data: RegisterRequest) => {
    await authApi.register(data);
    // After registration, auto login
    await login({ email: data.email, password: data.password });
  };

  const logout = () => {
    localStorage.removeItem('loyalty_jwt');
    setToken(null);
    setUser(null);
    setUserId(null);
    setRole(null);
  };

  const refreshUser = async () => {
    if (!token) return;
    try {
      const selfUser = await authApi.getSelf();
      setUser(selfUser);
    } catch (e) {
      console.error('Error refreshing user', e);
    }
  };

  const isAuthenticated = !!token && !!userId;
  const isAdmin = role === 'ROLE_ADMIN' || role === 'ADMIN';

  return (
    <AuthContext.Provider
      value={{
        token,
        user,
        userId,
        role,
        isAuthenticated,
        isAdmin,
        isLoading,
        login,
        register,
        logout,
        refreshUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
