import axiosClient from './axiosClient';

export const goalApi = {
  getGoals: status => axiosClient.get('/goals', { params: status ? { status } : {} }),
  getGoal: id => axiosClient.get(`/goals/${id}`),
  createGoal: data => axiosClient.post('/goals', data),
  updateGoal: (id, data) => axiosClient.put(`/goals/${id}`, data),
  setGoalStatus: (id, status) => axiosClient.patch(`/goals/${id}/status`, { status }),
  getContributions: (goalId, page = 0, size = 10) =>
    axiosClient.get(`/goals/${goalId}/contributions`, { params: { page, size } }),
  addContribution: (goalId, data) => axiosClient.post(`/goals/${goalId}/contributions`, data),
  deleteContribution: (goalId, contribId) => axiosClient.delete(`/goals/${goalId}/contributions/${contribId}`),
  getFreeBalance: () => axiosClient.get('/goals/free-balance')
};
