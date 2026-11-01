import React, { useState, useEffect } from 'react';
import { adminApi } from '../../api/adminApi';
import LoadingSpinner from '../../components/common/LoadingSpinner';
import { FolderTree, Plus, Check, X, AlertCircle } from 'lucide-react';

export default function AdminCategoriesPage() {
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(true);

  // New category form modal state
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [name, setName] = useState('');
  const [type, setType] = useState('EXPENSE');
  const [color, setColor] = useState('#4f46e5');
  const [icon, setIcon] = useState('Tag');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const fetchCategories = async () => {
    setLoading(true);
    try {
      const resp = await adminApi.getAdminCategories();
      setCategories(resp.data);
    } catch (err) {
      console.error('Failed to load system categories', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCategories();
  }, []);

  const handleToggle = async (catId, currentActive) => {
    try {
      await adminApi.toggleCategoryStatus(catId, !currentActive);
      fetchCategories();
    } catch (err) {
      alert('Failed to toggle category status');
    }
  };

  const handleCreate = async e => {
    e.preventDefault();
    setError('');
    setSaving(true);

    try {
      await adminApi.createSystemCategory({
        name: name.trim(),
        type,
        colorHex: color,
        icon
      });
      setName('');
      setIsModalOpen(false);
      fetchCategories();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create system category');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header */}
      <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: '1rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.25rem' }}>
            <span className="status-pill warning">Admin Console</span>
          </div>
          <h1 style={{ fontSize: '1.625rem', fontWeight: 700, color: 'var(--text-primary)', letterSpacing: '-0.02em' }}>
            System Taxonomy & Categories
          </h1>
          <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)' }}>
            Configure default global categories available to all registered users across the platform
          </p>
        </div>

        <button onClick={() => setIsModalOpen(true)} className="btn-finmate-primary">
          <Plus size={16} />
          <span>New System Category</span>
        </button>
      </div>

      {/* Categories Table */}
      <div className="finmate-card" style={{ overflow: 'hidden' }}>
        {loading ? (
          <div style={{ padding: '3rem 0' }}>
            <LoadingSpinner message="Loading taxonomy..." />
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table className="finmate-table">
              <thead>
                <tr>
                  <th>Color</th>
                  <th>Category Name</th>
                  <th>Flow Type</th>
                  <th>Icon Identifier</th>
                  <th>System Preset</th>
                  <th style={{ textAlign: 'center' }}>Active Status</th>
                </tr>
              </thead>
              <tbody>
                {categories.map(c => (
                  <tr key={c.id}>
                    <td style={{ width: 40 }}>
                      <div
                        style={{
                          width: 20,
                          height: 20,
                          borderRadius: 4,
                          backgroundColor: c.colorHex || '#64748b'
                        }}
                      />
                    </td>
                    <td style={{ fontWeight: 600 }}>{c.name}</td>
                    <td>
                      <span className={`status-pill ${c.type === 'INCOME' ? 'success' : 'danger'}`}>
                        {c.type}
                      </span>
                    </td>
                    <td style={{ color: 'var(--text-secondary)' }}>{c.icon || '-'}</td>
                    <td>
                      <span className="status-pill neutral">
                        {c.isSystem ? 'Global Default' : 'Custom User'}
                      </span>
                    </td>
                    <td style={{ textAlign: 'center' }}>
                      <button
                        onClick={() => handleToggle(c.id, c.isActive)}
                        className={`status-pill ${c.isActive ? 'success' : 'neutral'}`}
                        style={{ cursor: 'pointer', border: 'none' }}
                      >
                        {c.isActive ? 'Enabled' : 'Disabled'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal */}
      {isModalOpen && (
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
          <div className="finmate-card" style={{ width: '100%', maxWidth: 440 }}>
            <div className="finmate-card-header">
              <h3 style={{ fontSize: '1.0625rem', fontWeight: 600, margin: 0 }}>
                Create System Category
              </h3>
              <button onClick={() => setIsModalOpen(false)} style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer' }}>
                <X size={20} />
              </button>
            </div>

            <div style={{ padding: '1.5rem' }}>
              {error && (
                <div style={{ padding: '0.75rem 1rem', backgroundColor: 'var(--danger-bg)', color: 'var(--danger-text)', borderRadius: 'var(--radius-md)', fontSize: '0.875rem', marginBottom: '1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <AlertCircle size={18} /> {error}
                </div>
              )}

              <form onSubmit={handleCreate} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                <div>
                  <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                    Category Name *
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="e.g. Subscriptions, Pet Care"
                    className="finmate-input"
                    value={name}
                    onChange={e => setName(e.target.value)}
                  />
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
                  <div>
                    <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                      Flow Type *
                    </label>
                    <select
                      className="finmate-select"
                      value={type}
                      onChange={e => setType(e.target.value)}
                    >
                      <option value="EXPENSE">Expense</option>
                      <option value="INCOME">Income</option>
                    </select>
                  </div>

                  <div>
                    <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                      Accent Color
                    </label>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <input
                        type="color"
                        value={color}
                        onChange={e => setColor(e.target.value)}
                        style={{ width: 40, height: 36, border: 'none', background: 'none', cursor: 'pointer' }}
                      />
                      <input
                        type="text"
                        className="finmate-input"
                        value={color}
                        onChange={e => setColor(e.target.value)}
                        style={{ flex: 1 }}
                      />
                    </div>
                  </div>
                </div>

                <div>
                  <label style={{ display: 'block', fontSize: '0.8125rem', fontWeight: 600, marginBottom: '0.375rem' }}>
                    Icon Name (Lucide)
                  </label>
                  <input
                    type="text"
                    placeholder="e.g. ShoppingBag, Briefcase, Film"
                    className="finmate-input"
                    value={icon}
                    onChange={e => setIcon(e.target.value)}
                  />
                </div>

                <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
                  <button type="button" onClick={() => setIsModalOpen(false)} className="btn-finmate-secondary">
                    Cancel
                  </button>
                  <button type="submit" disabled={saving} className="btn-finmate-primary">
                    {saving ? 'Creating...' : 'Create Category'}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
