import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { groupApi } from '../../api/groupApi';
import { formatDate, getInitials } from '../../utils/formatters';
import CreateGroupModal from '../../components/groups/CreateGroupModal';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import EmptyState from '../../components/common/EmptyState';
import {
  Users2,
  Plus,
  ArrowRight,
  MailCheck,
  Check,
  X,
  Shield,
  Calendar
} from 'lucide-react';

export default function ExpenseGroupsPage() {
  const [groups, setGroups] = useState([]);
  const [invitations, setInvitations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);

  const fetchGroupsData = async () => {
    setLoading(true);
    try {
      const [groupsRes, invRes] = await Promise.all([
        groupApi.getGroups('ACTIVE'),
        groupApi.getPendingInvitations().catch(e => {
          console.warn('Could not fetch invitations', e);
          return { data: [] };
        })
      ]);
      setGroups(groupsRes.data || []);
      setInvitations(invRes.data || []);
    } catch (err) {
      console.error('Failed to load groups', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchGroupsData();
  }, []);

  const handleRespondInvitation = async (invitationId, accept) => {
  try {
    await groupApi.respondInvitation(invitationId, accept);
    fetchGroupsData();
  } catch (err) {
    alert(err.response?.data?.message || 'Failed to respond to invitation');
  }
};

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header */}
      <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '1.625rem', fontWeight: 700, color: 'var(--text-primary)', letterSpacing: '-0.02em' }}>
            Shared Expense Groups
          </h1>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
            Split trip tabs, roommate utilities, and project expenses with exact debt reconciliation
          </p>
        </div>

        <button
          onClick={() => setIsModalOpen(true)}
          className="btn-finmate-primary"
        >
          <Plus size={16} />
          <span>Create Expense Group</span>
        </button>
      </div>

      {/* Pending Invitations Banner */}
      {invitations.length > 0 && (
        <div
          className="finmate-card"
          style={{
            padding: '1rem 1.25rem',
            backgroundColor: 'var(--info-bg)',
            borderColor: 'var(--info-border)'
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
            <MailCheck size={18} style={{ color: 'var(--info-main)' }} />
            <h4 style={{ fontSize: '0.9375rem', fontWeight: 700, color: 'var(--info-text)', margin: 0 }}>
              Pending Group Invitations ({invitations.length})
            </h4>
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            {invitations.map(inv => (
              <div
                key={inv.id}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  padding: '0.5rem 0.75rem',
                  backgroundColor: 'var(--bg-card)',
                  borderRadius: 'var(--radius-md)',
                  fontSize: '0.8125rem'
                }}
              >
                <div>
                  You were invited to join <strong>{inv.groupName}</strong> by {inv.invitedBy?.fullName}
                </div>
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <button
                    onClick={() => handleRespondInvitation(inv.id, true)}
                    className="btn-finmate-primary"
                    style={{ padding: '0.25rem 0.6rem', fontSize: '0.75rem' }}
                  >
                    <Check size={13} /> Accept
                  </button>
                  <button
                    onClick={() => handleRespondInvitation(inv.id, false)}
                    className="btn-finmate-secondary"
                    style={{ padding: '0.25rem 0.6rem', fontSize: '0.75rem', color: 'var(--danger-main)' }}
                  >
                    <X size={13} /> Decline
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Groups Grid */}
      {loading ? (
        <div style={{ padding: '3rem 0' }}>
          <LoadingSpinner message="Loading shared groups..." />
        </div>
      ) : groups.length > 0 ? (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '1.25rem' }}>
          {groups.map(g => (
            <div
              key={g.id}
              className="finmate-card"
              style={{
                padding: '1.5rem',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                gap: '1.25rem'
              }}
            >
              <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.5rem' }}>
                  <h3 style={{ fontSize: '1.125rem', fontWeight: 700, color: 'var(--text-primary)', margin: 0 }}>
                    {g.name}
                  </h3>
                  <span className="status-pill info">
                    {g.memberCount} {g.memberCount === 1 ? 'member' : 'members'}
                  </span>
                </div>

                {g.description && (
                  <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', marginBottom: '0.75rem', lineHeight: 1.4 }}>
                    {g.description}
                  </p>
                )}

                <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                  <Calendar size={13} />
                  <span>Created {formatDate(g.createdAt)}</span>
                </div>
              </div>

              {/* Members Avatars preview */}
              <div style={{ borderTop: '1px solid var(--border-color)', paddingTop: '0.875rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', alignItems: 'center' }}>
                  {g.members?.slice(0, 4).map((m, idx) => (
                    <div
                      key={m.userId}
                      title={`${m.fullName} (${m.role})`}
                      style={{
                        width: 28,
                        height: 28,
                        borderRadius: '50%',
                        backgroundColor: 'var(--primary-100)',
                        color: 'var(--primary-700)',
                        fontSize: '0.6875rem',
                        fontWeight: 700,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        border: '2px solid var(--bg-card)',
                        marginLeft: idx === 0 ? 0 : -8
                      }}
                    >
                      {getInitials(m.fullName)}
                    </div>
                  ))}
                  {g.memberCount > 4 && (
                    <div
                      style={{
                        width: 28,
                        height: 28,
                        borderRadius: '50%',
                        backgroundColor: 'var(--bg-tertiary)',
                        color: 'var(--text-secondary)',
                        fontSize: '0.6875rem',
                        fontWeight: 600,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        border: '2px solid var(--bg-card)',
                        marginLeft: -8
                      }}
                    >
                      +{g.memberCount - 4}
                    </div>
                  )}
                </div>

                <Link
                  to={`/groups/${g.id}`}
                  className="btn-finmate-secondary"
                  style={{ fontSize: '0.75rem', padding: '0.35rem 0.65rem' }}
                >
                  Open Workspace
                  <ArrowRight size={13} />
                </Link>
              </div>
            </div>
          ))}
        </div>
      ) : (
        <EmptyState
          icon={Users2}
          title="No shared expense groups yet"
          description="Create a group to split trip tabs, rent, or dinner expenses with friends and track who owes whom."
          action={
            <button
              onClick={() => setIsModalOpen(true)}
              className="btn-finmate-primary"
            >
              <Plus size={16} />
              <span>Create First Group</span>
            </button>
          }
        />
      )}

      <CreateGroupModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSuccess={fetchGroupsData}
      />
    </div>
  );
}
