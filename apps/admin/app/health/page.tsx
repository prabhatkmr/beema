'use client';

import { CheckCircle, XCircle, AlertTriangle, ExternalLink, RefreshCw } from 'lucide-react';
import { PageHeader } from '@/components/PageHeader';
import { useInfrastructureStatus } from '@/hooks/useInfrastructureStatus';
import type { ServiceStatus } from '@/types/infrastructure';

function statusIcon(status: ServiceStatus) {
  switch (status) {
    case 'healthy':
      return <CheckCircle className="w-5 h-5 text-green-500" />;
    case 'degraded':
      return <AlertTriangle className="w-5 h-5 text-amber-500" />;
    case 'unreachable':
      return <XCircle className="w-5 h-5 text-red-500" />;
  }
}

function statusLabel(status: ServiceStatus) {
  switch (status) {
    case 'healthy':
      return <span className="text-xs font-medium text-green-600">Healthy</span>;
    case 'degraded':
      return <span className="text-xs font-medium text-amber-600">Degraded</span>;
    case 'unreachable':
      return <span className="text-xs font-medium text-red-500">Unreachable</span>;
  }
}

function renderSummary(name: string, summary: Record<string, unknown>, status: ServiceStatus) {
  if (status === 'unreachable') return null;

  switch (name) {
    case 'Flink': {
      const running = summary.jobsRunning as number ?? 0;
      const finished = summary.jobsFinished as number ?? 0;
      const failed = summary.jobsFailed as number ?? 0;
      const tms = summary.taskManagers as number ?? 0;
      const slotsAvail = summary.slotsAvailable as number ?? 0;
      const slotsTotal = summary.slotsTotal as number ?? 0;
      return (
        <div className="flex items-center space-x-4 text-xs text-gray-500">
          <span>{running} running / {finished} finished / {failed} failed</span>
          <span>{tms} TaskManager{tms !== 1 ? 's' : ''}</span>
          <span>{slotsAvail}/{slotsTotal} slots free</span>
        </div>
      );
    }
    case 'Prometheus': {
      const up = summary.targetsUp as number ?? 0;
      const total = summary.totalTargets as number ?? 0;
      const down = summary.targetsDown as number ?? 0;
      const alerts = summary.activeAlerts as number ?? 0;
      return (
        <div className="flex items-center space-x-4 text-xs text-gray-500">
          <span>{up}/{total} targets up</span>
          {down > 0 && <span className="text-red-500">{down} down</span>}
          <span>{alerts} active alert{alerts !== 1 ? 's' : ''}</span>
        </div>
      );
    }
    case 'Jaeger': {
      const services = summary.tracedServices as string[] ?? [];
      return (
        <div className="text-xs text-gray-500">
          <span>{services.length} service{services.length !== 1 ? 's' : ''} traced</span>
          {services.length > 0 && (
            <span className="ml-2 text-gray-400">({services.join(', ')})</span>
          )}
        </div>
      );
    }
    case 'Grafana': {
      const dashboards = summary.dashboardCount as number ?? 0;
      const datasources = summary.datasourceCount as number ?? 0;
      return (
        <div className="flex items-center space-x-4 text-xs text-gray-500">
          <span>{dashboards} dashboard{dashboards !== 1 ? 's' : ''}</span>
          <span>{datasources} datasource{datasources !== 1 ? 's' : ''}</span>
        </div>
      );
    }
    case 'Kernel': {
      const kernelStatus = summary.status as string ?? 'UP';
      return (
        <div className="text-xs text-gray-500">
          <span>Status: {kernelStatus}</span>
        </div>
      );
    }
    default:
      return null;
  }
}

export default function HealthPage() {
  const { data: infraStatus, loading, refetch } = useInfrastructureStatus();

  const healthyCount = infraStatus?.services.filter(s => s.status === 'healthy').length ?? 0;
  const totalCount = infraStatus?.services.length ?? 0;
  const degradedCount = infraStatus?.services.filter(s => s.status === 'degraded').length ?? 0;
  const unreachableCount = infraStatus?.services.filter(s => s.status === 'unreachable').length ?? 0;

  return (
    <div className="p-8">
      <PageHeader
        title="System Health"
        description="Monitor infrastructure services and observability tools"
      />

      {/* Summary Bar */}
      {infraStatus && (
        <div className="flex items-center space-x-6 mb-6 p-4 bg-white rounded-lg border border-gray-200 shadow-sm">
          <div className="flex items-center space-x-2">
            <CheckCircle className="w-4 h-4 text-green-500" />
            <span className="text-sm text-gray-700"><strong>{healthyCount}</strong> healthy</span>
          </div>
          {degradedCount > 0 && (
            <div className="flex items-center space-x-2">
              <AlertTriangle className="w-4 h-4 text-amber-500" />
              <span className="text-sm text-gray-700"><strong>{degradedCount}</strong> degraded</span>
            </div>
          )}
          {unreachableCount > 0 && (
            <div className="flex items-center space-x-2">
              <XCircle className="w-4 h-4 text-red-500" />
              <span className="text-sm text-gray-700"><strong>{unreachableCount}</strong> unreachable</span>
            </div>
          )}
          <div className="flex-1" />
          <div className="flex items-center space-x-2">
            {infraStatus.timestamp && (
              <p className="text-xs text-gray-400">
                Updated {new Date(infraStatus.timestamp).toLocaleTimeString()}
              </p>
            )}
            <button
              onClick={refetch}
              className="p-1 text-gray-400 hover:text-gray-600 rounded transition-colors"
              title="Refresh"
            >
              <RefreshCw className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      )}

      {/* All Services */}
      <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-6 mb-8">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">
          All Services ({healthyCount}/{totalCount} healthy)
        </h3>
        <div className="space-y-2">
          {loading && !infraStatus ? (
            Array.from({ length: 9 }).map((_, i) => (
              <div key={i} className="flex items-center space-x-3 p-3 bg-gray-50 rounded-lg animate-pulse">
                <div className="w-5 h-5 bg-gray-200 rounded-full" />
                <div className="flex-1">
                  <div className="h-4 bg-gray-200 rounded w-24 mb-1" />
                  <div className="h-3 bg-gray-100 rounded w-32" />
                </div>
              </div>
            ))
          ) : (
            infraStatus?.services.map((svc) => (
              <div
                key={svc.name}
                className={`flex items-center justify-between p-3 rounded-lg ${
                  svc.status === 'unreachable' ? 'bg-red-50' :
                  svc.status === 'degraded' ? 'bg-amber-50' : 'bg-gray-50'
                }`}
              >
                <div className="flex items-center space-x-3 flex-1 min-w-0">
                  {statusIcon(svc.status)}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center space-x-2">
                      <p className="text-sm font-medium text-gray-900">{svc.name}</p>
                      {statusLabel(svc.status)}
                      <span className="text-xs text-gray-400">{svc.description}</span>
                    </div>
                    {renderSummary(svc.name, svc.summary, svc.status)}
                  </div>
                </div>
                <a
                  href={svc.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-blue-600 hover:text-blue-800 ml-3 shrink-0"
                >
                  <ExternalLink className="w-4 h-4" />
                </a>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
