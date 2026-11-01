import axiosClient from './axiosClient';

export const transactionApi = {
  getTransactions: params => axiosClient.get('/transactions', { params }),
  getTransaction: id => axiosClient.get(`/transactions/${id}`),
  createTransaction: data => axiosClient.post('/transactions', data),
  updateTransaction: (id, data) => axiosClient.put(`/transactions/${id}`, data),
  deleteTransaction: id => axiosClient.delete(`/transactions/${id}`),
  exportCsv: params => axiosClient.get('/transactions/export', { params, responseType: 'blob' }),

  // Recurring Rules
  getRecurringRules: () => axiosClient.get('/recurring-rules'),
  createRecurringRule: data => axiosClient.post('/recurring-rules', data),
  updateRecurringRule: (id, data) => axiosClient.put(`/recurring-rules/${id}`, data),
  toggleRecurringRule: id => axiosClient.patch(`/recurring-rules/${id}/toggle`),
  runRecurringRuleNow: id => axiosClient.post(`/recurring-rules/${id}/run`),
  deleteRecurringRule: id => axiosClient.delete(`/recurring-rules/${id}`)
};
