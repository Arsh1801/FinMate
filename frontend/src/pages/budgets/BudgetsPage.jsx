import React, { useState, useEffect } from 'react';
import { budgetApi } from '../../api/budgetApi';
import { formatCurrency } from '../../utils/formatters';
import BudgetModal from '../../components/budgets/BudgetModal';
import CopyBudgetModal from '../../components/budgets/CopyBudgetModal';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import EmptyState from '../../components/common/EmptyState';
import {
  PiggyBank,
  Plus,
  Copy,
  Edit2,
  Trash2,
  AlertTriangle,
  CheckCircle,
  TrendingDown,
  Calendar
} from 'lucide-react';

export default function BudgetsPage() {
  const today = new Date();
  const [year, setYear] = useState(today.getFullYear());
  const [month, setMonth] = useState(today.getMonth() + 1);
  const [budgets, setBudgets] = useState([]);
  const [loading, setLoading] = useState(true);

  // Modals
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isCopyModalOpen, setIsCopyModalOpen] = useState(false);
  const [editingBudget, setEditingBudget] = useState(null);

  const fetchBudgets = async () => {
    setLoading(true);
    try {
      const resp = await budgetApi.getBudgetsForMonth(year, month);
      setBudgets(resp.data);
    } catch (err) {
      console.error('Failed to load budgets', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchBudgets();
  }, [year, month]);

  const overallBudget = budgets.find(b => b.category === null);
  const categoryBudgets = budgets.filter(b => b.category !== null);

  const handleDelete = async id => {
    if (window.confirm('Are you sure you want to delete this budget cap?')) {
      try {
        await budgetApi.deleteBudget(id);
        fetchBudgets();
      } catch (err) {
        alert('Failed to delete budget');
      }
    }
  };

  const monthName = new Date(year, month - 1).toLocaleString('default', { month: 'long', year: 'numeric' });

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header */}
      <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '1.625rem', fontWeight: 700, color: 'var(--text-primary)', letterSpacing: '-0.02em' }}>
            Budgets & Expense Guardrails
          </h1>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
            Plan category limits, prevent lifestyle creep, and monitor threshold warning triggers
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', flexWrap: 'wrap' }}>
          {/* Month & Year Selectors */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', backgroundColor: 'var(--bg-card)', padding: '0.25rem 0.5rem', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-color)' }}>
            <Calendar size={16} style={{ color: 'var(--text-muted)' }} />
            <select
              className="finmate-select"
              value={month}
              onChange={e => setMonth(parseInt(e.target.value))}
              style={{ border: 'none', padding: '0.25rem 0.5rem', background: 'transparent' }}
            >
              {Array.from({ length: 12 }, (_, i) => i + 1).map(m => (
                <option key={m} value={m}>
                  {new Date(2024, m - 1).toLocaleString('default', { month: 'short' })}
                </option>
              ))}
            </select>
            <input
              type="number"
              className="finmate-input"
              value={year}
              onChange={e => setYear(parseInt(e.target.value) || year)}
              style={{ width: 75, border: 'none', padding: '0.25rem', background: 'transparent' }}
            />
          </div>

          <button onClick={() => setIsCopyModalOpen(true)} className="btn-finmate-secondary">
            <Copy size={15} />
            <span>Copy Prior Month</span>
          </button>

          <button
            onClick={() => {
              setEditingBudget(null);
              setIsModalOpen(true);
            }}
            className="btn-finmate-primary"
          >
            <Plus size={16} />
            <span>New Budget</span>
          </button>
        </div>
      </div>

      {loading ? (
        <div style={{ padding: '3rem 0' }}>
          <LoadingSpinner message="Calculating budget pacing..." />
        </div>
      ) : (
        <>
          {/* Overall Month Budget Banner */}
          {overallBudget ? (
            <div
              className="finmate-card"
              style={{
                padding: '1.25rem 1.5rem',
                borderLeft: `5px solid ${
                  overallBudget.exceeded ? 'var(--danger-main)' :
                  overallBudget.status === 'WARNING' ? 'var(--warning-main)' : 'var(--primary-600)'
                }`
              }}
            >
              <div style={{ display: 'flex', flexWrap: 'wrap', justifyContent: 'space-between', alignItems: 'flex-start', gap: '1rem', marginBottom: '1rem' }}>
                <div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                    <h3 style={{ fontSize: '1.125rem', fontWeight: 700, color: 'var(--text-primary)', margin: 0 }}>
                      Overall Month Budget ({monthName})
                    </h3>
                    <span
                      className={`status-pill ${
                        overallBudget.exceeded ? 'danger' :
                        overallBudget.status === 'WARNING' ? 'warning' : 'success'
                      }`}
                    >
                      {overallBudget.exceeded ? 'Exceeded' : overallBudget.status === 'WARNING' ? 'Warning Pacing' : 'On Track'}
                    </span>
                  </div>
                  <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', marginTop: '0.2rem' }}>
                    Warning trigger set at {overallBudget.alertThreshold}% of total allowance
                  </p>
                </div>

                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <button
                    onClick={() => {
                      setEditingBudget(overallBudget);
                      setIsModalOpen(true);
                    }}
                    className="btn-finmate-secondary"
                    style={{ padding: '0.35rem 0.6rem', fontSize: '0.75rem' }}
                  >
                    <Edit2 size={13} /> Edit
                  </button>
                  <button
                    onClick={() => handleDelete(overallBudget.id)}
                    className="btn-finmate-secondary"
                    style={{ padding: '0.35rem 0.6rem', fontSize: '0.75rem', color: 'var(--danger-main)' }}
                  >
                    <Trash2 size={13} />
                  </button>
                </div>
              </div>

              {/* Progress metrics */}
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '1rem', marginBottom: '0.75rem' }}>
                <div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Spent to Date</div>
                  <div className="tabular-nums" style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                    {formatCurrency(overallBudget.spentAmount)}
                  </div>
                </div>
                <div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Budget Limit Cap</div>
                  <div className="tabular-nums" style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                    {formatCurrency(overallBudget.limitAmount)}
                  </div>
                </div>
                <div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Remaining Margin</div>
                  <div className="tabular-nums" style={{ fontSize: '1.25rem', fontWeight: 700, color: overallBudget.exceeded ? 'var(--danger-main)' : 'var(--success-main)' }}>
                    {formatCurrency(overallBudget.remainingAmount)}
                  </div>
                </div>
                <div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Consumed</div>
                  <div className="tabular-nums" style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                    {overallBudget.utilizationPercent}%
                  </div>
                </div>
              </div>

              {/* Bar */}
              <div style={{ height: 10, backgroundColor: 'var(--bg-tertiary)', borderRadius: 5, overflow: 'hidden' }}>
                <div
                  style={{
                    height: '100%',
                    width: `${Math.min(100, overallBudget.utilizationPercent || 0)}%`,
                    backgroundColor: overallBudget.exceeded ? 'var(--danger-main)' : overallBudget.status === 'WARNING' ? 'var(--warning-main)' : 'var(--success-main)',
                    borderRadius: 5,
                    transition: 'width 0.3s'
                  }}
                />
              </div>
            </div>
          ) : (
            <div
              className="finmate-card"
              style={{
                padding: '1.25rem 1.5rem',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                borderStyle: 'dashed'
              }}
            >
              <div>
                <h4 style={{ fontSize: '0.9375rem', fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>
                  No overall monthly budget established for {monthName}
                </h4>
                <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', margin: 0 }}>
                  Establish an umbrella monthly spending cap to safeguard overall personal savings.
                </p>
              </div>
              <button
                onClick={() => {
                  setEditingBudget(null);
                  setIsModalOpen(true);
                }}
                className="btn-finmate-secondary"
                style={{ fontSize: '0.8125rem' }}
              >
                Set Overall Cap
              </button>
            </div>
          )}

          {/* Category Budgets Grid */}
          <div>
            <h3 style={{ fontSize: '1.125rem', fontWeight: 700, color: 'var(--text-primary)', marginBottom: '1rem' }}>
              Category Budgets ({categoryBudgets.length})
            </h3>

            {categoryBudgets.length > 0 ? (
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(310px, 1fr))', gap: '1.25rem' }}>
                {categoryBudgets.map(b => {
                  const percent = b.utilizationPercent || 0;
                  let color = 'var(--success-main)';
                  if (b.exceeded) color = 'var(--danger-main)';
                  else if (b.status === 'WARNING') color = 'var(--warning-main)';

                  return (
                    <div key={b.id} className="finmate-card" style={{ padding: '1.25rem', display: 'flex', flexDirection: 'column', gap: '0.875rem' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                          <span
                            style={{
                              width: 10,
                              height: 10,
                              borderRadius: '50%',
                              backgroundColor: b.category?.color || '#6366f1'
                            }}
                          />
                          <span style={{ fontWeight: 700, fontSize: '0.9375rem', color: 'var(--text-primary)' }}>
                            {b.category?.name}
                          </span>
                        </div>

                        <span className={`status-pill ${b.exceeded ? 'danger' : b.status === 'WARNING' ? 'warning' : 'success'}`}>
                          {b.exceeded ? 'Exceeded' : b.status === 'WARNING' ? 'Caution' : 'Safe'}
                        </span>
                      </div>

                      {/* Amounts */}
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
                        <div>
                          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Spent</div>
                          <div className="tabular-nums" style={{ fontSize: '1.125rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                            {formatCurrency(b.spentAmount)}
                          </div>
                        </div>
                        <div style={{ textAlign: 'right' }}>
                          <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Limit Cap</div>
                          <div className="tabular-nums" style={{ fontSize: '1.125rem', fontWeight: 700, color: 'var(--text-secondary)' }}>
                            {formatCurrency(b.limitAmount)}
                          </div>
                        </div>
                      </div>

                      {/* Progress bar */}
                      <div>
                        <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', color: 'var(--text-secondary)', marginBottom: '0.25rem' }}>
                          <span>{percent}% used</span>
                          <span>{b.exceeded ? `Overspent by ${formatCurrency(b.overspentAmount)}` : `${formatCurrency(b.remainingAmount)} left`}</span>
                        </div>
                        <div style={{ height: 8, backgroundColor: 'var(--bg-tertiary)', borderRadius: 4, overflow: 'hidden' }}>
                          <div
                            style={{
                              height: '100%',
                              width: `${Math.min(100, percent)}%`,
                              backgroundColor: color,
                              borderRadius: 4,
                              transition: 'width 0.3s'
                            }}
                          />
                        </div>
                      </div>

                      {/* Card Footer Actions */}
                      <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', paddingTop: '0.5rem', borderTop: '1px solid var(--border-color)' }}>
                        <button
                          onClick={() => {
                            setEditingBudget(b);
                            setIsModalOpen(true);
                          }}
                          style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer', padding: '0.25rem' }}
                          title="Edit Limit"
                        >
                          <Edit2 size={15} />
                        </button>
                        <button
                          onClick={() => handleDelete(b.id)}
                          style={{ background: 'none', border: 'none', color: 'var(--danger-main)', cursor: 'pointer', padding: '0.25rem' }}
                          title="Delete"
                        >
                          <Trash2 size={15} />
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            ) : (
              <EmptyState
                icon={PiggyBank}
                title="No category budgets configured"
                description={`You haven't set individual category limits for ${monthName}. Create one or clone from last month.`}
                action={
                  <button
                    onClick={() => {
                      setEditingBudget(null);
                      setIsModalOpen(true);
                    }}
                    className="btn-finmate-primary"
                  >
                    <Plus size={16} />
                    <span>Create Category Budget</span>
                  </button>
                }
              />
            )}
          </div>
        </>
      )}

      {/* Modals */}
      <BudgetModal
        isOpen={isModalOpen}
        onClose={() => {
          setIsModalOpen(false);
          setEditingBudget(null);
        }}
        budget={editingBudget}
        defaultYear={year}
        defaultMonth={month}
        onSuccess={fetchBudgets}
      />

      <CopyBudgetModal
        isOpen={isCopyModalOpen}
        onClose={() => setIsCopyModalOpen(false)}
        targetYear={year}
        targetMonth={month}
        onSuccess={fetchBudgets}
      />
    </div>
  );
}
