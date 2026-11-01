import React, { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import { transactionApi } from '../../api/transactionApi';
import { categoryApi } from '../../api/categoryApi';
import { formatCurrency, formatDate } from '../../utils/formatters';
import TransactionModal from '../../components/transactions/TransactionModal';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import EmptyState from '../../components/common/EmptyState';
import {
  Plus,
  Download,
  Search,
  Filter,
  Trash2,
  Edit2,
  Receipt,
  ArrowUpRight,
  ArrowDownRight,
  X
} from 'lucide-react';

export default function TransactionsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [transactions, setTransactions] = useState([]);
  const [categories, setCategories] = useState([]);
  const [totals, setTotals] = useState({ totalIncome: '0.00', totalExpense: '0.00', netCashFlow: '0.00' });
  const [loading, setLoading] = useState(true);

  // Filters
  const [search, setSearch] = useState('');
  const [type, setType] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [paymentMethod, setPaymentMethod] = useState('');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);

  // Modal State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingTransaction, setEditingTransaction] = useState(null);

  useEffect(() => {
    fetchCategories();
    if (searchParams.get('action') === 'new') {
      setIsModalOpen(true);
      searchParams.delete('action');
      setSearchParams(searchParams);
    }
  }, []);

  useEffect(() => {
    fetchTransactions();
  }, [page, type, categoryId, paymentMethod, startDate, endDate]);

  const fetchCategories = async () => {
    try {
      const resp = await categoryApi.getCategories();
      setCategories(resp.data);
    } catch (err) {
      console.error('Failed to load categories', err);
    }
  };

  const fetchTransactions = async () => {
    setLoading(true);
    try {
      const params = {
        page,
        size: 15,
        type: type || undefined,
        categoryId: categoryId || undefined,
        paymentMethod: paymentMethod || undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        search: search.trim() || undefined
      };
      const resp = await transactionApi.getTransactions(params);
      setTransactions(resp.data.content);
      setTotalPages(resp.data.totalPages);
      setTotalElements(resp.data.totalElements);
      if (resp.data.totals) {
        setTotals(resp.data.totals);
      }
    } catch (err) {
      console.error('Failed to fetch transactions', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSearchSubmit = e => {
    e.preventDefault();
    setPage(0);
    fetchTransactions();
  };

  const clearFilters = () => {
    setSearch('');
    setType('');
    setCategoryId('');
    setPaymentMethod('');
    setStartDate('');
    setEndDate('');
    setPage(0);
  };

  const handleDelete = async id => {
    if (window.confirm('Are you sure you want to delete this transaction?')) {
      try {
        await transactionApi.deleteTransaction(id);
        fetchTransactions();
      } catch (err) {
        alert(err.response?.data?.message || 'Failed to delete transaction');
      }
    }
  };

  const handleExportCsv = async () => {
    try {
      const params = {
        type: type || undefined,
        categoryId: categoryId || undefined,
        paymentMethod: paymentMethod || undefined,
        startDate: startDate || undefined,
        endDate: endDate || undefined,
        search: search.trim() || undefined
      };
      const response = await transactionApi.exportCsv(params);
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `FinMate-Transactions-${new Date().toISOString().split('T')[0]}.csv`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (err) {
      alert('Failed to export transactions to CSV');
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header */}
      <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '1.625rem', fontWeight: 700, color: 'var(--text-primary)', letterSpacing: '-0.02em' }}>
            Transactions
          </h1>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
            Search, filter, categorize, and export your personal cash ledger
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <button onClick={handleExportCsv} className="btn-finmate-secondary">
            <Download size={16} />
            <span>Export CSV</span>
          </button>
          <button
            onClick={() => {
              setEditingTransaction(null);
              setIsModalOpen(true);
            }}
            className="btn-finmate-primary"
          >
            <Plus size={16} />
            <span>Record Transaction</span>
          </button>
        </div>
      </div>

      {/* Filtered Totals Bar */}
      <div
        className="finmate-card"
        style={{
          padding: '1rem 1.5rem',
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
          gap: '1rem',
          backgroundColor: 'var(--bg-card)'
        }}
      >
        <div>
          <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase' }}>
            Filtered Income
          </div>
          <div className="tabular-nums" style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--success-main)', marginTop: '0.2rem' }}>
            +{formatCurrency(totals.totalIncome)}
          </div>
        </div>

        <div>
          <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase' }}>
            Filtered Expenses
          </div>
          <div className="tabular-nums" style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--danger-main)', marginTop: '0.2rem' }}>
            -{formatCurrency(totals.totalExpense)}
          </div>
        </div>

        <div>
          <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase' }}>
            Net Period Flow
          </div>
          <div className="tabular-nums" style={{ fontSize: '1.25rem', fontWeight: 700, color: parseFloat(totals.netCashFlow) >= 0 ? 'var(--success-main)' : 'var(--danger-main)', marginTop: '0.2rem' }}>
            {formatCurrency(totals.netCashFlow)}
          </div>
        </div>

        <div>
          <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase' }}>
            Matching Entries
          </div>
          <div className="tabular-nums" style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--text-primary)', marginTop: '0.2rem' }}>
            {totalElements}
          </div>
        </div>
      </div>

      {/* Filter Controls Toolbar */}
      <div className="finmate-card" style={{ padding: '1rem 1.25rem' }}>
        <form onSubmit={handleSearchSubmit} style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(160px, 1fr))', gap: '0.75rem', alignItems: 'center' }}>
          {/* Search Box */}
          <div style={{ position: 'relative', gridColumn: 'span 2' }}>
            <Search size={16} style={{ position: 'absolute', left: 10, top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
            <input
              type="text"
              className="finmate-input"
              placeholder="Search by description or tag..."
              value={search}
              onChange={e => setSearch(e.target.value)}
              style={{ paddingLeft: '2rem' }}
            />
          </div>

          {/* Type Filter */}
          <select
            className="finmate-select"
            value={type}
            onChange={e => {
              setType(e.target.value);
              setPage(0);
            }}
          >
            <option value="">All Types</option>
            <option value="EXPENSE">Expenses Only</option>
            <option value="INCOME">Income Only</option>
          </select>

          {/* Category Filter */}
          <select
            className="finmate-select"
            value={categoryId}
            onChange={e => {
              setCategoryId(e.target.value);
              setPage(0);
            }}
          >
            <option value="">All Categories</option>
            {categories.map(c => (
              <option key={c.id} value={c.id}>
                {c.name} ({c.type})
              </option>
            ))}
          </select>

          {/* Payment Method Filter */}
          <select
            className="finmate-select"
            value={paymentMethod}
            onChange={e => {
              setPaymentMethod(e.target.value);
              setPage(0);
            }}
          >
            <option value="">All Methods</option>
            <option value="UPI">UPI</option>
            <option value="CREDIT_CARD">Credit Card</option>
            <option value="DEBIT_CARD">Debit Card</option>
            <option value="NET_BANKING">Net Banking</option>
            <option value="CASH">Cash</option>
            <option value="WALLET">Wallet</option>
          </select>

          {/* Start Date */}
          <input
            type="date"
            className="finmate-input"
            value={startDate}
            onChange={e => {
              setStartDate(e.target.value);
              setPage(0);
            }}
            placeholder="From Date"
          />

          {/* End Date */}
          <input
            type="date"
            className="finmate-input"
            value={endDate}
            onChange={e => {
              setEndDate(e.target.value);
              setPage(0);
            }}
            placeholder="To Date"
          />

          {/* Clear Filters Button */}
          {(search || type || categoryId || paymentMethod || startDate || endDate) && (
            <button
              type="button"
              onClick={clearFilters}
              className="btn-finmate-secondary"
              style={{ padding: '0.5rem', justifyContent: 'center' }}
              title="Clear all filters"
            >
              <X size={16} />
              <span>Clear</span>
            </button>
          )}
        </form>
      </div>

      {/* Transactions Table Card */}
      <div className="finmate-card" style={{ overflow: 'hidden' }}>
        {loading ? (
          <div style={{ padding: '3rem 0' }}>
            <LoadingSpinner message="Fetching transactions ledger..." />
          </div>
        ) : transactions.length > 0 ? (
          <>
            <div style={{ overflowX: 'auto' }}>
              <table className="finmate-table">
                <thead>
                  <tr>
                    <th>Date</th>
                    <th>Description</th>
                    <th>Category</th>
                    <th>Method</th>
                    <th style={{ textAlign: 'right' }}>Amount</th>
                    <th style={{ textAlign: 'center', width: 90 }}>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {transactions.map(t => {
                    const isIncome = t.type === 'INCOME';
                    return (
                      <tr key={t.id}>
                        <td style={{ whiteSpace: 'nowrap', color: 'var(--text-secondary)' }}>
                          {formatDate(t.transactionDate)}
                        </td>
                        <td>
                          <div style={{ fontWeight: 600, color: 'var(--text-primary)' }}>
                            {t.description}
                          </div>
                          {t.tags && t.tags.length > 0 && (
                            <div style={{ display: 'flex', gap: '0.25rem', marginTop: '0.2rem', flexWrap: 'wrap' }}>
                              {t.tags.map((tag, i) => (
                                <span key={i} className="status-pill neutral" style={{ fontSize: '0.6875rem', padding: '0.1rem 0.35rem' }}>
                                  #{tag}
                                </span>
                              ))}
                            </div>
                          )}
                        </td>
                        <td>
                          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                            <span
                              style={{
                                width: 8,
                                height: 8,
                                borderRadius: '50%',
                                backgroundColor: t.category?.color || '#94a3b8'
                              }}
                            />
                            <span>{t.category?.name || 'Uncategorized'}</span>
                          </div>
                        </td>
                        <td>
                          <span className="status-pill neutral" style={{ fontSize: '0.6875rem' }}>
                            {t.paymentMethod}
                          </span>
                        </td>
                        <td style={{ textAlign: 'right', whiteSpace: 'nowrap' }}>
                          <span
                            className="tabular-nums"
                            style={{
                              fontWeight: 700,
                              fontSize: '0.9375rem',
                              color: isIncome ? 'var(--success-main)' : 'var(--danger-main)',
                              display: 'inline-flex',
                              alignItems: 'center',
                              gap: '0.2rem'
                            }}
                          >
                            {isIncome ? '+' : '-'}{formatCurrency(t.amount)}
                          </span>
                        </td>
                        <td style={{ textAlign: 'center' }}>
                          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.4rem' }}>
                            <button
                              onClick={() => {
                                setEditingTransaction(t);
                                setIsModalOpen(true);
                              }}
                              style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer', padding: '0.25rem' }}
                              title="Edit"
                            >
                              <Edit2 size={15} />
                            </button>
                            <button
                              onClick={() => handleDelete(t.id)}
                              style={{ background: 'none', border: 'none', color: 'var(--danger-main)', cursor: 'pointer', padding: '0.25rem' }}
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

            {/* Pagination Controls */}
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '0.875rem 1.25rem',
                borderTop: '1px solid var(--border-color)',
                fontSize: '0.8125rem',
                color: 'var(--text-secondary)'
              }}
            >
              <div>
                Showing page <strong>{page + 1}</strong> of <strong>{totalPages || 1}</strong> ({totalElements} total)
              </div>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <button
                  disabled={page <= 0}
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  className="btn-finmate-secondary"
                  style={{ padding: '0.35rem 0.75rem', fontSize: '0.75rem' }}
                >
                  Previous
                </button>
                <button
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage(p => p + 1)}
                  className="btn-finmate-secondary"
                  style={{ padding: '0.35rem 0.75rem', fontSize: '0.75rem' }}
                >
                  Next
                </button>
              </div>
            </div>
          </>
        ) : (
          <EmptyState
            icon={Receipt}
            title="No transactions match your query"
            description="Adjust your search filters or record a new transaction to view it in the ledger."
            action={
              <button
                onClick={() => {
                  setEditingTransaction(null);
                  setIsModalOpen(true);
                }}
                className="btn-finmate-primary"
              >
                <Plus size={16} />
                <span>Record Transaction</span>
              </button>
            }
          />
        )}
      </div>

      {/* Modal for Add / Edit */}
      <TransactionModal
        isOpen={isModalOpen}
        onClose={() => {
          setIsModalOpen(false);
          setEditingTransaction(null);
        }}
        transaction={editingTransaction}
        onSuccess={fetchTransactions}
      />
    </div>
  );
}
