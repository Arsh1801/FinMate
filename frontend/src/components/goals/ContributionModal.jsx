import React, { useState, useEffect } from 'react';
import { goalApi } from '../../api/goalApi';
import { formatCurrency } from '../../utils/formatters';
import { X, AlertCircle, PlusCircle, ShieldCheck } from 'lucide-react';

export default function ContributionModal({ isOpen, onClose, goal, onSuccess }) {
  const [amount, setAmount] = useState('');
  const [contributionDate, setContributionDate] = useState(new Date().toISOString().split('T')[0]);
  const [note, setNote] = useState('');
  const [freeBalance, setFreeBalance] = useState('0.00');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isOpen) {
      setAmount('');
      setContributionDate(new Date().toISOString().split('T')[0]);
      setNote('');
      setError('');
      fetchFreeBalance();
    }
  }, [isOpen]);

  const fetchFreeBalance = async () => {
    try {
      const resp = await goalApi.getFreeBalance();
      setFreeBalance(resp.data?.freeBalance || '0.00');
    } catch (err) {
      console.error('Failed to get free balance', err);
    }
  };

  const handleSubmit = async e => {
    e.preventDefault();
    setError('');

    const parsedAmount = parseFloat(amount);
    if (isNaN(parsedAmount) || parsedAmount <= 0) {
      setError('Please enter a valid positive contribution amount.');
      return;
    }

    const freeNum = parseFloat(freeBalance);
    if (parsedAmount > freeNum) {
      setError(`Contribution exceeds available free balance of ${formatCurrency(freeBalance)}. Please reduce amount or top-up income.`);
      return;
    }

    setLoading(true);

    try {
      await goalApi.addContribution(goal.id, {
        amount: parsedAmount.toFixed(2),
        contributionDate,
        note: note.trim() || null
      });
      onSuccess();
      onClose();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to deposit contribution');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen || !goal) return null;

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
      <div className="finmate-card" style={{ width: '100%', maxWidth: 440 }}>
        <div className="finmate-card-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <PlusCircle size={18} style={{ color: 'var(--primary-600)' }} />
            <h3 style={{ fontSize: '1.0625rem', fontWeight: 600, margin: 0 }}>
              Deposit to: {goal.name}
            </h3>
          </div>
          <button onClick={onClose} style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}>
            <X size={20} />
          </button>
        </div>

        <div style={{ padding: '1.5rem' }}>
          {/* Free balance indicator */}
          <div
            style={{
              padding: '0.75rem 1rem',
              backgroundColor: 'var(--bg-tertiary)',
              borderRadius: 'var(--radius-md)',
              marginBottom: '1.25rem',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              fontSize: '0.8125rem'
            }}
          >
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.4rem', color: 'var(--text-secondary)' }}>
              <ShieldCheck size={16} style={{ color: 'var(--success-main)' }} />
              <span>Available Free Balance:</span>
            </div>
            <strong className="tabular-nums" style={{ color: 'var(--text-primary)', fontSize: '0.9375rem' }}>
              {formatCurrency(freeBalance)}
            </strong>
          </div>

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
            <div>
              <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                Deposit Amount (₹) *
              </label>
              <input
                type="number"
                step="0.01"
                min="0.01"
                required
                placeholder="5000.00"
                className="finmate-input tabular-nums"
                value={amount}
                onChange={e => setAmount(e.target.value)}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                Contribution Date *
              </label>
              <input
                type="date"
                required
                max={new Date().toISOString().split('T')[0]}
                className="finmate-input"
                value={contributionDate}
                onChange={e => setContributionDate(e.target.value)}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                Note (Optional)
              </label>
              <input
                type="text"
                placeholder="e.g. Monthly savings cut, bonus portion"
                className="finmate-input"
                value={note}
                onChange={e => setNote(e.target.value)}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
              <button type="button" onClick={onClose} className="btn-finmate-secondary">
                Cancel
              </button>
              <button type="submit" disabled={loading} className="btn-finmate-primary">
                {loading ? 'Depositing...' : 'Confirm Contribution'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
