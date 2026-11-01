import React, { useState, useEffect } from 'react';
import { goalApi } from '../../api/goalApi';
import { formatCurrency, formatDate } from '../../utils/formatters';
import GoalModal from '../../components/goals/GoalModal';
import ContributionModal from '../../components/goals/ContributionModal';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import EmptyState from '../../components/common/EmptyState';
import {
  Target,
  Plus,
  PlusCircle,
  Clock,
  TrendingUp,
  ShieldCheck,
  CheckCircle2,
  Archive,
  Edit2,
  Calendar,
  AlertCircle
} from 'lucide-react';

export default function SavingsGoalsPage() {
  const [goals, setGoals] = useState([]);
  const [statusFilter, setStatusFilter] = useState('ACTIVE');
  const [freeBalance, setFreeBalance] = useState('0.00');
  const [loading, setLoading] = useState(true);

  // Modals
  const [isGoalModalOpen, setIsGoalModalOpen] = useState(false);
  const [isContributionModalOpen, setIsContributionModalOpen] = useState(false);
  const [selectedGoal, setSelectedGoal] = useState(null);

  const fetchGoalsData = async () => {
    setLoading(true);
    try {
      const [goalsRes, balanceRes] = await Promise.all([
        goalApi.getGoals(statusFilter),
        goalApi.getFreeBalance()
      ]);
      setGoals(goalsRes.data);
      setFreeBalance(balanceRes.data?.freeBalance || '0.00');
    } catch (err) {
      console.error('Failed to load goals data', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchGoalsData();
  }, [statusFilter]);

  const handleStatusChange = async (goalId, newStatus) => {
    try {
      await goalApi.setGoalStatus(goalId, newStatus);
      fetchGoalsData();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to update goal status');
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header */}
      <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '1.625rem', fontWeight: 700, color: 'var(--text-primary)', letterSpacing: '-0.02em' }}>
            Savings Goals & Targets
          </h1>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
            Dedicate liquid capital toward milestone purchases, emergency funds, and future security
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <select
            className="finmate-select"
            value={statusFilter}
            onChange={e => setStatusFilter(e.target.value)}
            style={{ width: 'auto' }}
          >
            <option value="ACTIVE">Active Goals</option>
            <option value="COMPLETED">Completed</option>
            <option value="ARCHIVED">Archived</option>
          </select>

          <button
            onClick={() => {
              setSelectedGoal(null);
              setIsGoalModalOpen(true);
            }}
            className="btn-finmate-primary"
          >
            <Plus size={16} />
            <span>New Savings Goal</span>
          </button>
        </div>
      </div>

      {/* Free Balance Quick Banner */}
      <div
        className="finmate-card"
        style={{
          padding: '1.25rem 1.5rem',
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '1rem',
          backgroundColor: 'var(--bg-card)'
        }}
      >
        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div
            style={{
              width: 44,
              height: 44,
              borderRadius: 'var(--radius-md)',
              backgroundColor: 'var(--success-bg)',
              color: 'var(--success-main)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}
          >
            <ShieldCheck size={22} />
          </div>
          <div>
            <div style={{ fontSize: '0.8125rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
              Unallocated Free Liquid Balance
            </div>
            <div className="tabular-nums" style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--text-primary)' }}>
              {formatCurrency(freeBalance)}
            </div>
          </div>
        </div>

        <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', maxWidth: 440, margin: 0 }}>
          Cash available in your ledger not yet committed to any savings goal. Safe to deposit without dipping into living expenses.
        </p>
      </div>

      {/* Goals Grid */}
      {loading ? (
        <div style={{ padding: '3rem 0' }}>
          <LoadingSpinner message="Loading savings milestones..." />
        </div>
      ) : goals.length > 0 ? (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '1.25rem' }}>
          {goals.map(g => {
            const percent = g.completionPercent || 0;
            const isCompleted = g.status === 'COMPLETED' || percent >= 100;

            return (
              <div key={g.id} className="finmate-card" style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.625rem' }}>
                    <div
                      style={{
                        width: 36,
                        height: 36,
                        borderRadius: 'var(--radius-md)',
                        backgroundColor: 'var(--primary-50)',
                        color: 'var(--primary-600)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center'
                      }}
                    >
                      <Target size={18} />
                    </div>
                    <div>
                      <h3 style={{ fontSize: '1rem', fontWeight: 700, color: 'var(--text-primary)', margin: 0 }}>
                        {g.name}
                      </h3>
                      <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                        Target: {formatDate(g.targetDate)}
                      </div>
                    </div>
                  </div>

                  <span className={`status-pill ${isCompleted ? 'success' : g.onTrack ? 'info' : 'warning'}`}>
                    {isCompleted ? 'Completed' : g.onTrack ? 'On Track' : 'Falling Behind'}
                  </span>
                </div>

                {/* Amounts */}
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
                  <div>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Saved</div>
                    <div className="tabular-nums" style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                      {formatCurrency(g.savedAmount)}
                    </div>
                  </div>
                  <div style={{ textAlign: 'right' }}>
                    <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Target</div>
                    <div className="tabular-nums" style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--text-secondary)' }}>
                      {formatCurrency(g.targetAmount)}
                    </div>
                  </div>
                </div>

                {/* Progress bar */}
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', color: 'var(--text-secondary)', marginBottom: '0.35rem' }}>
                    <span>{percent}% Funded</span>
                    <span>{formatCurrency(g.remainingAmount)} remaining</span>
                  </div>
                  <div style={{ height: 8, backgroundColor: 'var(--bg-tertiary)', borderRadius: 4, overflow: 'hidden' }}>
                    <div
                      style={{
                        height: '100%',
                        width: `${Math.min(100, percent)}%`,
                        backgroundColor: isCompleted ? 'var(--success-main)' : 'var(--primary-600)',
                        borderRadius: 4,
                        transition: 'width 0.3s ease'
                      }}
                    />
                  </div>
                </div>

                {/* Target pace pill */}
                {!isCompleted && (
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'space-between',
                      padding: '0.5rem 0.75rem',
                      backgroundColor: 'var(--bg-tertiary)',
                      borderRadius: 'var(--radius-md)',
                      fontSize: '0.75rem',
                      color: 'var(--text-secondary)'
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                      <Clock size={13} style={{ color: 'var(--text-muted)' }} />
                      <span>{g.daysLeft} days remaining</span>
                    </div>
                    <div>
                      Required: <strong className="tabular-nums">{formatCurrency(g.requiredMonthlySaving)}/mo</strong>
                    </div>
                  </div>
                )}

                {/* Card Actions */}
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingTop: '0.5rem', borderTop: '1px solid var(--border-color)' }}>
                  {!isCompleted ? (
                    <button
                      onClick={() => {
                        setSelectedGoal(g);
                        setIsContributionModalOpen(true);
                      }}
                      className="btn-finmate-primary"
                      style={{ padding: '0.4rem 0.75rem', fontSize: '0.75rem' }}
                    >
                      <PlusCircle size={14} />
                      Deposit
                    </button>
                  ) : (
                    <span style={{ fontSize: '0.75rem', color: 'var(--success-main)', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                      <CheckCircle2 size={15} /> Fully Funded!
                    </span>
                  )}

                  <div style={{ display: 'flex', gap: '0.4rem' }}>
                    <button
                      onClick={() => {
                        setSelectedGoal(g);
                        setIsGoalModalOpen(true);
                      }}
                      style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer', padding: '0.25rem' }}
                      title="Edit Goal"
                    >
                      <Edit2 size={15} />
                    </button>

                    {g.status === 'ACTIVE' && (
                      <button
                        onClick={() => handleStatusChange(g.id, 'COMPLETED')}
                        style={{ background: 'none', border: 'none', color: 'var(--success-main)', cursor: 'pointer', padding: '0.25rem' }}
                        title="Mark Completed"
                      >
                        <CheckCircle2 size={15} />
                      </button>
                    )}

                    {g.status === 'ACTIVE' && (
                      <button
                        onClick={() => handleStatusChange(g.id, 'ARCHIVED')}
                        style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', padding: '0.25rem' }}
                        title="Archive Goal"
                      >
                        <Archive size={15} />
                      </button>
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        <EmptyState
          icon={Target}
          title="No savings goals in this view"
          description="Create dedicated targets for future investments, gadgets, travel, or an emergency safety net."
          action={
            <button
              onClick={() => {
                setSelectedGoal(null);
                setIsGoalModalOpen(true);
              }}
              className="btn-finmate-primary"
            >
              <Plus size={16} />
              <span>Create First Goal</span>
            </button>
          }
        />
      )}

      {/* Modals */}
      <GoalModal
        isOpen={isGoalModalOpen}
        onClose={() => {
          setIsGoalModalOpen(false);
          setSelectedGoal(null);
        }}
        goal={selectedGoal}
        onSuccess={fetchGoalsData}
      />

      <ContributionModal
        isOpen={isContributionModalOpen}
        onClose={() => {
          setIsContributionModalOpen(false);
          setSelectedGoal(null);
        }}
        goal={selectedGoal}
        onSuccess={fetchGoalsData}
      />
    </div>
  );
}
