import React, { useState, useEffect } from 'react';
import { dashboardApi } from '../../api/dashboardApi';
import MetricCard from '../../components/common/MetricCard';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import IconHelper from '../../components/common/IconHelper';
import {
  TrendingUp,
  TrendingDown,
  Wallet,
  PiggyBank,
  Percent,
  Calendar,
  Sparkles,
  Users2,
  AlertTriangle,
  ArrowRight,
  Plus
} from 'lucide-react';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Legend,
  PieChart,
  Pie,
  Cell,
  LineChart,
  Line
} from 'recharts';
import { Link } from 'react-router-dom';

export default function DashboardPage() {
  const [period, setPeriod] = useState('this_month');
  const [loading, setLoading] = useState(true);
  const [summary, setSummary] = useState(null);
  const [monthlyData, setMonthlyData] = useState([]);
  const [categoryData, setCategoryData] = useState([]);
  const [trendData, setTrendData] = useState([]);
  const [budgetBars, setBudgetBars] = useState([]);
  const [insights, setInsights] = useState([]);
  const [groupSummary, setGroupSummary] = useState(null);

  const fetchDashboardData = async () => {
    setLoading(true);
    try {
      const [
        sumRes,
        monthlyRes,
        catRes,
        trendRes,
        budgetRes,
        insightsRes,
        groupRes
      ] = await Promise.all([
        dashboardApi.getSummary(period),
        dashboardApi.getMonthlyBar(6),
        dashboardApi.getCategoryDonut(),
        dashboardApi.getSpendingTrend(30),
        dashboardApi.getBudgetBars(),
        dashboardApi.getInsights(),
        dashboardApi.getGroupSummary()
      ]);

      setSummary(sumRes.data);
      setMonthlyData(monthlyRes.data);
      setCategoryData(catRes.data);
      setTrendData(trendRes.data);
      setBudgetBars(budgetRes.data);
      setInsights(insightsRes.data);
      setGroupSummary(groupRes.data);
    } catch (err) {
      console.error('Failed to load dashboard metrics:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
  }, [period]);

  if (loading && !summary) {
    return <LoadingSpinner fullPage message="Loading financial analytics..." />;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Top Header & Period Selector */}
      <div
        style={{
          display: 'flex',
          flexWrap: 'wrap',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: '1rem'
        }}
      >
        <div>
          <h1 style={{ fontSize: '1.625rem', fontWeight: 700, color: 'var(--text-primary)', letterSpacing: '-0.02em' }}>
            Financial Dashboard
          </h1>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
            Real-time cash flow, category distributions, budget pacing, and debt reconciliation
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <select
            className="finmate-select"
            value={period}
            onChange={e => setPeriod(e.target.value)}
            style={{ width: 'auto', paddingRight: '2rem' }}
          >
            <option value="this_month">This Month</option>
            <option value="last_month">Last Month</option>
            <option value="last_3_months">Last 3 Months</option>
            <option value="this_year">This Year</option>
          </select>

          <Link to="/transactions?action=new" className="btn-finmate-primary">
            <Plus size={16} />
            <span>Add Transaction</span>
          </Link>
        </div>
      </div>

      {/* KPI Cards Row 1 */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))',
          gap: '1rem'
        }}
      >
        <MetricCard
          title="Total Income"
          value={summary?.totalIncome || '₹0.00'}
          delta={summary?.deltas?.income}
          icon={TrendingUp}
          subtitle="vs prior period"
        />

        <MetricCard
          title="Total Expenses"
          value={summary?.totalExpense || '₹0.00'}
          delta={summary?.deltas?.expense}
          icon={TrendingDown}
          variant="inverse"
          subtitle="vs prior period"
        />

        <MetricCard
          title="Net Savings"
          value={summary?.netSavings || '₹0.00'}
          delta={summary?.deltas?.savings}
          icon={PiggyBank}
          subtitle={`Savings rate: ${summary?.savingsRate || 0}%`}
        />

        <MetricCard
          title="Free Balance"
          value={summary?.freeBalance || '₹0.00'}
          icon={Wallet}
          subtitle={`Current balance: ${summary?.currentBalance || '₹0.00'}`}
        />
      </div>

      {/* Secondary Metrics Bar */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
          gap: '1rem'
        }}
      >
        <div className="finmate-card" style={{ padding: '1rem 1.25rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div>
            <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase' }}>
              Avg Daily Spend
            </div>
            <div style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--text-primary)' }}>
              {summary?.avgDailySpend || '₹0.00'}
            </div>
          </div>
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Projected Month-End</div>
            <div style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--text-secondary)' }}>
              {summary?.projectedMonthEnd || '₹0.00'}
            </div>
          </div>
        </div>

        <div className="finmate-card" style={{ padding: '1rem 1.25rem', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div>
            <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-secondary)', textTransform: 'uppercase' }}>
              Allocated In Goals
            </div>
            <div style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--text-primary)' }}>
              {summary?.totalSavings || '₹0.00'}
            </div>
          </div>
          <Link to="/goals" style={{ fontSize: '0.8125rem', color: 'var(--primary-600)', textDecoration: 'none', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.25rem' }}>
            View Goals <ArrowRight size={14} />
          </Link>
        </div>
      </div>

      {/* Visualizations Row */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(420px, 1fr))',
          gap: '1.5rem'
        }}
      >
        {/* Income vs Expense 6-Month Bar Chart */}
        <div className="finmate-card">
          <div className="finmate-card-header">
            <div>
              <h3 style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                Cash Flow Trajectory
              </h3>
              <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', margin: 0 }}>
                Historical 6-month comparison of income and expenses
              </p>
            </div>
          </div>
          <div className="finmate-card-body" style={{ height: 300 }}>
            {monthlyData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={monthlyData} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
                  <XAxis dataKey="month" stroke="var(--text-muted)" fontSize={12} tickLine={false} />
                  <YAxis stroke="var(--text-muted)" fontSize={12} tickLine={false} tickFormatter={v => `₹${v / 1000}k`} />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: 'var(--bg-card)',
                      borderColor: 'var(--border-color)',
                      borderRadius: 'var(--radius-md)',
                      color: 'var(--text-primary)',
                      fontSize: '0.8125rem'
                    }}
                    formatter={val => [`₹${parseFloat(val).toLocaleString('en-IN')}`, '']}
                  />
                  <Legend />
                  <Bar dataKey="income" name="Income" fill="#10b981" radius={[4, 4, 0, 0]} />
                  <Bar dataKey="expense" name="Expense" fill="#f43f5e" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            ) : (
              <div style={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-muted)' }}>
                No transaction history found
              </div>
            )}
          </div>
        </div>

        {/* Category Expense Donut Chart */}
        <div className="finmate-card">
          <div className="finmate-card-header">
            <div>
              <h3 style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                Category Distribution
              </h3>
              <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', margin: 0 }}>
                Top expense allocations for the period
              </p>
            </div>
          </div>
          <div className="finmate-card-body" style={{ height: 300, display: 'flex', alignItems: 'center' }}>
            {categoryData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={categoryData}
                    cx="50%"
                    cy="50%"
                    innerRadius={65}
                    outerRadius={95}
                    paddingAngle={3}
                    dataKey="percent"
                    nameKey="name"
                  >
                    {categoryData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color || '#6366f1'} />
                    ))}
                  </Pie>
                  <Tooltip
                    contentStyle={{
                      backgroundColor: 'var(--bg-card)',
                      borderColor: 'var(--border-color)',
                      borderRadius: 'var(--radius-md)',
                      color: 'var(--text-primary)',
                      fontSize: '0.8125rem'
                    }}
                    formatter={(val, name, item) => [
                      `${val}% (₹${parseFloat(item.payload.amount).toLocaleString('en-IN')})`,
                      item.payload.name
                    ]}
                  />
                  <Legend
                    verticalAlign="middle"
                    align="right"
                    layout="vertical"
                    formatter={value => (
                      <span style={{ color: 'var(--text-primary)', fontSize: '0.8125rem' }}>{value}</span>
                    )}
                  />
                </PieChart>
              </ResponsiveContainer>
            ) : (
              <div style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-muted)' }}>
                No expenses recorded for this month
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Row 3: Spending Trends & Budgets Progress */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(420px, 1fr))',
          gap: '1.5rem'
        }}
      >
        {/* Daily Spending Trajectory */}
        <div className="finmate-card">
          <div className="finmate-card-header">
            <div>
              <h3 style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                30-Day Spending Velocity
              </h3>
              <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', margin: 0 }}>
                Daily expense run-rate over the past 30 days
              </p>
            </div>
          </div>
          <div className="finmate-card-body" style={{ height: 260 }}>
            {trendData.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={trendData} margin={{ top: 10, right: 10, left: -10, bottom: 0 }}>
                  <XAxis dataKey="label" stroke="var(--text-muted)" fontSize={11} tickLine={false} />
                  <YAxis stroke="var(--text-muted)" fontSize={11} tickLine={false} tickFormatter={v => `₹${v}`} />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: 'var(--bg-card)',
                      borderColor: 'var(--border-color)',
                      borderRadius: 'var(--radius-md)',
                      color: 'var(--text-primary)',
                      fontSize: '0.8125rem'
                    }}
                    formatter={val => [`₹${parseFloat(val).toLocaleString('en-IN')}`, 'Spend']}
                  />
                  <Line
                    type="monotone"
                    dataKey="amount"
                    stroke="#6366f1"
                    strokeWidth={2.5}
                    dot={false}
                    activeDot={{ r: 5 }}
                  />
                </LineChart>
              </ResponsiveContainer>
            ) : (
              <div style={{ height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-muted)' }}>
                No daily data available
              </div>
            )}
          </div>
        </div>

        {/* Budget Progress Bars */}
        <div className="finmate-card">
          <div className="finmate-card-header">
            <div>
              <h3 style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)' }}>
                Active Budget Utilization
              </h3>
              <p style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', margin: 0 }}>
                Current month budget consumption pacing
              </p>
            </div>
            <Link to="/budgets" style={{ fontSize: '0.8125rem', color: 'var(--primary-600)', textDecoration: 'none', fontWeight: 600 }}>
              Manage
            </Link>
          </div>
          <div className="finmate-card-body" style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {budgetBars.length > 0 ? (
              budgetBars.slice(0, 4).map(b => {
                const percent = b.utilization || 0;
                let color = 'var(--success-main)';
                if (percent >= 100) color = 'var(--danger-main)';
                else if (percent >= 80) color = 'var(--warning-main)';

                return (
                  <div key={b.budgetId} style={{ display: 'flex', flexDirection: 'column', gap: '0.35rem' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.8125rem' }}>
                      <span style={{ fontWeight: 600, color: 'var(--text-primary)' }}>{b.categoryName}</span>
                      <span className="tabular-nums" style={{ color: 'var(--text-secondary)' }}>
                        ₹{b.spentAmount} / ₹{b.limitAmount} ({percent}%)
                      </span>
                    </div>
                    <div
                      style={{
                        height: 8,
                        backgroundColor: 'var(--bg-tertiary)',
                        borderRadius: 4,
                        overflow: 'hidden'
                      }}
                    >
                      <div
                        style={{
                          height: '100%',
                          width: `${Math.min(100, percent)}%`,
                          backgroundColor: color,
                          borderRadius: 4,
                          transition: 'width 0.3s ease'
                        }}
                      />
                    </div>
                  </div>
                );
              })
            ) : (
              <div style={{ padding: '2rem 0', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.875rem' }}>
                No category budgets set for this month.{' '}
                <Link to="/budgets" style={{ color: 'var(--primary-600)' }}>Create one</Link>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Row 4: Smart Insights & Shared Groups Widget */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
          gap: '1.5rem'
        }}
      >
        {/* Rule-Based Smart Insights */}
        <div className="finmate-card">
          <div className="finmate-card-header">
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Sparkles size={18} style={{ color: 'var(--primary-600)' }} />
              <h3 style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>
                Smart Insights
              </h3>
            </div>
          </div>
          <div className="finmate-card-body" style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            {insights.map((ins, i) => (
              <div
                key={i}
                style={{
                  padding: '0.75rem 1rem',
                  borderRadius: 'var(--radius-md)',
                  backgroundColor: 'var(--bg-tertiary)',
                  borderLeft: `4px solid ${
                    ins.type === 'CRITICAL' ? 'var(--danger-main)' :
                    ins.type === 'WARNING' ? 'var(--warning-main)' :
                    ins.type === 'TIP' ? 'var(--primary-600)' : 'var(--success-main)'
                  }`
                }}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.25rem' }}>
                  <span style={{ fontSize: '0.8125rem', fontWeight: 700, color: 'var(--text-primary)' }}>
                    {ins.title}
                  </span>
                  <span className="status-pill neutral" style={{ fontSize: '0.6875rem' }}>
                    {ins.category}
                  </span>
                </div>
                <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', margin: 0, lineHeight: 1.4 }}>
                  {ins.message}
                </p>
              </div>
            ))}
          </div>
        </div>

        {/* Group Balances Overview Card */}
        <div className="finmate-card">
          <div className="finmate-card-header">
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Users2 size={18} style={{ color: 'var(--primary-600)' }} />
              <h3 style={{ fontSize: '1rem', fontWeight: 600, color: 'var(--text-primary)', margin: 0 }}>
                Shared Group Balances
              </h3>
            </div>
            <Link to="/groups" style={{ fontSize: '0.8125rem', color: 'var(--primary-600)', textDecoration: 'none', fontWeight: 600 }}>
              View Groups
            </Link>
          </div>
          <div className="finmate-card-body" style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '1rem' }}>
              <div style={{ padding: '0.875rem', borderRadius: 'var(--radius-md)', backgroundColor: 'var(--success-bg)' }}>
                <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--success-text)', textTransform: 'uppercase' }}>
                  You Are Owed
                </div>
                <div className="tabular-nums" style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--success-text)', marginTop: '0.25rem' }}>
                  {groupSummary?.youAreOwed || '₹0.00'}
                </div>
              </div>

              <div style={{ padding: '0.875rem', borderRadius: 'var(--radius-md)', backgroundColor: 'var(--danger-bg)' }}>
                <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--danger-text)', textTransform: 'uppercase' }}>
                  You Owe
                </div>
                <div className="tabular-nums" style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--danger-text)', marginTop: '0.25rem' }}>
                  {groupSummary?.youOwe || '₹0.00'}
                </div>
              </div>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0.5rem 0', borderTop: '1px solid var(--border-color)', fontSize: '0.8125rem' }}>
              <span style={{ color: 'var(--text-secondary)' }}>Net Position Across Active Groups:</span>
              <span className="tabular-nums" style={{ fontWeight: 700, color: 'var(--text-primary)' }}>
                {groupSummary?.netBalance || '₹0.00'}
              </span>
            </div>

            <Link
              to="/groups"
              className="btn-finmate-secondary"
              style={{ width: '100%', fontSize: '0.8125rem' }}
            >
              Reconcile & Settle Group Debts
              <ArrowRight size={14} />
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
