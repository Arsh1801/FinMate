import React from 'react';
import { ArrowUpRight, ArrowDownRight, Minus } from 'lucide-react';

export default function MetricCard({
  title,
  value,
  delta,
  icon: Icon,
  variant = 'neutral',
  subtitle
}) {
  const isPositive = delta?.startsWith('+');
  const isNegative = delta?.startsWith('-');

  return (
    <div className="kpi-card finmate-card">
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <span className="kpi-label">{title}</span>
        {Icon && (
          <div
            style={{
              padding: '0.4rem',
              borderRadius: 'var(--radius-md)',
              backgroundColor: 'var(--bg-tertiary)',
              color: 'var(--primary-600)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center'
            }}
          >
            <Icon size={18} />
          </div>
        )}
      </div>

      <div className="kpi-value">{value}</div>

      {(delta || subtitle) && (
        <div className="kpi-footer">
          {delta && (
            <span
              className={`status-pill ${
                variant === 'inverse'
                  ? isPositive ? 'danger' : 'success'
                  : isPositive ? 'success' : isNegative ? 'danger' : 'neutral'
              }`}
              style={{ fontSize: '0.75rem', padding: '0.15rem 0.4rem' }}
            >
              {isPositive && <ArrowUpRight size={13} />}
              {isNegative && <ArrowDownRight size={13} />}
              {!isPositive && !isNegative && <Minus size={13} />}
              {delta}
            </span>
          )}
          {subtitle && (
            <span style={{ color: 'var(--text-secondary)', fontSize: '0.75rem' }}>
              {subtitle}
            </span>
          )}
        </div>
      )}
    </div>
  );
}
