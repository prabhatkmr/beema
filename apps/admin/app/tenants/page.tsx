'use client';

import { useState, useEffect, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { Plus, WifiOff, RefreshCw } from 'lucide-react';
import { Button } from '@beema/ui';
import { PageHeader } from '@/components/PageHeader';
import { DataTable } from '@/components/DataTable';
import { StatusBadge } from '@/components/StatusBadge';
import type { Tenant } from '@/types/admin';
import * as api from '@/lib/api';

const DEMO_TENANTS: Tenant[] = [
  { id: 'demo-1', tenantId: 'acme-insurance', name: 'Acme Insurance Corp', slug: 'acme', status: 'ACTIVE', tier: 'ENTERPRISE', regionCode: 'US', contactEmail: 'admin@acme-ins.com', config: {}, createdBy: 'system', updatedBy: 'system', createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-15T00:00:00Z' },
  { id: 'demo-2', tenantId: 'brit-mutual', name: 'Brit Mutual Ltd', slug: 'brit-mutual', status: 'ACTIVE', tier: 'PREMIUM', regionCode: 'UK', contactEmail: 'ops@britmutual.co.uk', config: {}, createdBy: 'system', updatedBy: 'system', createdAt: '2026-01-05T00:00:00Z', updatedAt: '2026-01-20T00:00:00Z' },
  { id: 'demo-3', tenantId: 'nordic-re', name: 'Nordic Reinsurance', slug: 'nordic-re', status: 'ACTIVE', tier: 'ENTERPRISE', regionCode: 'EU', contactEmail: 'tech@nordic-re.eu', config: {}, createdBy: 'system', updatedBy: 'system', createdAt: '2026-01-10T00:00:00Z', updatedAt: '2026-02-01T00:00:00Z' },
  { id: 'demo-4', tenantId: 'pacific-gen', name: 'Pacific General', slug: 'pacific-gen', status: 'SUSPENDED', tier: 'STANDARD', regionCode: 'APAC', contactEmail: 'support@pacgen.com.au', config: {}, createdBy: 'system', updatedBy: 'system', createdAt: '2026-01-12T00:00:00Z', updatedAt: '2026-02-05T00:00:00Z' },
  { id: 'demo-5', tenantId: 'metro-life', name: 'Metro Life Insurance', slug: 'metro-life', status: 'ACTIVE', tier: 'STANDARD', regionCode: 'US', contactEmail: 'it@metrolife.com', config: {}, createdBy: 'system', updatedBy: 'system', createdAt: '2026-01-15T00:00:00Z', updatedAt: '2026-02-10T00:00:00Z' },
];

export default function TenantsPage() {
  const router = useRouter();
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isDemo, setIsDemo] = useState(false);
  const [statusFilter, setStatusFilter] = useState<string>('');

  const fetchTenants = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.listTenants(statusFilter || undefined);
      setTenants(Array.isArray(data) ? data : []);
      setIsDemo(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load tenants');
      setTenants(DEMO_TENANTS);
      setIsDemo(true);
    } finally {
      setLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => {
    fetchTenants();
  }, [fetchTenants]);

  const columns = [
    { header: 'Tenant ID', accessor: 'tenantId' as keyof Tenant },
    { header: 'Name', accessor: 'name' as keyof Tenant },
    {
      header: 'Status',
      accessor: (row: Tenant) => <StatusBadge status={row.status} />,
    },
    {
      header: 'Tier',
      accessor: (row: Tenant) => <StatusBadge status={row.tier} />,
    },
    { header: 'Region', accessor: 'regionCode' as keyof Tenant },
    { header: 'Contact', accessor: 'contactEmail' as keyof Tenant },
  ];

  return (
    <div className="p-8">
      <PageHeader
        title="Tenants"
        description="Manage platform tenants and their configurations"
        action={
          <Button variant="primary" onClick={() => router.push('/tenants/new')}>
            <Plus className="w-4 h-4 mr-2" />
            New Tenant
          </Button>
        }
      />

      {/* Filters */}
      <div className="mb-6 flex items-center space-x-4">
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        >
          <option value="">All Statuses</option>
          <option value="ACTIVE">Active</option>
          <option value="SUSPENDED">Suspended</option>
          <option value="PROVISIONING">Provisioning</option>
          <option value="DEACTIVATED">Deactivated</option>
        </select>
      </div>

      {isDemo && (
        <div className="mb-4 px-4 py-3 rounded-md bg-amber-50 border border-amber-200">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <WifiOff className="w-4 h-4 text-amber-600" />
              <p className="text-sm text-amber-700">Backend unavailable — showing demo data.</p>
            </div>
            <button
              onClick={fetchTenants}
              className="flex items-center gap-1 text-xs font-medium text-amber-700 hover:text-amber-900 px-2 py-1 rounded hover:bg-amber-100 transition-colors"
            >
              <RefreshCw className="w-3 h-3" />
              Retry
            </button>
          </div>
        </div>
      )}

      {error && !isDemo && (
        <div className="mb-4 px-4 py-3 rounded-md bg-red-50 border border-red-200">
          <div className="flex items-center justify-between">
            <p className="text-sm text-red-700">{error}</p>
            <button onClick={() => setError(null)} className="text-red-400 hover:text-red-600 text-sm">
              Dismiss
            </button>
          </div>
        </div>
      )}

      <div className="bg-white rounded-lg border border-gray-200 shadow-sm">
        <DataTable
          columns={columns}
          data={tenants}
          loading={loading}
          emptyMessage="No tenants found. Create one to get started."
          onRowClick={(tenant) => router.push(`/tenants/${tenant.id}`)}
        />
      </div>
    </div>
  );
}
