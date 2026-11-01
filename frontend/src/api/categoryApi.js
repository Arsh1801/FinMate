import axiosClient from './axiosClient';

export const categoryApi = {
  getCategories: type => axiosClient.get('/categories', { params: type ? { type } : {} }),
  createCategory: data => axiosClient.post('/categories', data),
  updateCategory: (id, data) => axiosClient.put(`/categories/${id}`, data),
  deleteCategory: id => axiosClient.delete(`/categories/${id}`)
};
