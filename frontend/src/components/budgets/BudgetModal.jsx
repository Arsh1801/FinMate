import React, { useState, useEffect } from 'react';
import { budgetApi } from '../../api/budgetApi';
import { categoryApi } from '../../api/categoryApi';
import { X, AlertCircle } from 'lucide-react';

export default function BudgetModal({ isOpen, onClose, budget, defaultYear, defaultMonth, onSuccess }) {
  const [budgetYear, setBudgetYear] = useState(defaultYear || new Date().getFullYear());
  const [budgetMonth, setBudgetMonth] = useState(defaultMonth || new Date().getMonth() + 1);
  const [categoryId, setCategoryId] = useState('');
  const [isOverall, setIsOverall] = useState(false);
  const [limitAmount, setLimitAmount] = useState('');
  const [alertThreshold, setAlertThreshold] = useState('80');
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isOpen) {
      fetchCategories();
      if (budget) {
        setBudgetYear(budget.budgetYear);
        setBudgetMonth(budget.budgetMonth);
        setCategoryId(budget.category?.id || '');
        setIsOverall(!budget.category);
        setLimitAmount(budget.limitAmount);
        setAlertThreshold(budget.alertThreshold ? budget.alertThreshold.toString() : '80');
      } else {
        setBudgetYear(defaultYear || new Date().getFullYear());
        setBudgetMonth(defaultMonth || new Date().getMonth() + 1);
        setCategoryId('');
        setIsOverall(false);
        setLimitAmount('');
        setAlertThreshold('80');
      }
      setError('');
    }
  }, [isOpen, budget, defaultYear, defaultMonth]);

  const fetchCategories = async () => {
    try {
      const resp = await categoryApi.getCategories('EXPENSE');
      setCategories(resp.data);
    } catch (err) {
      console.error('Failed to load expense categories', err);
    }
  };

  const handleSubmit = async e => {
    e.preventDefault();
    setError('');

    const parsedAmount = parseFloat(limitAmount);
    if (isNaN(parsedAmount) || parsedAmount <= 0) {
      setError('Please specify a positive budget limit amount.');
      return;
    }

    if (!isOverall && !categoryId) {
      setError('Please choose a category or mark as Overall Monthly Budget.');
      return;
    }

    setLoading(true);

    const payload = {
  year: parseInt(budgetYear),
  month: parseInt(budgetMonth),
  categoryId: isOverall ? null : parseInt(categoryId),
  limitAmount: parsedAmount.toFixed(2),
  alertThreshold: parseInt(alertThreshold) || 80
};

    try {
      if (budget?.id) {
        await budgetApi.updateBudget(budget.id, {
          limitAmount: payload.limitAmount,
          alertThreshold: payload.alertThreshold
        });
      } else {
        await budgetApi.createBudget(payload);
      }
      onSuccess();
      onClose();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save budget plan');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div
      style={{
        position: 'fixed',
        inset: 0,
        backgroundColor: 'rgba(0,0,0,0.5)',
        zIndex: 1000,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '1rem'
      }}
    >
      <div
        className="finmate-card"
        style={{ width: '100%', maxWidth: 460, display: 'flex', flexDirection: 'column' }}
      >
        <div className="finmate-card-header">
          <h3 style={{ fontSize: '1.0625rem', fontWeight: 600, margin: 0 }}>
            {budget ? 'Edit Budget Cap' : 'Configure New Budget'}
          </h3>
          <button onClick={onClose} style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}>
            <X size={20} />
          </button>
        </div>

        <div style={{ padding: '1.5rem' }}>
          {error && (
            <div
              style={{
                backgroundColor: 'var(--danger-bg)',
                border: '1px solid var(--danger-border)',
                color: 'var(--danger-text)',
                padding: '0.75rem 1rem',
                borderRadius: 'var(--radius-md)',
                fontSize: '0.875rem',
                marginBottom: '1rem',
                display: 'flex',
                alignItems: 'center',
                gap: '0.5rem'
              }}
            >
              <AlertCircle size={18} />
              <span>{error}</span>
            </div>
          )}

          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {/* Year & Month (disabled on edit) */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                  Target Year
                </label>
                <input
                  type="number"
                  disabled={!!budget}
                  className="finmate-input"
                  value={budgetYear}
                  onChange={e => setBudgetYear(e.target.value)}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                  Target Month
                </label>
                <select
                  disabled={!!budget}
                  className="finmate-select"
                  value={budgetMonth}
                  onChange={e => setBudgetMonth(e.target.value)}
                >
                  {Array.from({ length: 12 }, (_, i) => i + 1).map(m => (
                    <option key={m} value={m}>
                      {new Date(2024, m - 1).toLocaleString('default', { month: 'long' })}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Scope / Category */}
            {!budget && (
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
                  <input
                    type="checkbox"
                    id="isOverall"
                    checked={isOverall}
                    onChange={e => setIsOverall(e.target.checked)}
                  />
                  <label htmlFor="isOverall" style={{ fontSize: '0.8125rem', fontWeight: 600, cursor: 'pointer' }}>
                    Set as Overall Total Monthly Budget Cap
                  </label>
                </div>

                {!isOverall && (
                  <div>
                    <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                      Expense Category *
                    </label>
                    <select
                      required={!isOverall}
                      className="finmate-select"
                      value={categoryId}
                      onChange={e => setCategoryId(e.target.value)}
                    >
                      <option value="">Select Category</option>
                      {categories.map(c => (
                        <option key={c.id} value={c.id}>
                          {c.name}
                        </option>
                      ))}
                    </select>
                  </div>
                )}
              </div>
            )}

            {/* Budget Limit Amount */}
            <div>
              <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                Budget Cap Limit (₹) *
              </label>
              <input
                type="number"
                step="0.01"
                min="0.01"
                required
                placeholder="6000.00"
                className="finmate-input tabular-nums"
                value={limitAmount}
                onChange={e => setLimitAmount(e.target.value)}
              />
            </div>

            {/* Alert Threshold % */}
            <div>
              <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                Alert Warning Threshold (% of limit)
              </label>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                <input
                  type="range"
                  min="50"
                  max="95"
                  step="5"
                  value={alertThreshold}
                  onChange={e => setAlertThreshold(e.target.value)}
                  style={{ flex: 1 }}
                />
                <span className="tabular-nums" style={{ fontSize: '0.875rem', fontWeight: 700, width: 45 }}>
                  {alertThreshold}%
                </span>
              </div>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                You will be notified as soon as spending crosses this benchmark.
              </span>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
              <button type="button" onClick={onClose} className="btn-finmate-secondary">
                Cancel
              </button>
              <button type="submit" disabled={loading} className="btn-finmate-primary">
                {loading ? 'Saving...' : budget ? 'Update Cap' : 'Establish Budget'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
