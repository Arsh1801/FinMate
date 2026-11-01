import axiosClient from './axiosClient';

export const reportApi = {
  getMonthlyReport: month => axiosClient.get('/reports/monthly', { params: { month } }),
  downloadMonthlyPdf: month =>
    axiosClient.get('/reports/monthly/pdf', { params: { month }, responseType: 'blob' })
};
