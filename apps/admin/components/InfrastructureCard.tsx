import { ExternalLink } from 'lucide-react';
import type { ServiceStatus } from '@/types/infrastructure';

interface InfrastructureCardProps {
  name: string;
  description: string;
  url: string;
  status: ServiceStatus;
  summary: Record<string, unknown>;
  loading?: boolean;
}

const statusDotColor: Record<ServiceStatus, string> = {
  healthy: 'bg-green-500',
  degraded: 'bg-amber-500',
  unreachable: 'bg-red-400',
};

function renderMetrics(name: string, summary: Record<string, unknown>, status: ServiceStatus) {
  if (status === 'unreachable') {
    return <p className="text-xs text-red-400 mt-1">Unreachable</p>;
  }

  switch (name) {
    case 'Flink': {
      const running = summary.jobsRunning as number ?? 0;
      const slotsAvail = summary.slotsAvailable as number ?? 0;
      const slotsTotal = summary.slotsTotal as number ?? 0;
      const tms = summary.taskManagers as number ?? 0;
      return (
        <div className="mt-1.5 space-y-0.5">
          <p className="text-xs text-gray-700 font-medium">{running} running job{running !== 1 ? 's' : ''}</p>
          <p className="text-xs text-gray-500">{slotsAvail}/{slotsTotal} slots free &middot; {tms} TM{tms !== 1 ? 's' : ''}</p>
        </div>
      );
    }
    case 'Prometheus': {
      const up = summary.targetsUp as number ?? 0;
      const total = summary.totalTargets as number ?? 0;
      const alerts = summary.activeAlerts as number ?? 0;
      return (
        <div className="mt-1.5 space-y-0.5">
          <p className="text-xs text-gray-700 font-medium">{up}/{total} targets up</p>
          <p className="text-xs text-gray-500">{alerts} active alert{alerts !== 1 ? 's' : ''}</p>
        </div>
      );
    }
    case 'Jaeger': {
      const count = summary.serviceCount as number ?? 0;
      return (
        <div className="mt-1.5">
          <p className="text-xs text-gray-700 font-medium">{count} service{count !== 1 ? 's' : ''} traced</p>
        </div>
      );
    }
    case 'Grafana': {
      const dashboards = summary.dashboardCount as number ?? 0;
      const datasources = summary.datasourceCount as number ?? 0;
      return (
        <div className="mt-1.5 space-y-0.5">
          <p className="text-xs text-gray-700 font-medium">{dashboards} dashboard{dashboards !== 1 ? 's' : ''}</p>
          <p className="text-xs text-gray-500">{datasources} datasource{datasources !== 1 ? 's' : ''}</p>
        </div>
      );
    }
    default:
      return (
        <div className="mt-1.5">
          <p className="text-xs text-green-600 font-medium">Healthy</p>
        </div>
      );
  }
}

export function InfrastructureCard({
  name,
  description,
  url,
  status,
  summary,
  loading,
}: InfrastructureCardProps) {
  if (loading) {
    return (
      <div className="p-3 border border-gray-200 rounded-lg animate-pulse">
        <div className="flex items-center justify-between mb-2">
          <div className="h-4 bg-gray-200 rounded w-16" />
          <div className="h-2.5 w-2.5 bg-gray-200 rounded-full" />
        </div>
        <div className="h-3 bg-gray-100 rounded w-20 mb-1" />
        <div className="h-3 bg-gray-100 rounded w-14" />
      </div>
    );
  }

  return (
    <a
      href={url}
      target="_blank"
      rel="noopener noreferrer"
      className={`group p-3 border rounded-lg transition-all ${
        status === 'unreachable'
          ? 'border-gray-200 bg-gray-50 opacity-75 hover:opacity-100'
          : 'border-gray-200 hover:border-blue-400 hover:shadow-sm'
      }`}
    >
      <div className="flex items-center justify-between mb-0.5">
        <p className="text-sm font-medium text-gray-900">{name}</p>
        <div className="flex items-center space-x-1.5">
          <span className={`inline-block h-2 w-2 rounded-full ${statusDotColor[status]}`} />
          <ExternalLink className="w-3 h-3 text-gray-400 opacity-0 group-hover:opacity-100 transition-opacity" />
        </div>
      </div>
      <p className="text-xs text-gray-500">{description}</p>
      {renderMetrics(name, summary, status)}
    </a>
  );
}
