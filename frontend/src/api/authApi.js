import axiosClient from './axiosClient';

export const authApi = {
  login: credentials => axiosClient.post('/auth/login', credentials),
  register: userData => axiosClient.post('/auth/register', userData),
  refreshToken: token => axiosClient.post('/auth/refresh', { refreshToken: token }),
  forgotPassword: email => axiosClient.post('/auth/forgot-password', { email }),
  resetPassword: data => axiosClient.post('/auth/reset-password', data),
  changePassword: data => axiosClient.post('/auth/change-password', data),
  logout: () => axiosClient.post('/auth/logout')
};
