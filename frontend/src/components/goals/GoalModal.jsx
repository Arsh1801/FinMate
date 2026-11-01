import React, { useState, useEffect } from 'react';
import { goalApi } from '../../api/goalApi';
import { X, AlertCircle } from 'lucide-react';

export default function GoalModal({ isOpen, onClose, goal, onSuccess }) {
  const [name, setName] = useState('');
  const [targetAmount, setTargetAmount] = useState('');
  const [targetDate, setTargetDate] = useState('');
  const [note, setNote] = useState('');
  const [icon, setIcon] = useState('Target');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isOpen) {
      if (goal) {
        setName(goal.name);
        setTargetAmount(goal.targetAmount);
        setTargetDate(goal.targetDate);
        setNote(goal.note || '');
        setIcon(goal.icon || 'Target');
      } else {
        setName('');
        setTargetAmount('');
        // Default target date: 6 months from now
        const d = new Date();
        d.setMonth(d.getMonth() + 6);
        setTargetDate(d.toISOString().split('T')[0]);
        setNote('');
        setIcon('Target');
      }
      setError('');
    }
  }, [isOpen, goal]);

  const handleSubmit = async e => {
    e.preventDefault();
    setError('');

    const parsedTarget = parseFloat(targetAmount);
    if (isNaN(parsedTarget) || parsedTarget <= 0) {
      setError('Please specify a positive target amount.');
      return;
    }

    setLoading(true);

    const payload = {
      name: name.trim(),
      targetAmount: parsedTarget.toFixed(2),
      targetDate,
      note: note.trim() || null,
      icon
    };

    try {
      if (goal?.id) {
        await goalApi.updateGoal(goal.id, payload);
      } else {
        await goalApi.createGoal(payload);
      }
      onSuccess();
      onClose();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save savings goal');
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
      <div className="finmate-card" style={{ width: '100%', maxWidth: 480 }}>
        <div className="finmate-card-header">
          <h3 style={{ fontSize: '1.0625rem', fontWeight: 600, margin: 0 }}>
            {goal ? 'Edit Savings Target' : 'Create New Savings Goal'}
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
            <div>
              <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                Goal Name *
              </label>
              <input
                type="text"
                required
                placeholder="e.g. New MacBook Pro M3, Emergency Fund"
                className="finmate-input"
                value={name}
                onChange={e => setName(e.target.value)}
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                  Target Amount (₹) *
                </label>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  required
                  placeholder="60000.00"
                  className="finmate-input tabular-nums"
                  value={targetAmount}
                  onChange={e => setTargetAmount(e.target.value)}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                  Target Completion Date *
                </label>
                <input
                  type="date"
                  required
                  min={new Date().toISOString().split('T')[0]}
                  className="finmate-input"
                  value={targetDate}
                  onChange={e => setTargetDate(e.target.value)}
                />
              </div>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                Purpose / Description Notes
              </label>
              <textarea
                rows={2}
                placeholder="Why are you saving for this?"
                className="finmate-textarea"
                value={note}
                onChange={e => setNote(e.target.value)}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
              <button type="button" onClick={onClose} className="btn-finmate-secondary">
                Cancel
              </button>
              <button type="submit" disabled={loading} className="btn-finmate-primary">
                {loading ? 'Saving...' : goal ? 'Update Goal' : 'Create Goal'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
