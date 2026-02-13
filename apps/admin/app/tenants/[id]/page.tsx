'use client';

import { useState, useEffect } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { Button } from '@beema/ui';
import { PageHeader } from '@/components/PageHeader';
import { StatusBadge } from '@/components/StatusBadge';
import type { Tenant, Region } from '@/types/admin';
import * as api from '@/lib/api';

export default function TenantDetailPage() {
  const router = useRouter();
  const params = useParams();
  const id = params.id as string;

  const [tenant, setTenant] = useState<Tenant | null>(null);
  const [regions, setRegions] = useState<Region[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [editing, setEditing] = useState(false);

  const [form, setForm] = useState({
    tenantId: '',
    name: '',
    slug: '',
    tier: '',
    regionCode: '',
    contactEmail: '',
  });

  useEffect(() => {
    async function load() {
      try {
        const [t, r] = await Promise.all([api.getTenant(id), api.listRegions()]);
        setTenant(t);
        setRegions(r);
        setForm({
          tenantId: t.tenantId,
          name: t.name,
          slug: t.slug,
          tier: t.tier,
          regionCode: t.regionCode,
          contactEmail: t.contactEmail || '',
        });
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load tenant');
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [id]);

  const handleSave = async () => {
    setSaving(true);
    setError(null);
    try {
      const updated = await api.updateTenant(id, { ...form, createdBy: 'admin' });
      setTenant(updated);
      setEditing(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update tenant');
    } finally {
      setSaving(false);
    }
  };

  const handleStatusAction = async (action: 'activate' | 'suspend' | 'deactivate') => {
    try {
      let updated: Tenant;
      if (action === 'activate') updated = await api.activateTenant(id);
      else if (action === 'suspend') updated = await api.suspendTenant(id);
      else updated = await api.deactivateTenant(id);
      setTenant(updated);
    } catch (err) {
      setError(err instanceof Error ? err.message : `Failed to ${action} tenant`);
    }
  };

  if (loading) {
    return (
      <div className="p-8 flex items-center justify-center">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600" />
      </div>
    );
  }

  if (!tenant) {
    return (
      <div className="p-8">
        <p className="text-gray-500">Tenant not found.</p>
      </div>
    );
  }

  return (
    <div className="p-8 max-w-3xl">
      <PageHeader
        title={tenant.name}
        description={`Tenant ID: ${tenant.tenantId}`}
        action={
          <div className="flex items-center space-x-2">
            <StatusBadge status={tenant.status} />
          </div>
        }
      />

      {error && (
        <div className="mb-6 px-4 py-3 rounded-md bg-red-50 border border-red-200">
          <p className="text-sm text-red-700">{error}</p>
        </div>
      )}

      {/* Status Actions */}
      <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-6 mb-6">
        <h3 className="text-sm font-semibold text-gray-900 mb-3">Status Actions</h3>
        <div className="flex items-center space-x-3">
          {tenant.status !== 'ACTIVE' && (
            <button
              onClick={() => handleStatusAction('activate')}
              className="px-3 py-1.5 text-sm bg-green-50 text-green-700 border border-green-200 rounded-lg hover:bg-green-100"
            >
              Activate
            </button>
          )}
          {tenant.status !== 'SUSPENDED' && tenant.status !== 'DEACTIVATED' && (
            <button
              onClick={() => handleStatusAction('suspend')}
              className="px-3 py-1.5 text-sm bg-yellow-50 text-yellow-700 border border-yellow-200 rounded-lg hover:bg-yellow-100"
            >
              Suspend
            </button>
          )}
          {tenant.status !== 'DEACTIVATED' && (
            <button
              onClick={() => handleStatusAction('deactivate')}
              className="px-3 py-1.5 text-sm bg-red-50 text-red-700 border border-red-200 rounded-lg hover:bg-red-100"
            >
              Deactivate
            </button>
          )}
        </div>
      </div>

      {/* Tenant Details */}
      <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-sm font-semibold text-gray-900">Details</h3>
          {!editing ? (
            <button
              onClick={() => setEditing(true)}
              className="text-sm text-blue-600 hover:text-blue-800"
            >
              Edit
            </button>
          ) : (
            <div className="flex items-center space-x-2">
              <button
                onClick={() => setEditing(false)}
                className="text-sm text-gray-500 hover:text-gray-700"
              >
                Cancel
              </button>
              <Button variant="primary" onClick={handleSave} disabled={saving}>
                {saving ? 'Saving...' : 'Save'}
              </Button>
            </div>
          )}
        </div>

        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Tenant ID</label>
              <p className="text-sm text-gray-900 bg-gray-50 px-3 py-2 rounded">{tenant.tenantId}</p>
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Slug</label>
              <p className="text-sm text-gray-900 bg-gray-50 px-3 py-2 rounded">{tenant.slug}</p>
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-gray-500 mb-1">Name</label>
            {editing ? (
              <input
                type="text"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            ) : (
              <p className="text-sm text-gray-900">{tenant.name}</p>
            )}
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Tier</label>
              {editing ? (
                <select
                  value={form.tier}
                  onChange={(e) => setForm({ ...form, tier: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="STANDARD">Standard</option>
                  <option value="PREMIUM">Premium</option>
                  <option value="ENTERPRISE">Enterprise</option>
                </select>
              ) : (
                <StatusBadge status={tenant.tier} />
              )}
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Region</label>
              {editing ? (
                <select
                  value={form.regionCode}
                  onChange={(e) => setForm({ ...form, regionCode: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  {regions.map((r) => (
                    <option key={r.code} value={r.code}>{r.name} ({r.code})</option>
                  ))}
                </select>
              ) : (
                <p className="text-sm text-gray-900">{tenant.regionCode}</p>
              )}
            </div>
          </div>

          <div>
            <label className="block text-xs font-medium text-gray-500 mb-1">Contact Email</label>
            {editing ? (
              <input
                type="email"
                value={form.contactEmail}
                onChange={(e) => setForm({ ...form, contactEmail: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            ) : (
              <p className="text-sm text-gray-900">{tenant.contactEmail || '-'}</p>
            )}
          </div>

          {tenant.datasourceKey && (
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Datasource</label>
              <p className="text-sm text-gray-900">{tenant.datasourceKey}</p>
            </div>
          )}

          <div className="grid grid-cols-2 gap-4 pt-4 border-t border-gray-100">
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Created</label>
              <p className="text-xs text-gray-500">{new Date(tenant.createdAt).toLocaleString()}</p>
            </div>
            <div>
              <label className="block text-xs font-medium text-gray-500 mb-1">Updated</label>
              <p className="text-xs text-gray-500">{new Date(tenant.updatedAt).toLocaleString()}</p>
            </div>
          </div>
        </div>
      </div>

      {/* Back link */}
      <div className="mt-6">
        <button
          onClick={() => router.push('/tenants')}
          className="text-sm text-blue-600 hover:text-blue-800"
        >
          &larr; Back to Tenants
        </button>
      </div>
    </div>
  );
}
