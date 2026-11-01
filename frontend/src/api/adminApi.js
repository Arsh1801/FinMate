import axiosClient from './axiosClient';

export const adminApi = {
  getUsers: (page = 0, size = 15, search = '') =>
    axiosClient.get('/admin/users', { params: { page, size, search } }),
  updateUserStatus: (userId, status) =>
    axiosClient.patch(`/admin/users/${userId}/status`, { status }),
  getPlatformStats: () => axiosClient.get('/admin/stats'),

  // Category Management
  getAdminCategories: () => axiosClient.get('/admin/categories'),
  createSystemCategory: data => axiosClient.post('/admin/categories', data),
  updateSystemCategory: (id, data) => axiosClient.put(`/admin/categories/${id}`, data),
  toggleCategoryStatus: (id, isActive) =>
    axiosClient.patch(`/admin/categories/${id}/status`, { isActive })
};
