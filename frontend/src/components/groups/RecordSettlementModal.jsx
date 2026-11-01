import React, { useState, useEffect } from 'react';
import { settlementApi } from '../../api/settlementApi';
import { X, AlertCircle, ArrowRightLeft } from 'lucide-react';

export default function RecordSettlementModal({ isOpen, onClose, groupId, members, suggestion, onSuccess }) {
  const [fromUserId, setFromUserId] = useState('');
  const [toUserId, setToUserId] = useState('');
  const [amount, setAmount] = useState('');
  const [paymentMode, setPaymentMode] = useState('UPI');
  const [referenceNote, setReferenceNote] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isOpen) {
      if (suggestion) {
        setFromUserId(suggestion.fromUserId);
        setToUserId(suggestion.toUserId);
        setAmount(suggestion.amount);
      } else {
        setFromUserId(members[0]?.userId || '');
        setToUserId(members[1]?.userId || '');
        setAmount('');
      }
      setPaymentMode('UPI');
      setReferenceNote('');
      setError('');
    }
  }, [isOpen, suggestion, members]);

  const handleSubmit = async e => {
    e.preventDefault();
    setError('');

    const parsedAmount = parseFloat(amount);
    if (isNaN(parsedAmount) || parsedAmount <= 0) {
      setError('Please specify a positive settlement transfer amount.');
      return;
    }

    if (fromUserId === toUserId) {
      setError('Debtor and creditor cannot be the same user.');
      return;
    }

    setLoading(true);

    try {
      const today = new Date();
const settlementDate = `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`;

await settlementApi.recordSettlement(groupId, {
  fromUserId: parseInt(fromUserId),
  toUserId: parseInt(toUserId),
  amount: parsedAmount.toFixed(2),
  settlementDate,
  note: referenceNote.trim() || null
});
      onSuccess();
      onClose();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to record debt settlement');
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
      <div className="finmate-card" style={{ width: '100%', maxWidth: 460 }}>
        <div className="finmate-card-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <ArrowRightLeft size={18} style={{ color: 'var(--primary-600)' }} />
            <h3 style={{ fontSize: '1.0625rem', fontWeight: 600, margin: 0 }}>
              Record Debt Settlement
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

          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {/* Payer and Receiver */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                  Payer (Who Paid) *
                </label>
                <select
                  required
                  className="finmate-select"
                  value={fromUserId}
                  onChange={e => setFromUserId(e.target.value)}
                >
                  {members.map(m => (
                    <option key={m.userId} value={m.userId}>
                      {m.fullName}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                  Receiver (Who Got Paid) *
                </label>
                <select
                  required
                  className="finmate-select"
                  value={toUserId}
                  onChange={e => setToUserId(e.target.value)}
                >
                  {members.map(m => (
                    <option key={m.userId} value={m.userId}>
                      {m.fullName}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {/* Amount & Mode */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                  Transfer Amount (₹) *
                </label>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  required
                  placeholder="2333.33"
                  className="finmate-input tabular-nums"
                  value={amount}
                  onChange={e => setAmount(e.target.value)}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                  Payment Mode *
                </label>
                <select
                  className="finmate-select"
                  value={paymentMode}
                  onChange={e => setPaymentMode(e.target.value)}
                >
                  <option value="UPI">UPI / Google Pay</option>
                  <option value="BANK_TRANSFER">Bank IMPS / NEFT</option>
                  <option value="CASH">Cash in Hand</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>
            </div>

            {/* Reference Note */}
            <div>
              <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                Transaction Ref / Note
              </label>
              <input
                type="text"
                placeholder="e.g. UPI Ref #4092837493"
                className="finmate-input"
                value={referenceNote}
                onChange={e => setReferenceNote(e.target.value)}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
              <button type="button" onClick={onClose} className="btn-finmate-secondary">
                Cancel
              </button>
              <button type="submit" disabled={loading} className="btn-finmate-primary">
                {loading ? 'Submitting...' : 'Record Settlement'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
