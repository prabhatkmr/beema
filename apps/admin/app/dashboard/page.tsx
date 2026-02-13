'use client';

import { useState, useEffect } from 'react';
import Link from 'next/link';
import { Users, Globe, Database, Activity, Plus, ArrowRight } from 'lucide-react';
import { PageHeader } from '@/components/PageHeader';
import { StatCard } from '@/components/StatCard';
import type { DashboardStats } from '@/types/admin';
import * as api from '@/lib/api';

export default function DashboardPage() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function fetchStats() {
      try {
        const data = await api.getDashboardStats();
        setStats(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load stats');
      } finally {
        setLoading(false);
      }
    }
    fetchStats();
  }, []);

  return (
    <div className="p-8">
      <PageHeader
        title="Dashboard"
        description="Platform overview and system health"
      />

      {error && (
        <div className="mb-6 px-4 py-3 rounded-md bg-red-50 border border-red-200">
          <p className="text-sm text-red-700">{error}</p>
        </div>
      )}

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <StatCard
          title="Active Tenants"
          value={loading ? '...' : stats?.activeTenants ?? 0}
          subtitle={loading ? '' : `${stats?.totalTenants ?? 0} total`}
          icon={<Users className="w-5 h-5" />}
          color="blue"
        />
        <StatCard
          title="Suspended"
          value={loading ? '...' : stats?.suspendedTenants ?? 0}
          subtitle="Tenants paused"
          icon={<Users className="w-5 h-5" />}
          color="amber"
        />
        <StatCard
          title="Active Regions"
          value={loading ? '...' : stats?.activeRegions ?? 0}
          subtitle={loading ? '' : `${stats?.totalRegions ?? 0} total`}
          icon={<Globe className="w-5 h-5" />}
          color="green"
        />
        <StatCard
          title="Datasources"
          value={loading ? '...' : stats?.totalDatasources ?? 0}
          subtitle="Connection pools"
          icon={<Database className="w-5 h-5" />}
          color="purple"
        />
      </div>

      {/* Tier Breakdown */}
      {stats?.tierBreakdown && (
        <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-6 mb-8">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Tenant Tiers</h3>
          <div className="grid grid-cols-3 gap-4">
            {Object.entries(stats.tierBreakdown).map(([tier, count]) => (
              <div key={tier} className="text-center p-4 bg-gray-50 rounded-lg">
                <p className="text-2xl font-bold text-gray-900">{count}</p>
                <p className="text-sm text-gray-500">{tier}</p>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Quick Actions */}
      <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-6 mb-8">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Quick Actions</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <Link
            href="/tenants/new"
            className="flex items-center space-x-3 p-4 border border-gray-200 rounded-lg hover:border-blue-500 hover:bg-blue-50 transition-colors"
          >
            <Plus className="w-5 h-5 text-blue-600" />
            <span className="text-sm font-medium text-gray-900">New Tenant</span>
          </Link>
          <Link
            href="/tenants"
            className="flex items-center space-x-3 p-4 border border-gray-200 rounded-lg hover:border-blue-500 hover:bg-blue-50 transition-colors"
          >
            <ArrowRight className="w-5 h-5 text-blue-600" />
            <span className="text-sm font-medium text-gray-900">Manage Tenants</span>
          </Link>
          <Link
            href="/health"
            className="flex items-center space-x-3 p-4 border border-gray-200 rounded-lg hover:border-green-500 hover:bg-green-50 transition-colors"
          >
            <Activity className="w-5 h-5 text-green-600" />
            <span className="text-sm font-medium text-gray-900">System Health</span>
          </Link>
        </div>
      </div>

      {/* Infrastructure Links */}
      <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Infrastructure</h3>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
          {[
            { name: 'Grafana', url: 'http://localhost:3002', desc: 'Dashboards' },
            { name: 'Jaeger', url: 'http://localhost:16686', desc: 'Tracing' },
            { name: 'Temporal', url: 'http://localhost:8088', desc: 'Workflows' },
            { name: 'Prometheus', url: 'http://localhost:9090', desc: 'Metrics' },
            { name: 'Swagger', url: 'http://localhost:8080/swagger-ui.html', desc: 'Kernel API' },
            { name: 'Keycloak', url: 'http://localhost:8180', desc: 'Auth' },
            { name: 'MinIO', url: 'http://localhost:9001', desc: 'Storage' },
            { name: 'Flink', url: 'http://localhost:8081', desc: 'Streaming' },
          ].map((svc) => (
            <a
              key={svc.name}
              href={svc.url}
              target="_blank"
              rel="noopener noreferrer"
              className="p-3 border border-gray-200 rounded-lg hover:border-blue-400 hover:shadow-sm transition-all text-center"
            >
              <p className="text-sm font-medium text-gray-900">{svc.name}</p>
              <p className="text-xs text-gray-500">{svc.desc}</p>
            </a>
          ))}
        </div>
      </div>
    </div>
  );
}
