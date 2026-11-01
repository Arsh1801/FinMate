import axiosClient from './axiosClient';

export const settlementApi = {
  getGroupBalances: groupId => axiosClient.get(`/groups/${groupId}/balances`),
  getSettlementSuggestions: groupId => axiosClient.get(`/groups/${groupId}/settlements/suggestions`),
  getSettlementsHistory: (groupId, status, page = 0, size = 10) =>
    axiosClient.get(`/groups/${groupId}/settlements`, { params: { status, page, size } }),
  recordSettlement: (groupId, data) => axiosClient.post(`/groups/${groupId}/settlements`, data),
  confirmSettlement: (groupId, settlementId) =>
    axiosClient.post(`/groups/${groupId}/settlements/${settlementId}/confirm`),
  rejectSettlement: (groupId, settlementId) =>
    axiosClient.post(`/groups/${groupId}/settlements/${settlementId}/reject`),
  getPendingForUser: () => axiosClient.get('/groups/settlements/pending-for-me')
};
