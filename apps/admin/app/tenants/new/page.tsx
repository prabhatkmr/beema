'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Button } from '@beema/ui';
import { PageHeader } from '@/components/PageHeader';
import type { Region } from '@/types/admin';
import * as api from '@/lib/api';

export default function NewTenantPage() {
  const router = useRouter();
  const [regions, setRegions] = useState<Region[]>([]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [form, setForm] = useState({
    tenantId: '',
    name: '',
    slug: '',
    tier: 'STANDARD',
    regionCode: 'US',
    contactEmail: '',
  });

  useEffect(() => {
    api.listRegions().then(setRegions).catch(() => {});
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);

    try {
      await api.createTenant({
        ...form,
        createdBy: 'admin',
      });
      router.push('/tenants');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create tenant');
    } finally {
      setSaving(false);
    }
  };

  const handleSlugify = () => {
    if (!form.slug && form.name) {
      setForm({
        ...form,
        slug: form.name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, ''),
      });
    }
  };

  return (
    <div className="p-8 max-w-2xl">
      <PageHeader
        title="New Tenant"
        description="Register a new tenant on the platform"
      />

      {error && (
        <div className="mb-6 px-4 py-3 rounded-md bg-red-50 border border-red-200">
          <p className="text-sm text-red-700">{error}</p>
        </div>
      )}

      <form onSubmit={handleSubmit} className="bg-white rounded-lg border border-gray-200 shadow-sm p-6 space-y-5">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Tenant ID</label>
          <input
            type="text"
            required
            placeholder="e.g., acme-insurance"
            value={form.tenantId}
            onChange={(e) => setForm({ ...form, tenantId: e.target.value })}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <p className="text-xs text-gray-500 mt-1">Used in X-Tenant-ID header for API requests</p>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Name</label>
          <input
            type="text"
            required
            placeholder="e.g., Acme Insurance Corp"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            onBlur={handleSlugify}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Slug</label>
          <input
            type="text"
            required
            placeholder="e.g., acme-insurance"
            value={form.slug}
            onChange={(e) => setForm({ ...form, slug: e.target.value })}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <p className="text-xs text-gray-500 mt-1">URL-safe unique identifier</p>
        </div>

        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Tier</label>
            <select
              value={form.tier}
              onChange={(e) => setForm({ ...form, tier: e.target.value })}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              <option value="STANDARD">Standard</option>
              <option value="PREMIUM">Premium</option>
              <option value="ENTERPRISE">Enterprise</option>
            </select>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Region</label>
            <select
              value={form.regionCode}
              onChange={(e) => setForm({ ...form, regionCode: e.target.value })}
              className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              {regions.length > 0 ? (
                regions.map((r) => (
                  <option key={r.code} value={r.code}>{r.name} ({r.code})</option>
                ))
              ) : (
                <>
                  <option value="US">United States (US)</option>
                  <option value="EU">European Union (EU)</option>
                  <option value="UK">United Kingdom (UK)</option>
                  <option value="APAC">Asia Pacific (APAC)</option>
                </>
              )}
            </select>
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Contact Email</label>
          <input
            type="email"
            placeholder="admin@acme-insurance.com"
            value={form.contactEmail}
            onChange={(e) => setForm({ ...form, contactEmail: e.target.value })}
            className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
        </div>

        <div className="flex items-center justify-end space-x-3 pt-4 border-t border-gray-200">
          <button
            type="button"
            onClick={() => router.push('/tenants')}
            className="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50"
          >
            Cancel
          </button>
          <Button type="submit" variant="primary" disabled={saving}>
            {saving ? 'Creating...' : 'Create Tenant'}
          </Button>
        </div>
      </form>
    </div>
  );
}
