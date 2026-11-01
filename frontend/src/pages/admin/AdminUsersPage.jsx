import React, { useState, useEffect } from 'react';
import { adminApi } from '../../api/adminApi';
import { formatCurrency, formatDate } from '../../utils/formatters';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import EmptyState from '../../components/common/EmptyState';
import {
  ShieldCheck,
  Search,
  UserX,
  UserCheck,
  Shield,
  User,
  Users2
} from 'lucide-react';

export default function AdminUsersPage() {
  const [users, setUsers] = useState([]);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const resp = await adminApi.getUsers(page, 15, search);
      setUsers(resp.data.content || []);
      setTotalPages(resp.data.totalPages || 1);
      setTotalElements(resp.data.totalElements || 0);
    } catch (err) {
      console.error('Failed to load user directory', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, [page]);

  const handleSearch = e => {
    e.preventDefault();
    setPage(0);
    fetchUsers();
  };

  const handleToggleStatus = async (userId, currentStatus) => {
    const newStatus = currentStatus === 'ACTIVE' ? 'DISABLED' : 'ACTIVE';
    const actionLabel = newStatus === 'DISABLED' ? 'disable' : 'activate';
    if (window.confirm(`Are you sure you want to ${actionLabel} this user account?`)) {
      try {
        await adminApi.updateUserStatus(userId, newStatus);
        fetchUsers();
      } catch (err) {
        alert(err.response?.data?.message || 'Failed to update user status');
      }
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header */}
      <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: '1rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
            <span className="status-pill warning">Admin Console</span>
          </div>
          <h1 style={{ fontSize: '1.625rem', fontWeight: 700, color: 'var(--text-primary)', letterSpacing: '-0.02em' }}>
            User Directory & Access Control
          </h1>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
            Supervise registered platform accounts, audit statuses, and manage access privileges
          </p>
        </div>

        <div style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
          Total Accounts: <strong className="tabular-nums" style={{ color: 'var(--text-primary)' }}>{totalElements}</strong>
        </div>
      </div>

      {/* Search Toolbar */}
      <div className="finmate-card" style={{ padding: '0.875rem 1.25rem' }}>
        <form onSubmit={handleSearch} style={{ display: 'flex', gap: '0.75rem' }}>
          <div style={{ position: 'relative', flex: 1 }}>
            <Search size={16} style={{ position: 'absolute', left: 10, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
            <input
              type="text"
              className="finmate-input"
              placeholder="Search by full name or email address..."
              value={search}
              onChange={e => setSearch(e.target.value)}
              style={{ paddingLeft: '2rem' }}
            />
          </div>
          <button type="submit" className="btn-finmate-primary" style={{ padding: '0.5rem 1.25rem' }}>
            Search
          </button>
        </form>
      </div>

      {/* Users Table */}
      <div className="finmate-card" style={{ overflow: 'hidden' }}>
        {loading ? (
          <div style={{ padding: '3rem 0' }}>
            <LoadingSpinner message="Loading user accounts..." />
          </div>
        ) : users.length > 0 ? (
          <>
            <div style={{ overflowX: 'auto' }}>
              <table className="finmate-table">
                <thead>
                  <tr>
                    <th>User ID</th>
                    <th>Full Name</th>
                    <th>Email</th>
                    <th>Role</th>
                    <th>Status</th>
                    <th>Opening Balance</th>
                    <th>Joined</th>
                    <th style={{ textAlign: 'center', width: 110 }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map(u => (
                    <tr key={u.id}>
                      <td style={{ color: 'var(--text-muted)' }}>#{u.id}</td>
                      <td>
                        <strong style={{ color: 'var(--text-primary)' }}>{u.fullName}</strong>
                      </td>
                      <td style={{ color: 'var(--text-secondary)' }}>{u.email}</td>
                      <td>
                        <span className={`status-pill ${u.role === 'ADMIN' ? 'warning' : 'neutral'}`}>
                          {u.role}
                        </span>
                      </td>
                      <td>
                        <span
                          className={`status-pill ${
                            u.status === 'ACTIVE' ? 'success' : 'danger'
                          }`}
                        >
                          {u.status}
                        </span>
                      </td>
                      <td className="tabular-nums">
                        {formatCurrency(u.openingBalance)}
                      </td>
                      <td style={{ color: 'var(--text-secondary)' }}>
                        {formatDate(u.createdAt)}
                      </td>
                      <td style={{ textAlign: 'center' }}>
                        {u.role !== 'ADMIN' && (
                          <button
                            onClick={() => handleToggleStatus(u.id, u.status)}
                            className={u.status === 'ACTIVE' ? 'btn-finmate-secondary' : 'btn-finmate-primary'}
                            style={{
                              padding: '0.25rem 0.5rem',
                              fontSize: '0.6875rem',
                              color: u.status === 'ACTIVE' ? 'var(--danger-main)' : '#fff'
                            }}
                            title={u.status === 'ACTIVE' ? 'Disable account' : 'Activate account'}
                          >
                            {u.status === 'ACTIVE' ? (
                              <><UserX size={12} /> Disable</>
                            ) : (
                              <><UserCheck size={12} /> Activate</>
                            )}
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Pagination */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '0.875rem 1.25rem',
                borderTop: '1px solid var(--border-color)',
                fontSize: '0.8125rem',
                color: 'var(--text-secondary)'
              }}
            >
              <div>
                Page <strong>{page + 1}</strong> of <strong>{totalPages || 1}</strong>
              </div>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <button
                  disabled={page <= 0}
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  className="btn-finmate-secondary"
                  style={{ padding: '0.35rem 0.75rem', fontSize: '0.75rem' }}
                >
                  Previous
                </button>
                <button
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage(p => p + 1)}
                  className="btn-finmate-secondary"
                  style={{ padding: '0.35rem 0.75rem', fontSize: '0.75rem' }}
                >
                  Next
                </button>
              </div>
            </div>
          </>
        ) : (
          <EmptyState
            icon={Users2}
            title="No users found"
            description="No user records matched your search query."
          />
        )}
      </div>
    </div>
  );
}
