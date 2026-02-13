'use client';

import { useState, useEffect, useCallback } from 'react';
import { Plus } from 'lucide-react';
import { Button } from '@beema/ui';
import { PageHeader } from '@/components/PageHeader';
import { DataTable } from '@/components/DataTable';
import { StatusBadge } from '@/components/StatusBadge';
import { Modal } from '@/components/Modal';
import type { DatasourceConfig, DatasourceRequest } from '@/types/admin';
import * as api from '@/lib/api';

export default function DatasourcesPage() {
  const [datasources, setDatasources] = useState<DatasourceConfig[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showModal, setShowModal] = useState(false);
  const [saving, setSaving] = useState(false);
  const [editingDs, setEditingDs] = useState<DatasourceConfig | null>(null);

  const [form, setForm] = useState<DatasourceRequest>({
    name: '',
    url: '',
    username: '',
    poolSize: 20,
  });

  const fetchDatasources = useCallback(async () => {
    setLoading(true);
    try {
      const data = await api.listDatasources();
      setDatasources(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load datasources');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDatasources();
  }, [fetchDatasources]);

  const handleSave = async () => {
    setSaving(true);
    setError(null);
    try {
      if (editingDs) {
        await api.updateDatasource(editingDs.id, form);
      } else {
        await api.createDatasource(form);
      }
      setShowModal(false);
      setEditingDs(null);
      setForm({ name: '', url: '', username: '', poolSize: 20 });
      await fetchDatasources();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to save datasource');
    } finally {
      setSaving(false);
    }
  };

  const openCreate = () => {
    setEditingDs(null);
    setForm({ name: '', url: '', username: '', poolSize: 20 });
    setShowModal(true);
  };

  const openEdit = (ds: DatasourceConfig) => {
    setEditingDs(ds);
    setForm({
      name: ds.name,
      url: ds.url,
      username: ds.username,
      poolSize: ds.poolSize,
      status: ds.status,
    });
    setShowModal(true);
  };

  const columns = [
    { header: 'Name', accessor: 'name' as keyof DatasourceConfig },
    {
      header: 'URL',
      accessor: (row: DatasourceConfig) => (
        <span className="text-xs font-mono text-gray-600 truncate max-w-xs block">{row.url}</span>
      ),
    },
    { header: 'Username', accessor: 'username' as keyof DatasourceConfig },
    { header: 'Pool Size', accessor: 'poolSize' as keyof DatasourceConfig },
    {
      header: 'Status',
      accessor: (row: DatasourceConfig) => <StatusBadge status={row.status} />,
    },
  ];

  return (
    <div className="p-8">
      <PageHeader
        title="Datasources"
        description="Manage database connection pools for cell-based routing"
        action={
          <Button variant="primary" onClick={openCreate}>
            <Plus className="w-4 h-4 mr-2" />
            New Datasource
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
          data={datasources}
          loading={loading}
          emptyMessage="No datasources configured"
          onRowClick={openEdit}
        />
      </div>

      {showModal && (
        <Modal
          title={editingDs ? 'Edit Datasource' : 'New Datasource'}
          onClose={() => { setShowModal(false); setEditingDs(null); }}
          footer={
            <>
              <button
                onClick={() => { setShowModal(false); setEditingDs(null); }}
                className="px-4 py-2 text-sm text-gray-700 border border-gray-300 rounded-lg hover:bg-gray-50"
              >
                Cancel
              </button>
              <Button variant="primary" onClick={handleSave} disabled={saving}>
                {saving ? 'Saving...' : editingDs ? 'Update' : 'Create'}
              </Button>
            </>
          }
        >
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">Name</label>
              <input
                type="text"
                required
                placeholder="e.g., master, tenant-vip-1"
                value={form.name}
                disabled={!!editingDs}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:bg-gray-100"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">JDBC URL</label>
              <input
                type="text"
                required
                placeholder="jdbc:postgresql://host:5432/dbname"
                value={form.url}
                onChange={(e) => setForm({ ...form, url: e.target.value })}
                className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm font-mono focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Username</label>
                <input
                  type="text"
                  required
                  placeholder="db_user"
                  value={form.username}
                  onChange={(e) => setForm({ ...form, username: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Pool Size</label>
                <input
                  type="number"
                  min={1}
                  max={100}
                  value={form.poolSize || 20}
                  onChange={(e) => setForm({ ...form, poolSize: parseInt(e.target.value) || 20 })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
              </div>
            </div>
            {editingDs && (
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Status</label>
                <select
                  value={form.status || 'ACTIVE'}
                  onChange={(e) => setForm({ ...form, status: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
                >
                  <option value="ACTIVE">Active</option>
                  <option value="INACTIVE">Inactive</option>
                  <option value="MAINTENANCE">Maintenance</option>
                </select>
              </div>
            )}
          </div>
        </Modal>
      )}
    </div>
  );
}
