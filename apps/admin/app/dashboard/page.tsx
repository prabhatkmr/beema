'use client';

import { useState, useEffect, useCallback } from 'react';
import Link from 'next/link';
import { Users, Globe, Database, Activity, Plus, ArrowRight, RefreshCw, WifiOff } from 'lucide-react';
import { PageHeader } from '@/components/PageHeader';
import { StatCard } from '@/components/StatCard';
import { InfrastructureCard } from '@/components/InfrastructureCard';
import { useInfrastructureStatus } from '@/hooks/useInfrastructureStatus';
import type { DashboardStats } from '@/types/admin';
import * as api from '@/lib/api';

const DEMO_STATS: DashboardStats = {
  totalTenants: 12,
  activeTenants: 8,
  suspendedTenants: 2,
  totalRegions: 4,
  activeRegions: 4,
  totalDatasources: 3,
  tierBreakdown: { STANDARD: 5, PREMIUM: 4, ENTERPRISE: 3 },
};

export default function DashboardPage() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isDemo, setIsDemo] = useState(false);
  const { data: infraStatus, loading: infraLoading, refetch: refetchInfra } = useInfrastructureStatus();

  const fetchStats = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.getDashboardStats();
      setStats(data);
      setIsDemo(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load stats');
      setStats(DEMO_STATS);
      setIsDemo(true);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchStats();
  }, [fetchStats]);

  const displayStats = stats ?? DEMO_STATS;

  return (
    <div className="p-8">
      <PageHeader
        title="Dashboard"
        description="Platform overview and system health"
      />

      {isDemo && (
        <div className="mb-6 px-4 py-3 rounded-md bg-amber-50 border border-amber-200">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <WifiOff className="w-4 h-4 text-amber-600" />
              <p className="text-sm text-amber-700">
                Backend service unavailable — showing demo data.
                Start the Kernel service to see live data.
              </p>
            </div>
            <button
              onClick={fetchStats}
              className="flex items-center gap-1 text-xs font-medium text-amber-700 hover:text-amber-900 px-2 py-1 rounded hover:bg-amber-100 transition-colors"
            >
              <RefreshCw className="w-3 h-3" />
              Retry
            </button>
          </div>
        </div>
      )}

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <StatCard
          title="Active Tenants"
          value={loading ? '...' : displayStats.activeTenants}
          subtitle={loading ? '' : `${displayStats.totalTenants} total`}
          icon={<Users className="w-5 h-5" />}
          color="blue"
        />
        <StatCard
          title="Suspended"
          value={loading ? '...' : displayStats.suspendedTenants}
          subtitle="Tenants paused"
          icon={<Users className="w-5 h-5" />}
          color="amber"
        />
        <StatCard
          title="Active Regions"
          value={loading ? '...' : displayStats.activeRegions}
          subtitle={loading ? '' : `${displayStats.totalRegions} total`}
          icon={<Globe className="w-5 h-5" />}
          color="green"
        />
        <StatCard
          title="Datasources"
          value={loading ? '...' : displayStats.totalDatasources}
          subtitle="Connection pools"
          icon={<Database className="w-5 h-5" />}
          color="purple"
        />
      </div>

      {/* Tier Breakdown */}
      {displayStats.tierBreakdown && (
        <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-6 mb-8">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Tenant Tiers</h3>
          <div className="grid grid-cols-3 gap-4">
            {Object.entries(displayStats.tierBreakdown).map(([tier, count]) => (
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

      {/* Infrastructure Services */}
      <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold text-gray-900">Infrastructure</h3>
          <div className="flex items-center space-x-2">
            {infraStatus?.timestamp && (
              <p className="text-xs text-gray-400">
                Updated {new Date(infraStatus.timestamp).toLocaleTimeString()}
              </p>
            )}
            <button
              onClick={refetchInfra}
              className="p-1 text-gray-400 hover:text-gray-600 rounded transition-colors"
              title="Refresh"
            >
              <RefreshCw className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
          {infraStatus?.services ? (
            infraStatus.services.map((svc) => (
              <InfrastructureCard
                key={svc.name}
                name={svc.name}
                description={svc.description}
                url={svc.url}
                status={svc.status}
                summary={svc.summary}
                loading={infraLoading && !infraStatus}
              />
            ))
          ) : (
            Array.from({ length: 9 }).map((_, i) => (
              <div key={i} className="p-3 border border-gray-200 rounded-lg animate-pulse">
                <div className="flex items-center justify-between mb-2">
                  <div className="h-4 bg-gray-200 rounded w-16" />
                  <div className="h-2.5 w-2.5 bg-gray-200 rounded-full" />
                </div>
                <div className="h-3 bg-gray-100 rounded w-20 mb-1" />
                <div className="h-3 bg-gray-100 rounded w-14" />
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
