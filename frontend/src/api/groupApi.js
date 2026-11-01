import axiosClient from './axiosClient';

export const groupApi = {
  getGroups: status => axiosClient.get('/groups', { params: status ? { status } : {} }),
  getGroup: id => axiosClient.get(`/groups/${id}`),
  createGroup: data => axiosClient.post('/groups', data),
  updateGroup: (id, data) => axiosClient.put(`/groups/${id}`, data),
  getGroupMembers: id => axiosClient.get(`/groups/${id}/members`),
  inviteMember: (id, email) => axiosClient.post(`/groups/${id}/invitations`, { email }),
  getPendingInvitations: () => axiosClient.get('/groups/invitations/pending'),
  respondInvitation: (invitationId, accept) =>
  axiosClient.patch(`/invitations/${invitationId}`, {
    action: accept ? 'ACCEPT' : 'REJECT'
  }),
  leaveGroup: id => axiosClient.post(`/groups/${id}/leave`),

  // Shared Expenses
  getSharedExpenses: (groupId, page = 0, size = 20) =>
    axiosClient.get(`/groups/${groupId}/expenses`, { params: { page, size } }),
  getSharedExpense: (groupId, expenseId) =>
    axiosClient.get(`/groups/${groupId}/expenses/${expenseId}`),
  createSharedExpense: (groupId, data) =>
    axiosClient.post(`/groups/${groupId}/expenses`, data),
  updateSharedExpense: (groupId, expenseId, data) =>
    axiosClient.put(`/groups/${groupId}/expenses/${expenseId}`, data),
  deleteSharedExpense: (groupId, expenseId) =>
    axiosClient.delete(`/groups/${groupId}/expenses/${expenseId}`)
};
