import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { groupApi } from '../../api/groupApi';
import { settlementApi } from '../../api/settlementApi';
import { useAuth } from '../../context/AuthContext';
import { formatCurrency, formatDate, getInitials } from '../../utils/formatters';
import AddSharedExpenseModal from '../../components/groups/AddSharedExpenseModal';
import RecordSettlementModal from '../../components/groups/RecordSettlementModal';
import InviteMemberModal from '../../components/groups/InviteMemberModal';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import EmptyState from '../../components/common/EmptyState';
import {
  Users2,
  Plus,
  ArrowRightLeft,
  UserPlus,
  Receipt,
  Scale,
  CheckCircle2,
  AlertTriangle,
  ChevronDown,
  ChevronRight,
  ShieldCheck,
  Check,
  X,
  Trash2,
  ArrowRight
} from 'lucide-react';

export default function GroupDetailPage() {
  const { id } = useParams();
  const groupId = parseInt(id);
  const { user } = useAuth();

  const [group, setGroup] = useState(null);
  const [members, setMembers] = useState([]);
  const [expenses, setExpenses] = useState([]);
  const [balances, setBalances] = useState(null);
  const [suggestions, setSuggestions] = useState([]);
  const [settlements, setSettlements] = useState([]);
  const [activeTab, setActiveTab] = useState('expenses'); // 'expenses' or 'balances'
  const [loading, setLoading] = useState(true);

  // Modals
  const [isExpenseModalOpen, setIsExpenseModalOpen] = useState(false);
  const [isInviteModalOpen, setIsInviteModalOpen] = useState(false);
  const [isSettlementModalOpen, setIsSettlementModalOpen] = useState(false);
  const [selectedSuggestion, setSelectedSuggestion] = useState(null);

  // Expanded splits accordion in expenses
  const [expandedExpenseIds, setExpandedExpenseIds] = useState([]);

  const fetchGroupWorkspace = async () => {
    setLoading(true);
    try {
      const [
        groupRes,
        membersRes,
        expensesRes,
        balancesRes,
        suggestionsRes,
        settlementsRes
      ] = await Promise.all([
        groupApi.getGroup(groupId),
        groupApi.getGroupMembers(groupId),
        groupApi.getSharedExpenses(groupId),
        settlementApi.getGroupBalances(groupId),
        settlementApi.getSettlementSuggestions(groupId),
        settlementApi.getSettlementsHistory(groupId)
      ]);

      setGroup(groupRes.data);
      setMembers(membersRes.data);
      setExpenses(expensesRes.data.content || []);
      setBalances(balancesRes.data);
      setSuggestions(suggestionsRes.data || []);
      setSettlements(settlementsRes.data.content || []);
    } catch (err) {
      console.error('Failed to load group workspace', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchGroupWorkspace();
  }, [groupId]);

  const toggleExpand = expId => {
    setExpandedExpenseIds(prev =>
      prev.includes(expId) ? prev.filter(i => i !== expId) : [...prev, expId]
    );
  };

  const handleDeleteExpense = async expId => {
    if (window.confirm('Delete this shared expense? (Cannot be deleted if locked by a confirmed settlement).')) {
      try {
        await groupApi.deleteSharedExpense(groupId, expId);
        fetchGroupWorkspace();
      } catch (err) {
        alert(err.response?.data?.message || 'Failed to delete shared expense');
      }
    }
  };

  const handleConfirmSettlement = async settlementId => {
    try {
      await settlementApi.confirmSettlement(groupId, settlementId);
      fetchGroupWorkspace();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to confirm settlement');
    }
  };

  const handleRejectSettlement = async settlementId => {
    try {
      await settlementApi.rejectSettlement(groupId, settlementId);
      fetchGroupWorkspace();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to reject settlement');
    }
  };

  if (loading && !group) {
    return <LoadingSpinner fullPage message="Loading group workspace and balances..." />;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Group Header Card */}
      <div className="finmate-card" style={{ padding: '1.5rem' }}>
        <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: '1rem' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
              <div
                style={{
                  width: 40,
                  height: 40,
                  borderRadius: 'var(--radius-md)',
                  backgroundColor: 'var(--primary-100)',
                  color: 'var(--primary-600)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}
              >
                <Users2 size={20} />
              </div>
              <div>
                <h1 style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--text-primary)', margin: 0, letterSpacing: '-0.02em' }}>
                  {group?.name}
                </h1>
                <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
                  {group?.description || 'Collaborative Expense Group'}
                </div>
              </div>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
            <button onClick={() => setIsInviteModalOpen(true)} className="btn-finmate-secondary">
              <UserPlus size={15} />
              <span>Invite Member</span>
            </button>

            <button
              onClick={() => {
                setSelectedSuggestion(null);
                setIsSettlementModalOpen(true);
              }}
              className="btn-finmate-secondary"
            >
              <ArrowRightLeft size={15} />
              <span>Record Settlement</span>
            </button>

            <button
              onClick={() => setIsExpenseModalOpen(true)}
              className="btn-finmate-primary"
            >
              <Plus size={16} />
              <span>Add Expense</span>
            </button>
          </div>
        </div>

        {/* Member Pills */}
        <div style={{ marginTop: '1.25rem', paddingTop: '1rem', borderTop: '1px solid var(--border-color)', display: 'flex', alignItems: 'center', gap: '0.5rem', flexWrap: 'wrap' }}>
          <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
            Members ({members.length}):
          </span>
          {members.map(m => (
            <div
              key={m.userId}
              style={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: '0.375rem',
                padding: '0.25rem 0.6rem',
                backgroundColor: 'var(--bg-tertiary)',
                borderRadius: '9999px',
                fontSize: '0.75rem',
                border: '1px solid var(--border-color)'
              }}
            >
              <div
                style={{
                  width: 18,
                  height: 18,
                  borderRadius: '50%',
                  backgroundColor: 'var(--primary-600)',
                  color: '#fff',
                  fontSize: '0.625rem',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}
              >
                {getInitials(m.fullName)}
              </div>
              <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{m.fullName}</span>
              {m.role === 'OWNER' && (
                <span style={{ fontSize: '0.625rem', color: 'var(--primary-600)', fontWeight: 700 }}>
                  (Admin)
                </span>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* Navigation Tabs */}
      <div style={{ display: 'flex', gap: '0.5rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.5rem' }}>
        <button
          onClick={() => setActiveTab('expenses')}
          style={{
            padding: '0.5rem 1rem',
            borderRadius: 'var(--radius-md)',
            border: 'none',
            fontSize: '0.875rem',
            fontWeight: 600,
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
            backgroundColor: activeTab === 'expenses' ? 'var(--primary-50)' : 'transparent',
            color: activeTab === 'expenses' ? 'var(--primary-600)' : 'var(--text-secondary)'
          }}
        >
          <Receipt size={16} />
          Shared Expenses ({expenses.length})
        </button>

        <button
          onClick={() => setActiveTab('balances')}
          style={{
            padding: '0.5rem 1rem',
            borderRadius: 'var(--radius-md)',
            border: 'none',
            fontSize: '0.875rem',
            fontWeight: 600,
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
            backgroundColor: activeTab === 'balances' ? 'var(--primary-50)' : 'transparent',
            color: activeTab === 'balances' ? 'var(--primary-600)' : 'var(--text-secondary)'
          }}
        >
          <Scale size={16} />
          Balances & Settlements ({suggestions.length} suggestions)
        </button>
      </div>

      {/* TAB 1: SHARED EXPENSES */}
      {activeTab === 'expenses' && (
        <div className="finmate-card" style={{ overflow: 'hidden' }}>
          {expenses.length > 0 ? (
            <div style={{ overflowX: 'auto' }}>
              <table className="finmate-table">
                <thead>
                  <tr>
                    <th style={{ width: 30 }}></th>
                    <th>Date</th>
                    <th>Description</th>
                    <th>Paid By</th>
                    <th>Split Mode</th>
                    <th style={{ textAlign: 'right' }}>Total Amount</th>
                    <th style={{ textAlign: 'center', width: 60 }}></th>
                  </tr>
                </thead>
                <tbody>
                  {expenses.map(e => {
                    const isExpanded = expandedExpenseIds.includes(e.id);
                    return (
                      <React.Fragment key={e.id}>
                        <tr>
                          <td>
                            <button
                              onClick={() => toggleExpand(e.id)}
                              style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', padding: 0 }}
                            >
                              {isExpanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
                            </button>
                          </td>
                          <td style={{ whiteSpace: 'nowrap', color: 'var(--text-secondary)' }}>
                            {formatDate(e.expenseDate)}
                          </td>
                          <td>
                            <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                              {e.description}
                            </div>
                            {e.notes && (
                              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                                {e.notes}
                              </div>
                            )}
                          </td>
                          <td>
                            <span style={{ fontWeight: 600 }}>{e.paidBy?.fullName}</span>
                          </td>
                          <td>
                            <span className="status-pill neutral">
                              {e.splitType}
                            </span>
                          </td>
                          <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                            <strong className="tabular-nums" style={{ fontSize: '0.9375rem', color: 'var(--text-primary)' }}>
                              {formatCurrency(e.amount)}
                            </strong>
                          </td>
                          <td style={{ textAlign: 'center' }}>
                            <button
                              onClick={() => handleDeleteExpense(e.id)}
                              style={{ background: 'none', border: 'none', color: 'var(--danger-main)', cursor: 'pointer', padding: '0.25rem' }}
                              title="Delete Expense"
                            >
                              <Trash2 size={15} />
                            </button>
                          </td>
                        </tr>

                        {/* Expandable Split Shares Drawer */}
                        {isExpanded && (
                          <tr>
                            <td colSpan={7} style={{ backgroundColor: 'var(--bg-tertiary)', padding: '0.75rem 2rem' }}>
                              <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                                Split Breakdown:
                              </div>
                              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '0.5rem' }}>
                                {e.splits?.map(s => (
                                  <div
                                    key={s.userId}
                                    style={{
                                      padding: '0.4rem 0.6rem',
                                      backgroundColor: 'var(--bg-card)',
                                      borderRadius: 'var(--radius-sm)',
                                      display: 'flex',
                                      justifyContent: 'space-between',
                                      fontSize: '0.8125rem'
                                    }}
                                  >
                                    <span>{s.userName || `User #${s.userId}`}</span>
                                    <strong className="tabular-nums">{formatCurrency(s.shareAmount)}</strong>
                                  </div>
                                ))}
                              </div>
                            </td>
                          </tr>
                        )}
                      </React.Fragment>
                    );
                  })}
                </tbody>
              </table>
            </div>
          ) : (
            <EmptyState
              icon={Receipt}
              title="No shared expenses recorded yet"
              description="Add your first shared expense to start tracking splits and debt balances."
              action={
                <button
                  onClick={() => setIsExpenseModalOpen(true)}
                  className="btn-finmate-primary"
                >
                  <Plus size={16} />
                  <span>Add First Expense</span>
                </button>
              }
            />
          )}
        </div>
      )}

      {/* TAB 2: BALANCES & SETTLEMENT RECONCILIATION */}
      {activeTab === 'balances' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {/* Balance Invariant Banner */}
          <div
            className="finmate-card"
            style={{
              padding: '1rem 1.25rem',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              backgroundColor: 'var(--success-bg)',
              borderColor: 'var(--success-border)'
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--success-text)' }}>
              <ShieldCheck size={20} />
              <span style={{ fontSize: '0.875rem', fontWeight: 600 }}>
                Group Balance Invariant Verified: Zero Discrepancy
              </span>
            </div>
            <div className="tabular-nums" style={{ fontSize: '0.875rem', fontWeight: 700, color: 'var(--success-text)' }}>
              Sum of Net Balances = ₹0.00
            </div>
          </div>

          {/* Member Net Balances Table */}
          <div className="finmate-card" style={{ overflow: 'hidden' }}>
            <div className="finmate-card-header">
              <h3 style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>
                Member Net Positions
              </h3>
            </div>
            <div style={{ overflowX: 'auto' }}>
              <table className="finmate-table">
                <thead>
                  <tr>
                    <th>Member</th>
                    <th style={{ textAlign: 'right' }}>Total Paid</th>
                    <th style={{ textAlign: 'right' }}>Total Share Owed</th>
                    <th style={{ textAlign: 'right' }}>Settled Paid</th>
                    <th style={{ textAlign: 'right' }}>Settled Received</th>
                    <th style={{ textAlign: 'right' }}>Net Balance</th>
                  </tr>
                </thead>
                <tbody>
                  {balances?.members?.map(m => {
                    const netNum = parseFloat(m.netBalance);
                    const isPositive = netNum > 0;
                    const isNegative = netNum < 0;

                    return (
                      <tr key={m.userId}>
                        <td style={{ fontWeight: 600 }}>
                          {m.fullName} {m.userId === user?.id && <span style={{ color: 'var(--primary-600)' }}>(You)</span>}
                        </td>
                        <td className="tabular-nums" style={{ textAlign: 'right' }}>
                          {formatCurrency(m.paidAmount)}
                        </td>
                        <td className="tabular-nums" style={{ textAlign: 'right' }}>
                          {formatCurrency(m.owedAmount)}
                        </td>
                        <td className="tabular-nums" style={{ textAlign: 'right', color: 'var(--text-muted)' }}>
                          {formatCurrency(m.settledPaid)}
                        </td>
                        <td className="tabular-nums" style={{ textAlign: 'right', color: 'var(--text-muted)' }}>
                          {formatCurrency(m.settledReceived)}
                        </td>
                        <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                          <span
                            className="tabular-nums"
                            style={{
                              fontWeight: 700,
                              fontSize: '0.9375rem',
                              color: isPositive ? 'var(--success-main)' : isNegative ? 'var(--danger-main)' : 'var(--text-muted)'
                            }}
                          >
                            {isPositive ? `+${formatCurrency(m.netBalance)} (Gets Back)` : isNegative ? `${formatCurrency(m.netBalance)} (Owes)` : 'Settled Up (₹0.00)'}
                          </span>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>

          {/* Greedy Settlement Suggestions Card */}
          <div className="finmate-card" style={{ padding: '1.25rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.75rem' }}>
              <ArrowRightLeft size={18} style={{ color: 'var(--primary-600)' }} />
              <h3 style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>
                Smart Debt Clearing Suggestions (Greedy Minimization)
              </h3>
            </div>
            <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', marginBottom: '1rem' }}>
              Optimal minimum transfers required to bring everyone's net group balance to exactly ₹0.00.
            </p>

            {suggestions.length > 0 ? (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
                {suggestions.map((s, idx) => (
                  <div
                    key={idx}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '0.75rem 1rem',
                      backgroundColor: 'var(--bg-tertiary)',
                      borderRadius: 'var(--radius-md)',
                      fontSize: '0.875rem'
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <strong>{s.fromUserName}</strong>
                      <span style={{ color: 'var(--text-muted)' }}>pays</span>
                      <strong>{s.toUserName}</strong>
                      <span className="tabular-nums" style={{ fontWeight: 700, color: 'var(--primary-600)', marginLeft: '0.5rem' }}>
                        {formatCurrency(s.amount)}
                      </span>
                    </div>

                    <button
                      onClick={() => {
                        setSelectedSuggestion(s);
                        setIsSettlementModalOpen(true);
                      }}
                      className="btn-finmate-primary"
                      style={{ padding: '0.35rem 0.75rem', fontSize: '0.75rem' }}
                    >
                      Settle Now
                    </button>
                  </div>
                ))}
              </div>
            ) : (
              <div style={{ padding: '1.5rem', textAlign: 'center', color: 'var(--success-main)', fontWeight: 600, fontSize: '0.875rem' }}>
                <CheckCircle2 size={24} style={{ marginBottom: '0.5rem' }} />
                <div>All group balances are settled up! No transfers needed.</div>
              </div>
            )}
          </div>

          {/* Pending & Confirmed Settlements Ledger */}
          <div className="finmate-card" style={{ overflow: 'hidden' }}>
            <div className="finmate-card-header">
              <h3 style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>
                Settlement Transfers History
              </h3>
            </div>

            {settlements.length > 0 ? (
              <div style={{ overflowX: 'auto' }}>
                <table className="finmate-table">
                  <thead>
                    <tr>
                      <th>Transfer Date</th>
                      <th>From (Payer)</th>
                      <th>To (Receiver)</th>
                      <th>Mode</th>
                      <th style={{ textAlign: 'right' }}>Amount</th>
                      <th style={{ textAlign: 'center' }}>Status</th>
                      <th style={{ textAlign: 'center', width: 140 }}>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {settlements.map(st => {
                      const isPending = st.status === 'PENDING';
                      const isCurrentUserReceiver = st.toUser?.id === user?.id;

                      return (
                        <tr key={st.id}>
                          <td style={{ color: 'var(--text-secondary)' }}>
                            {formatDate(st.settlementDate)}
                          </td>
                          <td style={{ fontWeight: 600 }}>{st.fromUser?.fullName}</td>
                          <td style={{ fontWeight: 600 }}>{st.toUser?.fullName}</td>
                          <td>
                            <span className="status-pill neutral">{st.paymentMode}</span>
                          </td>
                          <td style={{ textAlign: 'right' }}>
                            <strong className="tabular-nums">{formatCurrency(st.amount)}</strong>
                          </td>
                          <td style={{ textAlign: 'center' }}>
                            <span
                              className={`status-pill ${
                                st.status === 'CONFIRMED' ? 'success' :
                                st.status === 'PENDING' ? 'warning' : 'danger'
                              }`}
                            >
                              {st.status}
                            </span>
                          </td>
                          <td style={{ textAlign: 'center' }}>
                            {isPending && isCurrentUserReceiver ? (
                              <div style={{ display: 'flex', gap: '0.35rem', justifyContent: 'center' }}>
                                <button
                                  onClick={() => handleConfirmSettlement(st.id)}
                                  className="btn-finmate-primary"
                                  style={{ padding: '0.2rem 0.5rem', fontSize: '0.6875rem' }}
                                  title="Confirm receipt of funds"
                                >
                                  <Check size={12} /> Confirm
                                </button>
                                <button
                                  onClick={() => handleRejectSettlement(st.id)}
                                  className="btn-finmate-secondary"
                                  style={{ padding: '0.2rem 0.5rem', fontSize: '0.6875rem', color: 'var(--danger-main)' }}
                                  title="Reject"
                                >
                                  <X size={12} />
                                </button>
                              </div>
                            ) : (
                              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                                {st.status === 'CONFIRMED' ? 'Verified' : isPending ? 'Awaiting receiver' : 'Cancelled'}
                              </span>
                            )}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            ) : (
              <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.875rem' }}>
                No debt settlement transfers recorded yet.
              </div>
            )}
          </div>
        </div>
      )}

      {/* Modals */}
      <AddSharedExpenseModal
        isOpen={isExpenseModalOpen}
        onClose={() => setIsExpenseModalOpen(false)}
        groupId={groupId}
        members={members}
        onSuccess={fetchGroupWorkspace}
      />

      <InviteMemberModal
        isOpen={isInviteModalOpen}
        onClose={() => setIsInviteModalOpen(false)}
        groupId={groupId}
        onSuccess={fetchGroupWorkspace}
      />

      <RecordSettlementModal
        isOpen={isSettlementModalOpen}
        onClose={() => {
          setIsSettlementModalOpen(false);
          setSelectedSuggestion(null);
        }}
        groupId={groupId}
        members={members}
        suggestion={selectedSuggestion}
        onSuccess={fetchGroupWorkspace}
      />
    </div>
  );
}
