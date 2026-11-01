import axiosClient from './axiosClient';

export const notificationApi = {
  getNotifications: (unreadOnly = false, page = 0, size = 20) =>
    axiosClient.get('/notifications', { params: { unreadOnly, page, size } }),
  getUnreadCount: () => axiosClient.get('/notifications/unread-count'),
  markAsRead: id => axiosClient.patch(`/notifications/${id}/read`),
  markAllAsRead: () => axiosClient.patch('/notifications/read-all'),
  deleteNotification: id => axiosClient.delete(`/notifications/${id}`)
};
