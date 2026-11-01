import React, { useState, useEffect } from 'react';
import { adminApi } from '../../api/adminApi';
import { formatCurrency } from '../../utils/formatters';
import MetricCard from '../../components/common/MetricCard';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import {
  Activity,
  Users2,
  Receipt,
  Scale,
  Server,
  Database,
  ShieldCheck,
  Cpu
} from 'lucide-react';

export default function AdminStatsPage() {
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchStats = async () => {
      setLoading(true);
      try {
        const resp = await adminApi.getPlatformStats();
        setStats(resp.data);
      } catch (err) {
        console.error('Failed to load platform stats', err);
      } finally {
        setLoading(false);
      }
    };

    fetchStats();
  }, []);

  if (loading) {
    return <LoadingSpinner fullPage message="Querying platform telemetry..." />;
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header */}
      <div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
          <span className="status-pill warning">Admin Console</span>
        </div>
        <h1 style={{ fontSize: '1.625rem', fontWeight: 700, color: 'var(--text-primary)', letterSpacing: '-0.02em' }}>
          Platform Analytics & System Health
        </h1>
        <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
          High-level operational metrics, global transaction throughput, and infrastructure telemetry
        </p>
      </div>

      {/* Primary KPI Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(240px, 1fr))', gap: '1rem' }}>
        <MetricCard
          title="Total Registered Accounts"
          value={stats?.totalUsers || 0}
          icon={Users2}
          subtitle={`Active: ${stats?.activeUsers || 0}`}
        />

        <MetricCard
          title="Total Transactions Logged"
          value={stats?.totalTransactions || 0}
          icon={Receipt}
          subtitle="Across personal ledgers"
        />

        <MetricCard
          title="Active Shared Groups"
          value={stats?.totalGroups || 0}
          icon={Scale}
          subtitle="Collaborative workspaces"
        />

        <MetricCard
          title="Platform Volume Handled"
          value={formatCurrency(stats?.totalVolume || '0.00')}
          icon={Activity}
          subtitle="Cumulative ledger throughput"
        />
      </div>

      {/* Infrastructure Telemetry Card */}
      <div className="finmate-card" style={{ padding: '1.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1.25rem' }}>
          <Server size={18} style={{ color: 'var(--primary-600)' }} />
          <h3 style={{ fontSize: '1.0625rem', fontWeight: 600, margin: 0 }}>
            System Architecture Status
          </h3>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem' }}>
          <div style={{ padding: '1rem', backgroundColor: 'var(--bg-tertiary)', borderRadius: 'var(--radius-md)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
              <Cpu size={16} style={{ color: 'var(--primary-600)' }} />
              <strong style={{ fontSize: '0.875rem' }}>Spring Boot Runtime</strong>
            </div>
            <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
              v3.3.4 (Java 21 / OpenJDK)
            </div>
            <span className="status-pill success" style={{ marginTop: '0.5rem' }}>
              Healthy
            </span>
          </div>

          <div style={{ padding: '1rem', backgroundColor: 'var(--bg-tertiary)', borderRadius: 'var(--radius-md)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
              <Database size={16} style={{ color: 'var(--primary-600)' }} />
              <strong style={{ fontSize: '0.875rem' }}>Database Persistence</strong>
            </div>
            <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
              H2 in-memory (MySQL Mode) / MySQL 8
            </div>
            <span className="status-pill success" style={{ marginTop: '0.5rem' }}>
              Connected
            </span>
          </div>

          <div style={{ padding: '1rem', backgroundColor: 'var(--bg-tertiary)', borderRadius: 'var(--radius-md)' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
              <ShieldCheck size={16} style={{ color: 'var(--primary-600)' }} />
              <strong style={{ fontSize: '0.875rem' }}>Security Protocol</strong>
            </div>
            <div style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>
              Spring Security 6 + Rotating JWT
            </div>
            <span className="status-pill success" style={{ marginTop: '0.5rem' }}>
              Enforced
            </span>
          </div>
        </div>
      </div>
    </div>
  );
}
