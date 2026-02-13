'use client';

import { useState, useEffect, useCallback } from 'react';
import { useRouter } from 'next/navigation';
import { Plus } from 'lucide-react';
import { Button } from '@beema/ui';
import { PageHeader } from '@/components/PageHeader';
import { DataTable } from '@/components/DataTable';
import { StatusBadge } from '@/components/StatusBadge';
import type { Tenant } from '@/types/admin';
import * as api from '@/lib/api';

export default function TenantsPage() {
  const router = useRouter();
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<string>('');

  const fetchTenants = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.listTenants(statusFilter || undefined);
      setTenants(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load tenants');
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

      {error && (
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
