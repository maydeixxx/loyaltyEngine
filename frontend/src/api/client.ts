import axios from 'axios';

export const api = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to attach JWT
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('loyalty_jwt');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Response interceptor to handle auth expiration
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // If 401 Unauthorized and not an auth attempt, clear token
    if (
      error.response?.status === 401 &&
      !error.config?.url?.includes('/users/auth') &&
      !error.config?.url?.includes('/users/register') &&
      !error.config?.url?.includes('/products/all')
    ) {
      localStorage.removeItem('loyalty_jwt');
      const publicPaths = ['/login', '/register', '/', '/catalog', '/products'];
      if (!publicPaths.includes(window.location.pathname)) {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);
