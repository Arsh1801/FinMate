import React, { useState, useEffect } from 'react';
import { reportApi } from '../../api/reportApi';
import { formatCurrency } from '../../utils/formatters';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import {
  FileText,
  Download,
  Calendar,
  PiggyBank,
  TrendingUp,
  TrendingDown,
  Target,
  Percent,
  CheckCircle2
} from 'lucide-react';

export default function ReportsPage() {
  const today = new Date();
  const [selectedMonth, setSelectedMonth] = useState(
    `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}`
  );
  const [report, setReport] = useState(null);
  const [loading, setLoading] = useState(true);
  const [downloading, setDownloading] = useState(false);

  const fetchReport = async () => {
    setLoading(true);
    try {
      const resp = await reportApi.getMonthlyReport(selectedMonth);
      setReport(resp.data);
    } catch (err) {
      console.error('Failed to load monthly report', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReport();
  }, [selectedMonth]);

  const handleDownloadPdf = async () => {
    setDownloading(true);
    try {
      const resp = await reportApi.downloadMonthlyPdf(selectedMonth);
      const url = window.URL.createObjectURL(new Blob([resp.data], { type: 'application/pdf' }));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `FinMate-Monthly-Report-${selectedMonth}.pdf`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (err) {
      alert('Failed to generate PDF financial statement');
    } finally {
      setDownloading(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header */}
      <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '1.625rem', fontWeight: 700, color: 'var(--text-primary)', letterSpacing: '-0.02em' }}>
            Monthly Financial Statement & PDF Export
          </h1>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
            Structured reconciliation reports, category expenditures, and bank-grade PDF exports
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <input
            type="month"
            className="finmate-input"
            value={selectedMonth}
            onChange={e => setSelectedMonth(e.target.value)}
            style={{ width: 'auto' }}
          />

          <button
            onClick={handleDownloadPdf}
            disabled={downloading}
            className="btn-finmate-primary"
          >
            <Download size={16} />
            <span>{downloading ? 'Rendering PDF...' : 'Download PDF Report'}</span>
          </button>
        </div>
      </div>

      {loading ? (
        <div style={{ padding: '3rem 0' }}>
          <LoadingSpinner message="Generating structured monthly statement..." />
        </div>
      ) : report ? (
        <>
          {/* Executive Summary Card */}
          <div
            className="finmate-card"
            style={{
              padding: '1.5rem',
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))',
              gap: '1.5rem',
              backgroundColor: 'var(--bg-card)'
            }}
          >
            <div>
              <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase' }}>
                Total Monthly Income
              </div>
              <div className="tabular-nums" style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--success-main)', marginTop: '0.25rem' }}>
                {report.totalIncome}
              </div>
            </div>

            <div>
              <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase' }}>
                Total Monthly Expenses
              </div>
              <div className="tabular-nums" style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--danger-main)', marginTop: '0.25rem' }}>
                {report.totalExpense}
              </div>
            </div>

            <div>
              <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase' }}>
                Net Monthly Savings
              </div>
              <div className="tabular-nums" style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--text-primary)', marginTop: '0.25rem' }}>
                {report.netSavings}
              </div>
            </div>

            <div>
              <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase' }}>
                Savings Rate
              </div>
              <div className="tabular-nums" style={{ fontSize: '1.5rem', fontWeight: 700, color: 'var(--primary-600)', marginTop: '0.25rem' }}>
                {report.savingsRate}%
              </div>
            </div>
          </div>

          {/* Category Breakdown Table */}
          <div className="finmate-card" style={{ overflow: 'hidden' }}>
            <div className="finmate-card-header">
              <h3 style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>
                1. Category Expenditure Breakdown
              </h3>
            </div>
            {report.categoryBreakdown?.length > 0 ? (
              <div style={{ overflowX: 'auto' }}>
                <table className="finmate-table">
                  <thead>
                    <tr>
                      <th>Category</th>
                      <th>Type</th>
                      <th style={{ textAlign: 'right' }}>Total Amount</th>
                      <th style={{ textAlign: 'right' }}>% of Total Outflow</th>
                    </tr>
                  </thead>
                  <tbody>
                    {report.categoryBreakdown.map((item, i) => (
                      <tr key={i}>
                        <td style={{ fontWeight: 600 }}>{item.categoryName}</td>
                        <td>
                          <span className="status-pill neutral">{item.categoryType}</span>
                        </td>
                        <td className="tabular-nums" style={{ textAlign: 'right' }}>
                          {item.amount}
                        </td>
                        <td className="tabular-nums" style={{ textAlign: 'right', fontWeight: 600 }}>
                          {item.percentage}%
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)' }}>
                No expenses recorded for this month.
              </div>
            )}
          </div>

          {/* Budget Performance Table */}
          <div className="finmate-card" style={{ overflow: 'hidden' }}>
            <div className="finmate-card-header">
              <h3 style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>
                2. Budget Target Performance
              </h3>
            </div>
            {report.budgetPerformance?.length > 0 ? (
              <div style={{ overflowX: 'auto' }}>
                <table className="finmate-table">
                  <thead>
                    <tr>
                      <th>Budget Scope</th>
                      <th style={{ textAlign: 'right' }}>Budgeted Cap</th>
                      <th style={{ textAlign: 'right' }}>Spent Amount</th>
                      <th style={{ textAlign: 'right' }}>Remaining</th>
                      <th style={{ textAlign: 'right' }}>% Consumed</th>
                      <th style={{ textAlign: 'center' }}>Outcome</th>
                    </tr>
                  </thead>
                  <tbody>
                    {report.budgetPerformance.map((b, i) => (
                      <tr key={i}>
                        <td style={{ fontWeight: 600 }}>{b.categoryName}</td>
                        <td className="tabular-nums" style={{ textAlign: 'right' }}>
                          {formatCurrency(b.budgetAmount)}
                        </td>
                        <td className="tabular-nums" style={{ textAlign: 'right' }}>
                          {formatCurrency(b.spentAmount)}
                        </td>
                        <td className="tabular-nums" style={{ textAlign: 'right' }}>
                          {formatCurrency(b.remainingAmount)}
                        </td>
                        <td className="tabular-nums" style={{ textAlign: 'right', fontWeight: 600 }}>
                          {b.percentageUsed}%
                        </td>
                        <td style={{ textAlign: 'center' }}>
                          <span
                            className={`status-pill ${
                              b.status === 'EXCEEDED' ? 'danger' :
                              b.status === 'WARNING' ? 'warning' : 'success'
                            }`}
                          >
                            {b.status}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)' }}>
                No active budgets were configured for this month.
              </div>
            )}
          </div>

          {/* Savings Goals Status */}
          <div className="finmate-card" style={{ overflow: 'hidden' }}>
            <div className="finmate-card-header">
              <h3 style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>
                3. Active Savings Goals Status
              </h3>
            </div>
            {report.goalsSummary?.length > 0 ? (
              <div style={{ overflowX: 'auto' }}>
                <table className="finmate-table">
                  <thead>
                    <tr>
                      <th>Goal Name</th>
                      <th style={{ textAlign: 'right' }}>Target Amount</th>
                      <th style={{ textAlign: 'right' }}>Saved to Date</th>
                      <th style={{ textAlign: 'right' }}>Completion</th>
                      <th>Target Date</th>
                    </tr>
                  </thead>
                  <tbody>
                    {report.goalsSummary.map((g, i) => (
                      <tr key={i}>
                        <td style={{ fontWeight: 600 }}>{g.goalName}</td>
                        <td className="tabular-nums" style={{ textAlign: 'right' }}>
                          {formatCurrency(g.targetAmount)}
                        </td>
                        <td className="tabular-nums" style={{ textAlign: 'right', color: 'var(--success-main)', fontWeight: 700 }}>
                          {formatCurrency(g.currentAmount)}
                        </td>
                        <td className="tabular-nums" style={{ textAlign: 'right', fontWeight: 600 }}>
                          {g.progressPercentage}%
                        </td>
                        <td style={{ color: 'var(--text-secondary)' }}>{g.targetDate}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div style={{ padding: '2rem', textAlign: 'center', color: 'var(--text-muted)' }}>
                No active savings goals found.
              </div>
            )}
          </div>
        </>
      ) : null}
    </div>
  );
}
