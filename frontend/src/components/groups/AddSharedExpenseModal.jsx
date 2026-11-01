import React, { useState, useEffect } from 'react';
import { groupApi } from '../../api/groupApi';
import { categoryApi } from '../../api/categoryApi';
import { useNotification } from '../../context/NotificationContext';
import { formatCurrency } from '../../utils/formatters';
import { X, AlertCircle, Receipt, Check } from 'lucide-react';

export default function AddSharedExpenseModal({ isOpen, onClose, groupId, members, onSuccess }) {
  const { refreshUnreadCount } = useNotification();
  const [description, setDescription] = useState('');
  const [amount, setAmount] = useState('');
  const [expenseDate, setExpenseDate] = useState(new Date().toISOString().split('T')[0]);
  const [paidByUserId, setPaidByUserId] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [notes, setNotes] = useState('');
  const [splitType, setSplitType] = useState('EQUAL');

  // Split state
  const [selectedUserIds, setSelectedUserIds] = useState([]);
  const [exactAmounts, setExactAmounts] = useState({});
  const [percentages, setPercentages] = useState({});

  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isOpen) {
      setDescription('');
      setAmount('');
      setExpenseDate(new Date().toISOString().split('T')[0]);
      setPaidByUserId(members[0]?.userId || '');
      setCategoryId('');
      setNotes('');
      setSplitType('EQUAL');

      // Initialize all members selected for equal split
      const allIds = members.map(m => m.userId);
      setSelectedUserIds(allIds);

      // Initialize empty exact and percentages
      const emptyExact = {};
      const emptyPct = {};
      members.forEach(m => {
        emptyExact[m.userId] = '';
        emptyPct[m.userId] = '';
      });
      setExactAmounts(emptyExact);
      setPercentages(emptyPct);

      setError('');
      fetchCategories();
    }
  }, [isOpen, members]);

  const fetchCategories = async () => {
    try {
      const resp = await categoryApi.getCategories('EXPENSE');
      setCategories(resp.data);
    } catch (err) {
      console.error('Failed to load categories', err);
    }
  };

  const toggleUserSelection = uId => {
    setSelectedUserIds(prev =>
      prev.includes(uId) ? prev.filter(id => id !== uId) : [...prev, uId]
    );
  };

  // Calculations for live indicators
  const totalNum = parseFloat(amount) || 0;

  // Running sum for Exact
  const exactSum = Object.values(exactAmounts).reduce((acc, v) => acc + (parseFloat(v) || 0), 0);
  const exactDiff = Math.abs(totalNum - exactSum);

  // Running sum for Percentage
  const pctSum = Object.values(percentages).reduce((acc, v) => acc + (parseFloat(v) || 0), 0);
  const pctDiff = Math.abs(100 - pctSum);

  const handleSubmit = async e => {
    e.preventDefault();
    setError('');

    if (totalNum <= 0) {
      setError('Please specify a positive total expense amount.');
      return;
    }

    if (!paidByUserId) {
      setError('Please select who paid for this expense.');
      return;
    }

    let splitsPayload = [];

    if (splitType === 'EQUAL') {
      if (selectedUserIds.length === 0) {
        setError('At least one participant must be included in an equal split.');
        return;
      }
      splitsPayload = selectedUserIds.map(uId => ({ userId: uId }));
    } else if (splitType === 'EXACT') {
      if (exactDiff > 0.005) {
        setError(`Exact shares sum to ₹${exactSum.toFixed(2)}, which does not equal total amount ₹${totalNum.toFixed(2)}.`);
        return;
      }
      splitsPayload = Object.entries(exactAmounts)
        .filter(([_, val]) => parseFloat(val) > 0)
        .map(([uId, val]) => ({
          userId: parseInt(uId),
          amount: parseFloat(val).toFixed(2)
        }));
    } else if (splitType === 'PERCENTAGE') {
      if (pctDiff > 0.05) {
        setError(`Percentage shares sum to ${pctSum.toFixed(2)}%, which must equal exactly 100.00%.`);
        return;
      }
      splitsPayload = Object.entries(percentages)
        .filter(([_, val]) => parseFloat(val) > 0)
        .map(([uId, val]) => ({
          userId: parseInt(uId),
          percentage: parseFloat(val).toFixed(2)
        }));
    }

    setLoading(true);

    const payload = {
      description: description.trim(),
      amount: totalNum.toFixed(2),
      expenseDate,
      paidBy: parseInt(paidByUserId),
      categoryId: categoryId ? parseInt(categoryId) : null,
      notes: notes.trim() || null,
      splitType,
      splits: splitsPayload
    };

    try {
      await groupApi.createSharedExpense(groupId, payload);
      onSuccess();
      refreshUnreadCount?.();
      onClose();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to record shared group expense');
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
        style={{ width: '100%', maxWidth: 560, maxHeight: '92vh', display: 'flex', flexDirection: 'column' }}
      >
        <div className="finmate-card-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Receipt size={18} style={{ color: 'var(--primary-600)' }} />
            <h3 style={{ fontSize: '1.0625rem', fontWeight: 600, margin: 0 }}>
              Add Shared Group Expense
            </h3>
          </div>
          <button onClick={onClose} style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}>
            <X size={20} />
          </button>
        </div>

        <div style={{ padding: '1.5rem', overflowY: 'auto' }}>
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
            {/* Description & Amount */}
            <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '0.75rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                  Expense Description *
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Dinner at Fisherman's Wharf, Cab to Airport"
                  className="finmate-input"
                  value={description}
                  onChange={e => setDescription(e.target.value)}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                  Total Amount (₹) *
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
            </div>

            {/* Paid By & Date */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                  Paid By *
                </label>
                <select
                  required
                  className="finmate-select"
                  value={paidByUserId}
                  onChange={e => setPaidByUserId(e.target.value)}
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
                  Expense Date *
                </label>
                <input
                  type="date"
                  required
                  className="finmate-input"
                  value={expenseDate}
                  onChange={e => setExpenseDate(e.target.value)}
                />
              </div>
            </div>

            {/* Category */}
            <div>
              <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                Category (Optional)
              </label>
              <select
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

            {/* Split Strategy Selector */}
            <div>
              <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                Split Method *
              </label>
              <div
                style={{
                  display: 'grid',
                  gridTemplateColumns: '1fr 1fr 1fr',
                  backgroundColor: 'var(--bg-tertiary)',
                  borderRadius: 'var(--radius-md)',
                  padding: '0.25rem',
                  gap: '0.25rem'
                }}
              >
                {['EQUAL', 'EXACT', 'PERCENTAGE'].map(t => (
                  <button
                    key={t}
                    type="button"
                    onClick={() => setSplitType(t)}
                    style={{
                      padding: '0.45rem',
                      borderRadius: 'var(--radius-sm)',
                      border: 'none',
                      fontSize: '0.75rem',
                      fontWeight: 600,
                      cursor: 'pointer',
                      backgroundColor: splitType === t ? 'var(--primary-600)' : 'transparent',
                      color: splitType === t ? '#ffffff' : 'var(--text-secondary)',
                      transition: 'all 0.15s ease'
                    }}
                  >
                    {t === 'EQUAL' ? 'Split Equally' : t === 'EXACT' ? 'Exact Amounts' : 'By Percentage'}
                  </button>
                ))}
              </div>
            </div>

            {/* Split Participants Inputs based on Split Strategy */}
            <div
              style={{
                backgroundColor: 'var(--bg-tertiary)',
                borderRadius: 'var(--radius-md)',
                padding: '1rem',
                border: '1px solid var(--border-color)'
              }}
            >
              {/* EQUAL SPLIT */}
              {splitType === 'EQUAL' && (
                <div>
                  <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginBottom: '0.5rem' }}>
                    Select who participates in this expense (remainder paise allocated deterministically):
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                    {members.map(m => {
                      const isChecked = selectedUserIds.includes(m.userId);
                      const approxShare = selectedUserIds.length > 0 && isChecked
                        ? (totalNum / selectedUserIds.length).toFixed(2)
                        : '0.00';

                      return (
                        <label
                          key={m.userId}
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            padding: '0.5rem 0.75rem',
                            backgroundColor: 'var(--bg-card)',
                            borderRadius: 'var(--radius-sm)',
                            cursor: 'pointer',
                            fontSize: '0.8125rem'
                          }}
                        >
                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                            <input
                              type="checkbox"
                              checked={isChecked}
                              onChange={() => toggleUserSelection(m.userId)}
                            />
                            <span style={{ fontWeight: 600 }}>{m.fullName}</span>
                          </div>
                          {isChecked && (
                            <span className="tabular-nums" style={{ color: 'var(--text-secondary)' }}>
                              ~{formatCurrency(approxShare)}
                            </span>
                          )}
                        </label>
                      );
                    })}
                  </div>
                </div>
              )}

              {/* EXACT SPLIT */}
              {splitType === 'EXACT' && (
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', marginBottom: '0.5rem' }}>
                    <span>Specify exact rupee amount per participant:</span>
                    <span className="tabular-nums" style={{ fontWeight: 700, color: exactDiff <= 0.005 ? 'var(--success-main)' : 'var(--danger-main)' }}>
                      Total: ₹{exactSum.toFixed(2)} / ₹{totalNum.toFixed(2)}
                    </span>
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                    {members.map(m => (
                      <div
                        key={m.userId}
                        style={{
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'space-between',
                          padding: '0.4rem 0.75rem',
                          backgroundColor: 'var(--bg-card)',
                          borderRadius: 'var(--radius-sm)',
                          fontSize: '0.8125rem'
                        }}
                      >
                        <span style={{ fontWeight: 600 }}>{m.fullName}</span>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                          <span>₹</span>
                          <input
                            type="number"
                            step="0.01"
                            min="0"
                            placeholder="0.00"
                            className="finmate-input tabular-nums"
                            style={{ width: 100, padding: '0.25rem 0.5rem' }}
                            value={exactAmounts[m.userId] || ''}
                            onChange={e => {
                              const val = e.target.value;
                              setExactAmounts(prev => ({ ...prev, [m.userId]: val }));
                            }}
                          />
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* PERCENTAGE SPLIT */}
              {splitType === 'PERCENTAGE' && (
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', marginBottom: '0.5rem' }}>
                    <span>Specify percentage split per participant:</span>
                    <span className="tabular-nums" style={{ fontWeight: 700, color: pctDiff <= 0.05 ? 'var(--success-main)' : 'var(--danger-main)' }}>
                      Sum: {pctSum.toFixed(2)}% / 100.00%
                    </span>
                  </div>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                    {members.map(m => {
                      const userPct = parseFloat(percentages[m.userId]) || 0;
                      const calculatedRupee = ((totalNum * userPct) / 100).toFixed(2);

                      return (
                        <div
                          key={m.userId}
                          style={{
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'space-between',
                            padding: '0.4rem 0.75rem',
                            backgroundColor: 'var(--bg-card)',
                            borderRadius: 'var(--radius-sm)',
                            fontSize: '0.8125rem'
                          }}
                        >
                          <span style={{ fontWeight: 600 }}>{m.fullName}</span>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                            <span className="tabular-nums" style={{ color: 'var(--text-muted)', fontSize: '0.75rem' }}>
                              (~₹{calculatedRupee})
                            </span>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
                              <input
                                type="number"
                                step="0.01"
                                min="0"
                                max="100"
                                placeholder="0"
                                className="finmate-input tabular-nums"
                                style={{ width: 75, padding: '0.25rem 0.5rem' }}
                                value={percentages[m.userId] || ''}
                                onChange={e => {
                                  const val = e.target.value;
                                  setPercentages(prev => ({ ...prev, [m.userId]: val }));
                                }}
                              />
                              <span>%</span>
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              )}
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
              <button type="button" onClick={onClose} className="btn-finmate-secondary">
                Cancel
              </button>
              <button
                type="submit"
                disabled={loading}
                className="btn-finmate-primary"
              >
                {loading ? 'Recording...' : 'Record Shared Expense'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
