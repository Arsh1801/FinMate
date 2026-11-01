import axiosClient from './axiosClient';

export const budgetApi = {
  getBudgetsForMonth: (year, month) => axiosClient.get('/budgets', { params: { year, month } }),
  getBudgetHistory: categoryId => axiosClient.get('/budgets/history', { params: { categoryId } }),
  createBudget: data => axiosClient.post('/budgets', data),
  updateBudget: (id, data) => axiosClient.put(`/budgets/${id}`, data),
  copyPriorMonthBudgets: (sourceYear, sourceMonth, targetYear, targetMonth) =>
    axiosClient.post('/budgets/copy', { sourceYear, sourceMonth, targetYear, targetMonth }),
  deleteBudget: id => axiosClient.delete(`/budgets/${id}`)
};
