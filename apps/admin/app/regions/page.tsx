'use client';

import { useState, useEffect, useCallback } from 'react';
import { Plus, WifiOff, RefreshCw } from 'lucide-react';
import { Button } from '@beema/ui';
import { PageHeader } from '@/components/PageHeader';
import { DataTable } from '@/components/DataTable';
import { StatusBadge } from '@/components/StatusBadge';
import { Modal } from '@/components/Modal';
import type { Region, RegionRequest } from '@/types/admin';
import * as api from '@/lib/api';

const DEMO_REGIONS: Region[] = [
  { id: 'demo-1', code: 'US', name: 'United States', description: 'US data residency zone', dataResidencyRules: { gdprCompliant: false, dataRetentionDays: 2555 }, isActive: true, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
  { id: 'demo-2', code: 'EU', name: 'European Union', description: 'EU data residency zone (GDPR)', dataResidencyRules: { gdprCompliant: true, dataRetentionDays: 1825, encryptionRequired: true }, isActive: true, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
  { id: 'demo-3', code: 'UK', name: 'United Kingdom', description: 'UK data residency zone', dataResidencyRules: { gdprCompliant: true, dataRetentionDays: 2190 }, isActive: true, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
  { id: 'demo-4', code: 'APAC', name: 'Asia Pacific', description: 'APAC data residency zone', dataResidencyRules: { gdprCompliant: false, dataRetentionDays: 2555 }, isActive: true, createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z' },
];

export default function RegionsPage() {
  const [regions, setRegions] = useState<Region[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isDemo, setIsDemo] = useState(false);
  const [showModal, setShowModal] = useState(false);
  const [saving, setSaving] = useState(false);
  const [editingRegion, setEditingRegion] = useState<Region | null>(null);

  const [form, setForm] = useState<RegionRequest>({
    code: '',
    name: '',
    description: '',
  });

  const fetchRegions = useCallback(async () => {
    setLoading(true);
    try {
      const data = await api.listRegions();
      setRegions(Array.isArray(data) ? data : []);
      setIsDemo(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load regions');
      setRegions(DEMO_REGIONS);
      setIsDemo(true);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchRegions();
  }, [fetchRegions]);

  const handleSave = async () => {
    setSaving(true);
    setError(null);
    try {
      if (editingRegion) {
        await api.updateRegion(editingRegion.id, form);
      } else {
        await api.createRegion(form);
      }
      setShowModal(false);
      setEditingRegion(null);
      setForm({ code: '', name: '', description: '' });
      await fetchRegions();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save region');
    } finally {
      setSaving(false);
    }
  };

  const openCreate = () => {
    setEditingRegion(null);
    setForm({ code: '', name: '', description: '' });
    setShowModal(true);
  };

  const openEdit = (region: Region) => {
    setEditingRegion(region);
    setForm({
      code: region.code,
      name: region.name,
      description: region.description || '',
      isActive: region.isActive,
    });
    setShowModal(true);
  };

  const columns = [
    { header: 'Code', accessor: 'code' as keyof Region },
    { header: 'Name', accessor: 'name' as keyof Region },
    { header: 'Description', accessor: 'description' as keyof Region },
    {
      header: 'Status',
      accessor: (row: Region) => (
        <StatusBadge status={row.isActive ? 'ACTIVE' : 'INACTIVE'} />
      ),
    },
    {
      header: 'Residency Rules',
      accessor: (row: Region) => (
        <span className="text-xs text-gray-500">
          {Object.keys(row.dataResidencyRules || {}).length} rules
        </span>
      ),
    },
  ];

  return (
    <div className="p-8">
      <PageHeader
        title="Regions"
        description="Manage data residency regions and compliance rules"
        action={
          <Button variant="primary" onClick={openCreate}>
            <Plus className="w-4 h-4 mr-2" />
            New Region
          </Button>
        }
      />

      {isDemo && (
        <div className="mb-4 px-4 py-3 rounded-md bg-amber-50 border border-amber-200">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <WifiOff className="w-4 h-4 text-amber-600" />
              <p className="text-sm text-amber-700">Backend unavailable — showing demo data.</p>
            </div>
            <button
              onClick={fetchRegions}
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
            <button onClick={() => setError(null)} className="text-red-400 hover:text-red-600 text-sm">Dismiss</button>
          </div>
        </div>
      )}

      <div className="bg-white rounded-lg border border-gray-200 shadow-sm">
        <DataTable
          columns={columns}
          data={regions}
          loading={loading}
          emptyMessage="No regions found"
          onRowClick={isDemo ? undefined : openEdit}
        />
      </div>

      {showModal && (
        <Modal
          title={editingRegion ? 'Edit Region' : 'New Region'}
          onClose={() => { setShowModal(false); setEditingRegion(null); }}
          footer={
            <>
              <button
                onClick={() => { setShowModal(false); setEditingRegion(null); }}
                className="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50"
              >
                Cancel
              </button>
              <Button variant="primary" onClick={handleSave} disabled={saving}>
                {saving ? 'Saving...' : editingRegion ? 'Update' : 'Create'}
              </Button>
            </>
          }
        >
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Code</label>
              <input
                type="text"
                required
                placeholder="e.g., US, EU, APAC"
                value={form.code}
                disabled={!!editingRegion}
                onChange={(e) => setForm({ ...form, code: e.target.value.toUpperCase() })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Name</label>
              <input
                type="text"
                required
                placeholder="e.g., United States"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Description</label>
              <textarea
                placeholder="Description of the region..."
                value={form.description || ''}
                onChange={(e) => setForm({ ...form, description: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                rows={2}
              />
            </div>
            {editingRegion && (
              <div className="flex items-center space-x-2">
                <input
                  type="checkbox"
                  id="isActive"
                  checked={form.isActive !== false}
                  onChange={(e) => setForm({ ...form, isActive: e.target.checked })}
                  className="rounded border-gray-300"
                />
                <label htmlFor="isActive" className="text-sm text-gray-700">Active</label>
              </div>
            )}
          </div>
        </Modal>
      )}
    </div>
  );
}
