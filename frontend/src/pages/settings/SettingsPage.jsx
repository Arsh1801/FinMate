import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { useTheme } from '../../context/ThemeContext';
import { authApi } from '../../api/authApi';
import { dashboardApi } from '../../api/dashboardApi';
import axiosClient from '../../api/axiosClient';
import { formatCurrency } from '../../utils/formatters';
import {
  User,
  Settings,
  Lock,
  Globe,
  Bell,
  CheckCircle2,
  AlertCircle,
  ArrowRightLeft,
  KeyRound
} from 'lucide-react';

export default function SettingsPage() {
  const { user, updateUser } = useAuth();
  const { theme, setTheme } = useTheme();

  // Profile state
  const [fullName, setFullName] = useState(user?.fullName || '');
  const [phone, setPhone] = useState(user?.phone || '');
  const [profileMsg, setProfileMsg] = useState('');
  const [profileError, setProfileError] = useState('');
  const [profileLoading, setProfileLoading] = useState(false);

  // Password state
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [pwdMsg, setPwdMsg] = useState('');
  const [pwdError, setPwdError] = useState('');
  const [pwdLoading, setPwdLoading] = useState(false);

  // External Currency Converter Widget
  const [baseCurrency, setBaseCurrency] = useState('USD');
  const [targetCurrency, setTargetCurrency] = useState('INR');
  const [exchangeAmount, setExchangeAmount] = useState('100');
  const [useWebClient, setUseWebClient] = useState(false);
  const [exchangeRateData, setExchangeRateData] = useState(null);
  const [rateLoading, setRateLoading] = useState(false);

  useEffect(() => {
    fetchExchangeRate();
  }, [baseCurrency, targetCurrency, useWebClient]);

  const fetchExchangeRate = async () => {
    setRateLoading(true);
    try {
      const resp = await axiosClient.get('/external/exchange-rate', {
        params: { base: baseCurrency, target: targetCurrency, useWebClient }
      });
      setExchangeRateData(resp.data);
    } catch (err) {
      console.error('Failed to query exchange rate', err);
    } finally {
      setRateLoading(false);
    }
  };

  const handleUpdateProfile = async e => {
    e.preventDefault();
    setProfileMsg('');
    setProfileError('');
    setProfileLoading(true);

    try {
      const resp = await axiosClient.put('/users/me', {
        fullName: fullName.trim(),
        phone: phone ? phone.trim() : null
      });
      updateUser(resp.data);
      setProfileMsg('Profile updated successfully!');
      setTimeout(() => setProfileMsg(''), 3000);
    } catch (err) {
      setProfileError(err.response?.data?.message || 'Failed to update profile');
    } finally {
      setProfileLoading(false);
    }
  };

  const handleChangePassword = async e => {
    e.preventDefault();
    setPwdMsg('');
    setPwdError('');

    if (newPassword !== confirmPassword) {
      setPwdError('New passwords do not match');
      return;
    }

    if (newPassword.length < 8) {
      setPwdError('New password must be at least 8 characters long');
      return;
    }

    setPwdLoading(true);

    try {
      await authApi.changePassword({ oldPassword, newPassword });
      setPwdMsg('Password updated successfully!');
      setOldPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setTimeout(() => setPwdMsg(''), 3000);
    } catch (err) {
      setPwdError(err.response?.data?.message || 'Failed to update password');
    } finally {
      setPwdLoading(false);
    }
  };

  const convertedValue = exchangeRateData?.rate
    ? (parseFloat(exchangeAmount || 0) * exchangeRateData.rate).toFixed(2)
    : '0.00';

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem', maxWidth: 900, margin: '0 auto' }}>
      {/* Header */}
      <div>
        <h1 style={{ fontSize: '1.625rem', fontWeight: 700, color: 'var(--text-primary)', letterSpacing: '-0.02em' }}>
          Account Settings & Preferences
        </h1>
        <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
          Manage your personal details, credentials, visual themes, and external financial tools
        </p>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(380px, 1fr))', gap: '1.5rem' }}>
        {/* Profile Card */}
        <div className="finmate-card" style={{ padding: '1.5rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1.25rem' }}>
            <User size={18} style={{ color: 'var(--primary-600)' }} />
            <h3 style={{ fontSize: '1.0625rem', fontWeight: 600, margin: 0 }}>
              Profile Information
            </h3>
          </div>

          {profileMsg && (
            <div style={{ padding: '0.5rem 0.75rem', backgroundColor: 'var(--success-bg)', color: 'var(--success-text)', borderRadius: 'var(--radius-sm)', fontSize: '0.8125rem', marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
              <CheckCircle2 size={16} /> {profileMsg}
            </div>
          )}

          {profileError && (
            <div style={{ padding: '0.5rem 0.75rem', backgroundColor: 'var(--danger-bg)', color: 'var(--danger-text)', borderRadius: 'var(--radius-sm)', fontSize: '0.8125rem', marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
              <AlertCircle size={16} /> {profileError}
            </div>
          )}

          <form onSubmit={handleUpdateProfile} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                Full Name *
              </label>
              <input
                type="text"
                required
                className="finmate-input"
                value={fullName}
                onChange={e => setFullName(e.target.value)}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                Email Address
              </label>
              <input
                type="email"
                disabled
                className="finmate-input"
                value={user?.email || ''}
                style={{ backgroundColor: 'var(--bg-tertiary)', color: 'var(--text-muted)' }}
              />
              <span style={{ fontSize: '0.6875rem', color: 'var(--text-muted)' }}>
                Email address is tied to your login identity and cannot be altered.
              </span>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                Phone Number
              </label>
              <input
                type="tel"
                className="finmate-input"
                placeholder="10-digit mobile"
                value={phone}
                onChange={e => setPhone(e.target.value)}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '0.5rem' }}>
              <button type="submit" disabled={profileLoading} className="btn-finmate-primary">
                {profileLoading ? 'Saving...' : 'Save Profile'}
              </button>
            </div>
          </form>
        </div>

        {/* Change Password Card */}
        <div className="finmate-card" style={{ padding: '1.5rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1.25rem' }}>
            <Lock size={18} style={{ color: 'var(--primary-600)' }} />
            <h3 style={{ fontSize: '1.0625rem', fontWeight: 600, margin: 0 }}>
              Change Password
            </h3>
          </div>

          {pwdMsg && (
            <div style={{ padding: '0.5rem 0.75rem', backgroundColor: 'var(--success-bg)', color: 'var(--success-text)', borderRadius: 'var(--radius-sm)', fontSize: '0.8125rem', marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
              <CheckCircle2 size={16} /> {pwdMsg}
            </div>
          )}

          {pwdError && (
            <div style={{ padding: '0.5rem 0.75rem', backgroundColor: 'var(--danger-bg)', color: 'var(--danger-text)', borderRadius: 'var(--radius-sm)', fontSize: '0.8125rem', marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
              <AlertCircle size={16} /> {pwdError}
            </div>
          )}

          <form onSubmit={handleChangePassword} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                Current Password *
              </label>
              <input
                type="password"
                required
                className="finmate-input"
                placeholder="••••••••"
                value={oldPassword}
                onChange={e => setOldPassword(e.target.value)}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                New Password *
              </label>
              <input
                type="password"
                required
                className="finmate-input"
                placeholder="Minimum 8 characters"
                value={newPassword}
                onChange={e => setNewPassword(e.target.value)}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                Confirm New Password *
              </label>
              <input
                type="password"
                required
                className="finmate-input"
                placeholder="Repeat new password"
                value={confirmPassword}
                onChange={e => setConfirmPassword(e.target.value)}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '0.5rem' }}>
              <button type="submit" disabled={pwdLoading} className="btn-finmate-primary">
                {pwdLoading ? 'Updating...' : 'Update Password'}
              </button>
            </div>
          </form>
        </div>
      </div>

      {/* Row 2: Live Currency Converter & Backend Integration */}
      <div className="finmate-card" style={{ padding: '1.5rem' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Globe size={18} style={{ color: 'var(--primary-600)' }} />
            <div>
              <h3 style={{ fontSize: '1.0625rem', fontWeight: 600, margin: 0 }}>
                Live Foreign Exchange Converter
              </h3>
              <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)', margin: 0 }}>
                Demonstrates Spring Boot HTTP client integration via RestTemplate & WebClient
              </p>
            </div>
          </div>

          <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', fontSize: '0.8125rem', cursor: 'pointer' }}>
            <input
              type="checkbox"
              checked={useWebClient}
              onChange={e => setUseWebClient(e.target.checked)}
            />
            <span>Use Reactive WebClient</span>
          </label>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem', alignItems: 'flex-end' }}>
          <div>
            <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
              Amount
            </label>
            <input
              type="number"
              className="finmate-input tabular-nums"
              value={exchangeAmount}
              onChange={e => setExchangeAmount(e.target.value)}
            />
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
              From (Base Currency)
            </label>
            <select
              className="finmate-select"
              value={baseCurrency}
              onChange={e => setBaseCurrency(e.target.value)}
            >
              <option value="USD">USD - US Dollar</option>
              <option value="EUR">EUR - Euro</option>
              <option value="GBP">GBP - British Pound</option>
              <option value="AED">AED - UAE Dirham</option>
              <option value="SGD">SGD - Singapore Dollar</option>
              <option value="CAD">CAD - Canadian Dollar</option>
              <option value="INR">INR - Indian Rupee</option>
            </select>
          </div>

          <div>
            <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
              To (Target Currency)
            </label>
            <select
              className="finmate-select"
              value={targetCurrency}
              onChange={e => setTargetCurrency(e.target.value)}
            >
              <option value="INR">INR - Indian Rupee</option>
              <option value="USD">USD - US Dollar</option>
              <option value="EUR">EUR - Euro</option>
              <option value="GBP">GBP - British Pound</option>
            </select>
          </div>

          <div
            style={{
              padding: '0.75rem 1rem',
              backgroundColor: 'var(--bg-tertiary)',
              borderRadius: 'var(--radius-md)',
              border: '1px solid var(--border-color)',
              display: 'flex',
              flexDirection: 'column'
            }}
          >
            <span style={{ fontSize: '0.6875rem', color: 'var(--text-muted)', textTransform: 'uppercase' }}>
              Converted Value
            </span>
            <span className="tabular-nums" style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--primary-600)' }}>
              {targetCurrency === 'INR' ? '₹' : ''}{convertedValue} {targetCurrency}
            </span>
          </div>
        </div>

        {exchangeRateData && (
          <div style={{ marginTop: '1rem', paddingTop: '0.75rem', borderTop: '1px solid var(--border-color)', display: 'flex', justifyContent: 'space-between', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
            <span>Rate: 1 {exchangeRateData.baseCurrency} = {exchangeRateData.rate} {exchangeRateData.targetCurrency}</span>
            <span>Client: <strong>{exchangeRateData.clientUsed}</strong> | As of: {exchangeRateData.asOf}</span>
          </div>
        )}
      </div>
    </div>
  );
}
