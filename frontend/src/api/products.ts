import { api } from './client';
import type {
  ProductDTO,
  CreateProductPayload,
  UpdateProductPayload,
  ChangeProductStatusPayload,
} from '../types';

export const productApi = {
  // Получить все товары
  getAllProducts: async (): Promise<ProductDTO[]> => {
    const response = await api.get<ProductDTO[]>('/products/all');
    return response.data;
  },

  // Получить товары по категории
  getProductsByCategory: async (category: string): Promise<ProductDTO[]> => {
    const response = await api.get<ProductDTO[]>(`/products/category/${category}`);
    return response.data;
  },

  // Получить товар по id
  getProductById: async (productId: string): Promise<ProductDTO> => {
    const response = await api.get<ProductDTO>(`/products/${productId}`);
    return response.data;
  },

  // Создать новый товар
  createProduct: async (payload: CreateProductPayload): Promise<void> => {
    await api.post('/products', payload);
  },

  // Обновить товар
  updateProduct: async (productId: string, payload: UpdateProductPayload): Promise<void> => {
    await api.put(`/products/${productId}`, payload);
  },

  // Активировать товар
  activateProduct: async (payload: ChangeProductStatusPayload): Promise<void> => {
    await api.put('/products/activate', payload);
  },

  // Остановить товар
  stopProduct: async (payload: ChangeProductStatusPayload): Promise<void> => {
    await api.put('/products/stop', payload);
  },

  // Удалить товар
  deleteProduct: async (productId: string): Promise<void> => {
    await api.delete(`/products/${productId}`);
  },
};
