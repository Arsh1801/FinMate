import React, { useState, useEffect } from 'react';
import { notificationApi } from '../../api/notificationApi';
import { useNotification } from '../../context/NotificationContext';
import { formatDateTime } from '../../utils/formatters';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import EmptyState from '../../components/common/EmptyState';
import {
  Bell,
  CheckCheck,
  Trash2,
  AlertTriangle,
  ArrowRightLeft,
  Users2,
  Calendar,
  Check
} from 'lucide-react';

export default function NotificationsPage() {
  const [notifications, setNotifications] = useState([]);
  const [unreadOnly, setUnreadOnly] = useState(false);
  const [loading, setLoading] = useState(true);
  const { setUnreadCount, refreshUnreadCount } = useNotification();

  const fetchNotifications = async () => {
    setLoading(true);
    try {
      const resp = await notificationApi.getNotifications(unreadOnly, 0, 50);
      setNotifications(resp.data.content || []);
      refreshUnreadCount();
    } catch (err) {
      console.error('Failed to load notifications', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchNotifications();
  }, [unreadOnly]);

  const handleMarkAsRead = async id => {
    try {
      await notificationApi.markAsRead(id);
      setNotifications(prev =>
        prev.map(n => (n.id === id ? { ...n, isRead: true } : n))
      );
      setUnreadCount(c => Math.max(0, c - 1));
    } catch (err) {
      console.error('Failed to mark notification read', err);
    }
  };

  const handleMarkAllRead = async () => {
    try {
      await notificationApi.markAllAsRead();
      setNotifications(prev => prev.map(n => ({ ...n, isRead: true })));
      setUnreadCount(0);
    } catch (err) {
      alert('Failed to mark all notifications as read');
    }
  };

  const handleDelete = async id => {
    try {
      await notificationApi.deleteNotification(id);
      setNotifications(prev => prev.filter(n => n.id !== id));
      refreshUnreadCount();
    } catch (err) {
      alert('Failed to delete notification');
    }
  };

  const getNotificationIcon = type => {
    switch (type) {
      case 'BUDGET_EXCEEDED':
      case 'BUDGET_WARNING':
        return <AlertTriangle size={18} style={{ color: 'var(--danger-main)' }} />;
      case 'SETTLEMENT_RECORDED':
      case 'SETTLEMENT_CONFIRMED':
        return <ArrowRightLeft size={18} style={{ color: 'var(--primary-600)' }} />;
      case 'GROUP_INVITATION':
      case 'GROUP_EXPENSE_ADDED':
        return <Users2 size={18} style={{ color: 'var(--info-main)' }} />;
      default:
        return <Bell size={18} style={{ color: 'var(--text-secondary)' }} />;
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem', maxWidth: 800, margin: '0 auto' }}>
      {/* Header */}
      <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '1.625rem', fontWeight: 700, color: 'var(--text-primary)', letterSpacing: '-0.02em' }}>
            Notifications & System Alerts
          </h1>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
            Real-time triggers for budget pacing warnings, group debts, settlements, and recurring payments
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <select
            className="finmate-select"
            value={unreadOnly ? 'unread' : 'all'}
            onChange={e => setUnreadOnly(e.target.value === 'unread')}
            style={{ width: 'auto' }}
          >
            <option value="all">All Notifications</option>
            <option value="unread">Unread Only</option>
          </select>

          <button onClick={handleMarkAllRead} className="btn-finmate-secondary">
            <CheckCheck size={16} />
            <span>Mark All as Read</span>
          </button>
        </div>
      </div>

      {/* Notifications List */}
      <div className="finmate-card" style={{ overflow: 'hidden' }}>
        {loading ? (
          <div style={{ padding: '3rem 0' }}>
            <LoadingSpinner message="Fetching notifications..." />
          </div>
        ) : notifications.length > 0 ? (
          <div style={{ display: 'flex', flexDirection: 'column' }}>
            {notifications.map(n => (
              <div
                key={n.id}
                style={{
                  display: 'flex',
                  alignItems: 'flex-start',
                  justifyContent: 'space-between',
                  padding: '1.25rem',
                  borderBottom: '1px solid var(--border-color)',
                  backgroundColor: n.isRead ? 'transparent' : 'var(--bg-tertiary)',
                  transition: 'background-color 0.15s'
                }}
              >
                <div style={{ display: 'flex', alignItems: 'flex-start', gap: '1rem' }}>
                  <div
                    style={{
                      width: 38,
                      height: 38,
                      borderRadius: 'var(--radius-md)',
                      backgroundColor: 'var(--bg-card)',
                      border: '1px solid var(--border-color)',
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      flexShrink: 0,
                      marginTop: '0.1rem'
                    }}
                  >
                    {getNotificationIcon(n.type)}
                  </div>

                  <div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
                      <h4 style={{ fontSize: '0.9375rem', fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>
                        {n.title}
                      </h4>
                      {!n.isRead && (
                        <span
                          style={{
                            width: 7,
                            height: 7,
                            borderRadius: '50%',
                            backgroundColor: 'var(--primary-600)'
                          }}
                        />
                      )}
                    </div>

                    <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', margin: '0 0 0.5rem 0', lineHeight: 1.4 }}>
                      {n.message}
                    </p>

                    <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                      {formatDateTime(n.createdAt)}
                    </span>
                  </div>
                </div>

                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', flexShrink: 0 }}>
                  {!n.isRead && (
                    <button
                      onClick={() => handleMarkAsRead(n.id)}
                      className="btn-finmate-secondary"
                      style={{ padding: '0.3rem 0.6rem', fontSize: '0.75rem' }}
                      title="Mark as read"
                    >
                      <Check size={13} />
                      Mark Read
                    </button>
                  )}
                  <button
                    onClick={() => handleDelete(n.id)}
                    style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', padding: '0.25rem' }}
                    title="Delete notification"
                  >
                    <Trash2 size={15} />
                  </button>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <EmptyState
            icon={Bell}
            title="All caught up!"
            description="You don't have any unread notifications or alerts right now."
          />
        )}
      </div>
    </div>
  );
}
