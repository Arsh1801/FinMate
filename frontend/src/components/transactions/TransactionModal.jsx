import React, { useState, useEffect } from 'react';
import { transactionApi } from '../../api/transactionApi';
import { categoryApi } from '../../api/categoryApi';
import { useNotification } from '../../context/NotificationContext';
import { X, AlertCircle } from 'lucide-react';

export default function TransactionModal({ isOpen, onClose, transaction, onSuccess }) {
  const { refreshUnreadCount } = useNotification();
  const [type, setType] = useState('EXPENSE');
  const [amount, setAmount] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [transactionDate, setTransactionDate] = useState(new Date().toISOString().split('T')[0]);
  const [paymentMethod, setPaymentMethod] = useState('UPI');
  const [description, setDescription] = useState('');
  const [notes, setNotes] = useState('');
  const [tags, setTags] = useState('');
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isOpen) {
      fetchCategories();
      if (transaction) {
        setType(transaction.type);
        setAmount(transaction.amount);
        setCategoryId(transaction.category?.id || '');
        setTransactionDate(transaction.transactionDate);
        setPaymentMethod(transaction.paymentMethod);
        setDescription(transaction.description);
        setNotes(transaction.notes || '');
        setTags(transaction.tags ? transaction.tags.join(', ') : '');
      } else {
        // Reset form
        setType('EXPENSE');
        setAmount('');
        setCategoryId('');
        setTransactionDate(new Date().toISOString().split('T')[0]);
        setPaymentMethod('UPI');
        setDescription('');
        setNotes('');
        setTags('');
      }
      setError('');
    }
  }, [isOpen, transaction]);

  const fetchCategories = async () => {
    try {
      const resp = await categoryApi.getCategories();
      setCategories(resp.data);
    } catch (err) {
      console.error('Failed to load categories', err);
    }
  };

  const filteredCategories = categories.filter(c => c.type === type);

  const handleSubmit = async e => {
    e.preventDefault();
    setError('');

    const parsedAmount = parseFloat(amount);
    if (isNaN(parsedAmount) || parsedAmount <= 0) {
      setError('Please enter a valid positive amount.');
      return;
    }

    if (!categoryId) {
      setError('Please select a category.');
      return;
    }

    setLoading(true);

    const tagList = tags
      .split(',')
      .map(t => t.trim())
      .filter(Boolean);

    const payload = {
      type,
      amount: parsedAmount.toFixed(2),
      categoryId: parseInt(categoryId),
      transactionDate,
      paymentMethod,
      description: description.trim(),
      notes: notes.trim() || null,
      tags: tagList
    };

    try {
      if (transaction?.id) {
  await transactionApi.updateTransaction(transaction.id, {
    ...payload,
    version: transaction.version
  });
} else {
  await transactionApi.createTransaction(payload);
}
      onSuccess();
      refreshUnreadCount?.();
      onClose();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save transaction');
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
        backgroundColor: 'rgba(0, 0, 0, 0.5)',
        zIndex: 1000,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '1rem'
      }}
    >
      <div
        className="finmate-card"
        style={{
          width: '100%',
          maxWidth: 520,
          maxHeight: '90vh',
          display: 'flex',
          flexDirection: 'column',
          boxShadow: 'var(--shadow-lg)'
        }}
      >
        {/* Modal Header */}
        <div className="finmate-card-header">
          <h3 style={{ fontSize: '1.0625rem', fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>
            {transaction ? 'Edit Transaction' : 'Record New Transaction'}
          </h3>
          <button
            onClick={onClose}
            style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}
          >
            <X size={20} />
          </button>
        </div>

        {/* Modal Body */}
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
            {/* Type Toggle */}
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: '1fr 1fr',
                backgroundColor: 'var(--bg-tertiary)',
                borderRadius: 'var(--radius-md)',
                padding: '0.25rem',
                gap: '0.25rem'
              }}
            >
              <button
                type="button"
                onClick={() => {
                  setType('EXPENSE');
                  setCategoryId('');
                }}
                style={{
                  padding: '0.5rem',
                  borderRadius: 'var(--radius-sm)',
                  border: 'none',
                  fontSize: '0.875rem',
                  fontWeight: 600,
                  cursor: 'pointer',
                  backgroundColor: type === 'EXPENSE' ? 'var(--danger-main)' : 'transparent',
                  color: type === 'EXPENSE' ? '#ffffff' : 'var(--text-secondary)',
                  transition: 'all 0.15s ease'
                }}
              >
                Expense
              </button>
              <button
                type="button"
                onClick={() => {
                  setType('INCOME');
                  setCategoryId('');
                }}
                style={{
                  padding: '0.5rem',
                  borderRadius: 'var(--radius-sm)',
                  border: 'none',
                  fontSize: '0.875rem',
                  fontWeight: 600,
                  cursor: 'pointer',
                  backgroundColor: type === 'INCOME' ? 'var(--success-main)' : 'transparent',
                  color: type === 'INCOME' ? '#ffffff' : 'var(--text-secondary)',
                  transition: 'all 0.15s ease'
                }}
              >
                Income
              </button>
            </div>

            {/* Amount & Date */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                  Amount (₹) *
                </label>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  required
                  placeholder="0.00"
                  className="finmate-input tabular-nums"
                  value={amount}
                  onChange={e => setAmount(e.target.value)}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                  Date *
                </label>
                <input
                  type="date"
                  required
                  max={new Date().toISOString().split('T')[0]}
                  className="finmate-input"
                  value={transactionDate}
                  onChange={e => setTransactionDate(e.target.value)}
                />
              </div>
            </div>

            {/* Category & Payment Method */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                  Category *
                </label>
                <select
                  required
                  className="finmate-select"
                  value={categoryId}
                  onChange={e => setCategoryId(e.target.value)}
                >
                  <option value="">Select Category</option>
                  {filteredCategories.map(cat => (
                    <option key={cat.id} value={cat.id}>
                      {cat.name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                  Payment Method *
                </label>
                <select
                  required
                  className="finmate-select"
                  value={paymentMethod}
                  onChange={e => setPaymentMethod(e.target.value)}
                >
                  <option value="UPI">UPI</option>
                  <option value="CREDIT_CARD">Credit Card</option>
                  <option value="DEBIT_CARD">Debit Card</option>
                  <option value="NET_BANKING">Net Banking</option>
                  <option value="CASH">Cash</option>
                  <option value="WALLET">Wallet</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>
            </div>

            {/* Description */}
            <div>
              <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                Description *
              </label>
              <input
                type="text"
                required
                placeholder="e.g. Weekly grocery basket, dinner at Toit"
                className="finmate-input"
                value={description}
                onChange={e => setDescription(e.target.value)}
              />
            </div>

            {/* Tags */}
            <div>
              <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                Tags (comma separated)
              </label>
              <input
                type="text"
                placeholder="e.g. travel, groceries, urgent"
                className="finmate-input"
                value={tags}
                onChange={e => setTags(e.target.value)}
              />
            </div>

            {/* Notes */}
            <div>
              <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                Optional Notes
              </label>
              <textarea
                rows={2}
                placeholder="Add any extra transaction context..."
                className="finmate-textarea"
                value={notes}
                onChange={e => setNotes(e.target.value)}
              />
            </div>

            {/* Actions */}
            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
              <button
                type="button"
                onClick={onClose}
                className="btn-finmate-secondary"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={loading}
                className="btn-finmate-primary"
              >
                {loading ? 'Saving...' : transaction ? 'Save Changes' : 'Record Transaction'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
