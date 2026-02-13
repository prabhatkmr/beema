'use client';

import { useState, useEffect, useCallback } from 'react';
import { Plus } from 'lucide-react';
import { Button } from '@beema/ui';
import { PageHeader } from '@/components/PageHeader';
import { DataTable } from '@/components/DataTable';
import { StatusBadge } from '@/components/StatusBadge';
import { Modal } from '@/components/Modal';
import type { Region, RegionRequest } from '@/types/admin';
import * as api from '@/lib/api';

export default function RegionsPage() {
  const [regions, setRegions] = useState<Region[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
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
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load regions');
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

      {error && (
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
          onRowClick={openEdit}
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
