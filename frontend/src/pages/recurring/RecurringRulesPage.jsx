import React, { useState, useEffect } from 'react';
import { transactionApi } from '../../api/transactionApi';
import { formatCurrency, formatDate } from '../../utils/formatters';
import RecurringRuleModal from '../../components/recurring/RecurringRuleModal';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import EmptyState from '../../components/common/EmptyState';
import {
  Repeat,
  Plus,
  Play,
  Edit2,
  Trash2,
  Calendar,
  CheckCircle,
  PauseCircle
} from 'lucide-react';

export default function RecurringRulesPage() {
  const [rules, setRules] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingRule, setEditingRule] = useState(null);
  const [actionMessage, setActionMessage] = useState('');

  const fetchRules = async () => {
    setLoading(true);
    try {
      const resp = await transactionApi.getRecurringRules();
      setRules(resp.data);
    } catch (err) {
      console.error('Failed to load recurring rules', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRules();
  }, []);

  const handleToggle = async id => {
    try {
      await transactionApi.toggleRecurringRule(id);
      fetchRules();
    } catch (err) {
      alert('Failed to update recurring rule status');
    }
  };

  const handleRunNow = async id => {
    try {
      await transactionApi.runRecurringRuleNow(id);
      setActionMessage('Recurring rule executed successfully! New transaction recorded in ledger.');
      setTimeout(() => setActionMessage(''), 4000);
      fetchRules();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to trigger rule execution');
    }
  };

  const handleDelete = async id => {
    if (window.confirm('Are you sure you want to delete this recurring rule?')) {
      try {
        await transactionApi.deleteRecurringRule(id);
        fetchRules();
      } catch (err) {
        alert('Failed to delete recurring rule');
      }
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header */}
      <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '1.625rem', fontWeight: 700, color: 'var(--text-primary)', letterSpacing: '-0.02em' }}>
            Recurring Rules & Subscriptions
          </h1>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
            Automate routine salaries, apartment rent, streaming subscriptions, and scheduled bills
          </p>
        </div>

        <button
          onClick={() => {
            setEditingRule(null);
            setIsModalOpen(true);
          }}
          className="btn-finmate-primary"
        >
          <Plus size={16} />
          <span>New Recurring Rule</span>
        </button>
      </div>

      {actionMessage && (
        <div
          style={{
            backgroundColor: 'var(--success-bg)',
            border: '1px solid var(--success-border)',
            color: 'var(--success-text)',
            padding: '0.75rem 1rem',
            borderRadius: 'var(--radius-md)',
            fontSize: '0.875rem',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem'
          }}
        >
          <CheckCircle size={18} />
          <span>{actionMessage}</span>
        </div>
      )}

      {/* Rules Table */}
      <div className="finmate-card" style={{ overflow: 'hidden' }}>
        {loading ? (
          <div style={{ padding: '3rem 0' }}>
            <LoadingSpinner message="Loading recurring schedules..." />
          </div>
        ) : rules.length > 0 ? (
          <div style={{ overflowX: 'auto' }}>
            <table className="finmate-table">
              <thead>
                <tr>
                  <th>Description</th>
                  <th>Cadence</th>
                  <th>Category</th>
                  <th style={{ textAlign: 'right' }}>Amount</th>
                  <th>Next Execution</th>
                  <th style={{ textAlign: 'center' }}>Status</th>
                  <th style={{ textAlign: 'center', width: 130 }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {rules.map(r => {
                  const isIncome = r.type === 'INCOME';
                  return (
                    <tr key={r.id}>
                      <td>
                        <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                          {r.description}
                        </div>
                        <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                          Method: {r.paymentMethod}
                        </div>
                      </td>
                      <td>
                        <span className="status-pill neutral">
                          {r.frequency}
                        </span>
                      </td>
                      <td>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                          <span
                            style={{
                              width: 8,
                              height: 8,
                              borderRadius: '50%',
                              backgroundColor: r.category?.color || '#94a3b8'
                            }}
                          />
                          <span>{r.category?.name || 'General'}</span>
                        </div>
                      </td>
                      <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                        <span
                          className="tabular-nums"
                          style={{
                            fontWeight: 700,
                            color: isIncome ? 'var(--success-main)' : 'var(--text-primary)'
                          }}
                        >
                          {isIncome ? '+' : '-'}{formatCurrency(r.amount)}
                        </span>
                      </td>
                      <td style={{ whiteSpace: 'nowrap', color: 'var(--text-secondary)' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                          <Calendar size={14} style={{ color: 'var(--text-muted)' }} />
                          <span>{formatDate(r.nextRunDate)}</span>
                        </div>
                      </td>
                      <td style={{ textAlign: 'center' }}>
                        <button
                          onClick={() => handleToggle(r.id)}
                          style={{
                            background: 'none',
                            border: 'none',
                            cursor: 'pointer',
                            padding: '0.2rem'
                          }}
                          title={r.isActive ? 'Click to Pause' : 'Click to Activate'}
                        >
                          <span className={`status-pill ${r.isActive ? 'success' : 'neutral'}`}>
                            {r.isActive ? 'Active' : 'Paused'}
                          </span>
                        </button>
                      </td>
                      <td style={{ textAlign: 'center' }}>
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }}>
                          <button
                            onClick={() => handleRunNow(r.id)}
                            style={{
                              background: 'none',
                              border: 'none',
                              color: 'var(--primary-600)',
                              cursor: 'pointer',
                              padding: '0.25rem'
                            }}
                            title="Run Now"
                          >
                            <Play size={15} />
                          </button>
                          <button
                            onClick={() => {
                              setEditingRule(r);
                              setIsModalOpen(true);
                            }}
                            style={{
                              background: 'none',
                              border: 'none',
                              color: 'var(--text-secondary)',
                              cursor: 'pointer',
                              padding: '0.25rem'
                            }}
                            title="Edit"
                          >
                            <Edit2 size={15} />
                          </button>
                          <button
                            onClick={() => handleDelete(r.id)}
                            style={{
                              background: 'none',
                              border: 'none',
                              color: 'var(--danger-main)',
                              cursor: 'pointer',
                              padding: '0.25rem'
                            }}
                            title="Delete"
                          >
                            <Trash2 size={15} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState
            icon={Repeat}
            title="No recurring rules scheduled"
            description="Automate repetitive expenses and income streams so your ledger stays up-to-date effortlessly."
            action={
              <button
                onClick={() => {
                  setEditingRule(null);
                  setIsModalOpen(true);
                }}
                className="btn-finmate-primary"
              >
                <Plus size={16} />
                <span>New Recurring Rule</span>
              </button>
            }
          />
        )}
      </div>

      <RecurringRuleModal
        isOpen={isModalOpen}
        onClose={() => {
          setIsModalOpen(false);
          setEditingRule(null);
        }}
        rule={editingRule}
        onSuccess={fetchRules}
      />
    </div>
  );
}
