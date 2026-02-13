'use client';

import { useState, useEffect } from 'react';
import { Activity, CheckCircle, XCircle, ExternalLink } from 'lucide-react';
import { PageHeader } from '@/components/PageHeader';

interface ServiceHealth {
  name: string;
  url: string;
  healthUrl: string;
  status: 'UP' | 'DOWN' | 'UNKNOWN';
  description: string;
}

const services: Omit<ServiceHealth, 'status'>[] = [
  { name: 'Beema Kernel', url: 'http://localhost:8080/swagger-ui.html', healthUrl: 'http://localhost:8080/actuator/health', description: 'Core API' },
  { name: 'PostgreSQL', url: '', healthUrl: '', description: 'Database (port 5433)' },
  { name: 'Kafka', url: '', healthUrl: '', description: 'Event streaming (port 9092)' },
  { name: 'Temporal', url: 'http://localhost:8088', healthUrl: '', description: 'Workflow engine' },
  { name: 'MinIO', url: 'http://localhost:9001', healthUrl: '', description: 'Object storage' },
  { name: 'Keycloak', url: 'http://localhost:8180', healthUrl: '', description: 'Authentication' },
];

const observabilityLinks = [
  { name: 'Grafana', url: 'http://localhost:3002', description: 'Metrics dashboards' },
  { name: 'Jaeger', url: 'http://localhost:16686', description: 'Distributed tracing' },
  { name: 'Prometheus', url: 'http://localhost:9090', description: 'Metrics collection' },
  { name: 'Temporal UI', url: 'http://localhost:8088', description: 'Workflow management' },
  { name: 'Flink Dashboard', url: 'http://localhost:8081', description: 'Stream processing' },
  { name: 'Inngest', url: 'http://localhost:8288', description: 'Background jobs' },
];

export default function HealthPage() {
  const [healthStatuses, setHealthStatuses] = useState<Record<string, 'UP' | 'DOWN' | 'UNKNOWN'>>({});
  const [checking, setChecking] = useState(true);

  useEffect(() => {
    async function checkHealth() {
      const statuses: Record<string, 'UP' | 'DOWN' | 'UNKNOWN'> = {};

      // Check kernel health endpoint
      try {
        const res = await fetch('/api/admin/dashboard/stats');
        statuses['Beema Kernel'] = res.ok ? 'UP' : 'DOWN';
      } catch {
        statuses['Beema Kernel'] = 'DOWN';
      }

      // Other services default to UNKNOWN (can't check from browser)
      for (const svc of services) {
        if (!statuses[svc.name]) {
          statuses[svc.name] = 'UNKNOWN';
        }
      }

      setHealthStatuses(statuses);
      setChecking(false);
    }

    checkHealth();
  }, []);

  const statusIcon = (status: string) => {
    switch (status) {
      case 'UP': return <CheckCircle className="w-5 h-5 text-green-500" />;
      case 'DOWN': return <XCircle className="w-5 h-5 text-red-500" />;
      default: return <Activity className="w-5 h-5 text-gray-400" />;
    }
  };

  return (
    <div className="p-8">
      <PageHeader
        title="System Health"
        description="Monitor infrastructure services and observability tools"
      />

      {/* Services */}
      <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-6 mb-8">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Core Services</h3>
        <div className="space-y-3">
          {services.map((svc) => (
            <div key={svc.name} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
              <div className="flex items-center space-x-3">
                {checking ? (
                  <div className="w-5 h-5 animate-spin rounded-full border-2 border-gray-300 border-t-blue-600" />
                ) : (
                  statusIcon(healthStatuses[svc.name] || 'UNKNOWN')
                )}
                <div>
                  <p className="text-sm font-medium text-gray-900">{svc.name}</p>
                  <p className="text-xs text-gray-500">{svc.description}</p>
                </div>
              </div>
              {svc.url && (
                <a
                  href={svc.url}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-blue-600 hover:text-blue-800"
                >
                  <ExternalLink className="w-4 h-4" />
                </a>
              )}
            </div>
          ))}
        </div>
      </div>

      {/* Observability */}
      <div className="bg-white rounded-lg border border-gray-200 shadow-sm p-6">
        <h3 className="text-lg font-semibold text-gray-900 mb-4">Observability & Monitoring</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {observabilityLinks.map((link) => (
            <a
              key={link.name}
              href={link.url}
              target="_blank"
              rel="noopener noreferrer"
              className="flex items-center justify-between p-4 border border-gray-200 rounded-lg hover:border-blue-500 hover:shadow-sm transition-all"
            >
              <div>
                <p className="text-sm font-medium text-gray-900">{link.name}</p>
                <p className="text-xs text-gray-500">{link.description}</p>
              </div>
              <ExternalLink className="w-4 h-4 text-gray-400" />
            </a>
          ))}
        </div>
      </div>
    </div>
  );
}
