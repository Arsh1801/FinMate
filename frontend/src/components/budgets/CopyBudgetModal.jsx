import React, { useState } from 'react';
import { budgetApi } from '../../api/budgetApi';
import { X, AlertCircle, Copy } from 'lucide-react';

export default function CopyBudgetModal({ isOpen, onClose, targetYear, targetMonth, onSuccess }) {
  const [sourceYear, setSourceYear] = useState(
    targetMonth === 1 ? targetYear - 1 : targetYear
  );
  const [sourceMonth, setSourceMonth] = useState(
    targetMonth === 1 ? 12 : targetMonth - 1
  );
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  if (!isOpen) return null;

  const handleCopy = async e => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      await budgetApi.copyPriorMonthBudgets(
        parseInt(sourceYear),
        parseInt(sourceMonth),
        parseInt(targetYear),
        parseInt(targetMonth)
      );
      onSuccess();
      onClose();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to clone budgets from source month');
    } finally {
      setLoading(false);
    }
  };

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
      <div className="finmate-card" style={{ width: '100%', maxWidth: 420 }}>
        <div className="finmate-card-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Copy size={18} style={{ color: 'var(--primary-600)' }} />
            <h3 style={{ fontSize: '1.0625rem', fontWeight: 600, margin: 0 }}>
              Copy Prior Month Budgets
            </h3>
          </div>
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

          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '1.25rem' }}>
            Replicate category limits into <strong>{new Date(targetYear, targetMonth - 1).toLocaleString('default', { month: 'long', year: 'numeric' })}</strong> without manual reconfiguration.
          </p>

          <form onSubmit={handleCopy} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                  Source Year
                </label>
                <input
                  type="number"
                  required
                  className="finmate-input"
                  value={sourceYear}
                  onChange={e => setSourceYear(e.target.value)}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                  Source Month
                </label>
                <select
                  className="finmate-select"
                  value={sourceMonth}
                  onChange={e => setSourceMonth(e.target.value)}
                >
                  {Array.from({ length: 12 }, (_, i) => i + 1).map(m => (
                    <option key={m} value={m}>
                      {new Date(2024, m - 1).toLocaleString('default', { month: 'long' })}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
              <button type="button" onClick={onClose} className="btn-finmate-secondary">
                Cancel
              </button>
              <button type="submit" disabled={loading} className="btn-finmate-primary">
                {loading ? 'Copying...' : 'Clone All Budgets'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
