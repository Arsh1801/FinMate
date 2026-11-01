import axiosClient from './axiosClient';

export const dashboardApi = {
  getSummary: period => axiosClient.get('/dashboard/summary', { params: { period } }),
  getMonthlyBar: months => axiosClient.get('/dashboard/monthly-bar', { params: { months } }),
  getCategoryDonut: month => axiosClient.get('/dashboard/category-donut', { params: { month } }),
  getSpendingTrend: days => axiosClient.get('/dashboard/spending-trend', { params: { days } }),
  getBudgetBars: (year, month) => axiosClient.get('/dashboard/budget-bars', { params: { year, month } }),
  getInsights: () => axiosClient.get('/dashboard/insights'),
  getGroupSummary: () => axiosClient.get('/dashboard/group-summary'),
  getExchangeRate: (base = 'USD', target = 'INR') =>
    axiosClient.get('/external/exchange-rate', { params: { base, target } })
};
