import React, { useState, useEffect } from 'react';
import { transactionApi } from '../../api/transactionApi';
import { categoryApi } from '../../api/categoryApi';
import { X, AlertCircle } from 'lucide-react';

export default function RecurringRuleModal({ isOpen, onClose, rule, onSuccess }) {
  const [type, setType] = useState('EXPENSE');
  const [amount, setAmount] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [frequency, setFrequency] = useState('MONTHLY');
  const [startDate, setStartDate] = useState(new Date().toISOString().split('T')[0]);
  const [endDate, setEndDate] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('UPI');
  const [description, setDescription] = useState('');
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (isOpen) {
      fetchCategories();
      if (rule) {
        setType(rule.type);
        setAmount(rule.amount);
        setCategoryId(rule.category?.id || '');
        setFrequency(rule.frequency);
        setStartDate(rule.startDate);
        setEndDate(rule.endDate || '');
        setPaymentMethod(rule.paymentMethod);
        setDescription(rule.description);
      } else {
        setType('EXPENSE');
        setAmount('');
        setCategoryId('');
        setFrequency('MONTHLY');
        setStartDate(new Date().toISOString().split('T')[0]);
        setEndDate('');
        setPaymentMethod('UPI');
        setDescription('');
      }
      setError('');
    }
  }, [isOpen, rule]);

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
      setError('Please enter a valid amount.');
      return;
    }

    if (!categoryId) {
      setError('Please select a category.');
      return;
    }

    setLoading(true);

    const payload = {
      type,
      amount: parsedAmount.toFixed(2),
      categoryId: parseInt(categoryId),
      frequency,
      startDate,
      endDate: endDate || null,
      paymentMethod,
      description: description.trim()
    };

    try {
      if (rule?.id) {
        await transactionApi.updateRecurringRule(rule.id, payload);
      } else {
        await transactionApi.createRecurringRule(payload);
      }
      onSuccess();
      onClose();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to save recurring rule');
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
          maxWidth: 500,
          maxHeight: '90vh',
          display: 'flex',
          flexDirection: 'column'
        }}
      >
        <div className="finmate-card-header">
          <h3 style={{ fontSize: '1.0625rem', fontWeight: 600, margin: 0 }}>
            {rule ? 'Edit Recurring Schedule' : 'Create Recurring Rule'}
          </h3>
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
            {/* Frequency & Type */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                  Type *
                </label>
                <select
                  className="finmate-select"
                  value={type}
                  onChange={e => {
                    setType(e.target.value);
                    setCategoryId('');
                  }}
                >
                  <option value="EXPENSE">Expense Rule</option>
                  <option value="INCOME">Income Rule</option>
                </select>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                  Cadence / Frequency *
                </label>
                <select
                  className="finmate-select"
                  value={frequency}
                  onChange={e => setFrequency(e.target.value)}
                >
                  <option value="DAILY">Daily</option>
                  <option value="WEEKLY">Weekly</option>
                  <option value="MONTHLY">Monthly</option>
                  <option value="YEARLY">Yearly</option>
                </select>
              </div>
            </div>

            {/* Amount & Description */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '0.75rem' }}>
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
                  Description *
                </label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Netflix Subscription, House Rent"
                  className="finmate-input"
                  value={description}
                  onChange={e => setDescription(e.target.value)}
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
                  {filteredCategories.map(c => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                  Default Payment Method *
                </label>
                <select
                  className="finmate-select"
                  value={paymentMethod}
                  onChange={e => setPaymentMethod(e.target.value)}
                >
                  <option value="UPI">UPI</option>
                  <option value="CREDIT_CARD">Credit Card</option>
                  <option value="DEBIT_CARD">Debit Card</option>
                  <option value="NET_BANKING">Net Banking</option>
                  <option value="CASH">Cash</option>
                </select>
              </div>
            </div>

            {/* Start Date & End Date */}
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                  Start Date *
                </label>
                <input
                  type="date"
                  required
                  className="finmate-input"
                  value={startDate}
                  onChange={e => setStartDate(e.target.value)}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                  End Date (Optional)
                </label>
                <input
                  type="date"
                  className="finmate-input"
                  value={endDate}
                  onChange={e => setEndDate(e.target.value)}
                />
              </div>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
              <button type="button" onClick={onClose} className="btn-finmate-secondary">
                Cancel
              </button>
              <button type="submit" disabled={loading} className="btn-finmate-primary">
                {loading ? 'Saving...' : rule ? 'Update Schedule' : 'Activate Schedule'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
