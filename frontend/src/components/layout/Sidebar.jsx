import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import {
  LayoutDashboard,
  Receipt,
  Repeat,
  PiggyBank,
  Target,
  Users2,
  BarChart3,
  Bell,
  Settings,
  ShieldCheck,
  FolderTree,
  Activity,
  Layers
} from 'lucide-react';

export default function Sidebar({ isOpen, onClose }) {
  const { isAdmin } = useAuth();

  const navLinkStyle = ({ isActive }) => ({
    display: 'flex',
    alignItems: 'center',
    gap: '0.75rem',
    padding: '0.625rem 0.875rem',
    borderRadius: 'var(--radius-md)',
    fontSize: '0.875rem',
    fontWeight: isActive ? 600 : 500,
    color: isActive ? 'var(--primary-600)' : 'var(--text-secondary)',
    backgroundColor: isActive ? 'var(--primary-50)' : 'transparent',
    textDecoration: 'none',
    transition: 'all 0.15s ease'
  });

  return (
    <>
      {isOpen && (
        <div
          onClick={onClose}
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0,0,0,0.4)',
            zIndex: 99
          }}
          className="d-lg-none"
        />
      )}

      <aside className={`app-sidebar ${isOpen ? 'open' : ''}`}>
        {/* Brand Header */}
        <div
          style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            gap: '0.75rem',
            padding: '0 1.5rem',
            borderBottom: '1px solid var(--border-color)'
          }}
        >
          <div
            style={{
              width: 32,
              height: 32,
              borderRadius: 'var(--radius-md)',
              backgroundColor: 'var(--primary-600)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#ffffff'
            }}
          >
            <Layers size={18} />
          </div>
          <div>
            <div style={{ fontWeight: 700, fontSize: '1.0625rem', color: 'var(--text-primary)', letterSpacing: '-0.02em', lineHeight: 1.1 }}>
              FinMate
            </div>
            <div style={{ fontSize: '0.6875rem', color: 'var(--text-muted)', fontWeight: 500 }}>
              Smart Personal Finance
            </div>
          </div>
        </div>

        {/* Navigation Sections */}
        <div
          style={{
            flex: 1,
            overflowY: 'auto',
            padding: '1rem',
            display: 'flex',
            flexDirection: 'column',
            gap: '1.25rem'
          }}
        >
          {/* Main */}
          <div>
            <div style={{ fontSize: '0.6875rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.08em', padding: '0 0.5rem 0.5rem' }}>
              Overview
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.2rem' }}>
              <NavLink to="/dashboard" style={navLinkStyle} onClick={onClose}>
                <LayoutDashboard size={18} />
                Dashboard
              </NavLink>
              <NavLink to="/reports" style={navLinkStyle} onClick={onClose}>
                <BarChart3 size={18} />
                Reports & PDF
              </NavLink>
            </div>
          </div>

          {/* Personal Finance */}
          <div>
            <div style={{ fontSize: '0.6875rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.08em', padding: '0 0.5rem 0.5rem' }}>
              Personal Finance
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.2rem' }}>
              <NavLink to="/transactions" style={navLinkStyle} onClick={onClose}>
                <Receipt size={18} />
                Transactions
              </NavLink>
              <NavLink to="/recurring" style={navLinkStyle} onClick={onClose}>
                <Repeat size={18} />
                Recurring Rules
              </NavLink>
              <NavLink to="/budgets" style={navLinkStyle} onClick={onClose}>
                <PiggyBank size={18} />
                Budgets
              </NavLink>
              <NavLink to="/goals" style={navLinkStyle} onClick={onClose}>
                <Target size={18} />
                Savings Goals
              </NavLink>
            </div>
          </div>

          {/* Collaborative */}
          <div>
            <div style={{ fontSize: '0.6875rem', fontWeight: 700, color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.08em', padding: '0 0.5rem 0.5rem' }}>
              Shared Expenses
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.2rem' }}>
              <NavLink to="/groups" style={navLinkStyle} onClick={onClose}>
                <Users2 size={18} />
                Expense Groups
              </NavLink>
            </div>
          </div>

          {/* Admin Console */}
          {isAdmin && (
            <div>
              <div style={{ fontSize: '0.6875rem', fontWeight: 700, color: 'var(--primary-600)', textTransform: 'uppercase', letterSpacing: '0.08em', padding: '0 0.5rem 0.5rem' }}>
                Admin Console
              </div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.2rem' }}>
                <NavLink to="/admin/users" style={navLinkStyle} onClick={onClose}>
                  <ShieldCheck size={18} />
                  User Directory
                </NavLink>
                <NavLink to="/admin/categories" style={navLinkStyle} onClick={onClose}>
                  <FolderTree size={18} />
                  Categories
                </NavLink>
                <NavLink to="/admin/stats" style={navLinkStyle} onClick={onClose}>
                  <Activity size={18} />
                  Platform Stats
                </NavLink>
              </div>
            </div>
          )}
        </div>

        {/* Sidebar Footer */}
        <div
          style={{
            padding: '0.875rem 1.25rem',
            borderTop: '1px solid var(--border-color)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            fontSize: '0.75rem',
            color: 'var(--text-muted)'
          }}
        >
          <NavLink to="/settings" style={{ color: 'var(--text-secondary)', textDecoration: 'none', display: 'flex', alignItems: 'center', gap: '0.4rem', fontWeight: 500 }}>
            <Settings size={15} />
            Settings
          </NavLink>
          <span>v1.0.0</span>
        </div>
      </aside>
    </>
  );
}
