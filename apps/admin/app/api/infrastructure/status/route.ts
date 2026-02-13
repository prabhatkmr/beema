import { NextResponse } from 'next/server';
import type { ServiceStatus, ServiceInfo } from '@/types/infrastructure';

export const dynamic = 'force-dynamic';

const SERVICE_URLS = {
  flink: process.env.FLINK_URL || 'http://localhost:8081',
  prometheus: process.env.PROMETHEUS_URL || 'http://localhost:9090',
  jaeger: process.env.JAEGER_URL || 'http://localhost:16686',
  grafana: process.env.GRAFANA_URL || 'http://localhost:3002',
  keycloak: process.env.KEYCLOAK_URL || 'http://localhost:8180',
  minio: process.env.MINIO_URL || 'http://localhost:9000',
  inngest: process.env.INNGEST_URL || 'http://localhost:8288',
  kernel: process.env.BEEMA_KERNEL_URL || 'http://localhost:8080',
};

const GRAFANA_AUTH = process.env.GRAFANA_BASIC_AUTH || 'admin:admin';
const FETCH_TIMEOUT = 4000;

async function fetchWithTimeout(url: string, options?: RequestInit): Promise<Response> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), FETCH_TIMEOUT);
  try {
    return await fetch(url, { ...options, signal: controller.signal });
  } finally {
    clearTimeout(timeoutId);
  }
}

interface QueryResult {
  status: ServiceStatus;
  summary: Record<string, unknown>;
}

async function queryFlink(): Promise<QueryResult> {
  const res = await fetchWithTimeout(`${SERVICE_URLS.flink}/overview`);
  if (!res.ok) return { status: 'unreachable', summary: {} };
  const data = await res.json();
  const hasFailedJobs = (data['jobs-failed'] || 0) > 0;
  return {
    status: hasFailedJobs ? 'degraded' : 'healthy',
    summary: {
      version: data['flink-version'],
      jobsRunning: data['jobs-running'] || 0,
      jobsFinished: data['jobs-finished'] || 0,
      jobsCancelled: data['jobs-cancelled'] || 0,
      jobsFailed: data['jobs-failed'] || 0,
      taskManagers: data['taskmanagers'] || 0,
      slotsTotal: data['slots-total'] || 0,
      slotsAvailable: data['slots-available'] || 0,
    },
  };
}

async function queryPrometheus(): Promise<QueryResult> {
  const [targetsRes, alertsRes] = await Promise.allSettled([
    fetchWithTimeout(`${SERVICE_URLS.prometheus}/api/v1/targets`),
    fetchWithTimeout(`${SERVICE_URLS.prometheus}/api/v1/alerts`),
  ]);

  let totalTargets = 0;
  let targetsUp = 0;
  let targetsDown = 0;

  if (targetsRes.status === 'fulfilled' && targetsRes.value.ok) {
    const data = await targetsRes.value.json();
    const activeTargets = data?.data?.activeTargets || [];
    totalTargets = activeTargets.length;
    targetsUp = activeTargets.filter((t: { health: string }) => t.health === 'up').length;
    targetsDown = activeTargets.filter((t: { health: string }) => t.health === 'down').length;
  }

  let activeAlerts = 0;
  if (alertsRes.status === 'fulfilled' && alertsRes.value.ok) {
    const data = await alertsRes.value.json();
    activeAlerts = data?.data?.alerts?.length || 0;
  }

  if (totalTargets === 0 && targetsRes.status === 'rejected') {
    return { status: 'unreachable', summary: {} };
  }

  const status: ServiceStatus =
    targetsDown > 0 ? 'degraded' : totalTargets > 0 ? 'healthy' : 'unreachable';

  return {
    status,
    summary: { totalTargets, targetsUp, targetsDown, activeAlerts },
  };
}

async function queryJaeger(): Promise<QueryResult> {
  const res = await fetchWithTimeout(`${SERVICE_URLS.jaeger}/api/services`);
  if (!res.ok) return { status: 'unreachable', summary: {} };
  const data = await res.json();
  const services: string[] = (data?.data || []).filter((s: string) => s !== 'jaeger-query');
  return {
    status: 'healthy',
    summary: { serviceCount: services.length, tracedServices: services },
  };
}

async function queryGrafana(): Promise<QueryResult> {
  const authHeader = `Basic ${Buffer.from(GRAFANA_AUTH).toString('base64')}`;
  const headers = { Authorization: authHeader };

  const [dashRes, dsRes] = await Promise.allSettled([
    fetchWithTimeout(`${SERVICE_URLS.grafana}/api/search?type=dash-db`, { headers }),
    fetchWithTimeout(`${SERVICE_URLS.grafana}/api/datasources`, { headers }),
  ]);

  let dashboardCount = 0;
  let datasourceCount = 0;

  if (dashRes.status === 'fulfilled' && dashRes.value.ok) {
    const data = await dashRes.value.json();
    dashboardCount = Array.isArray(data) ? data.length : 0;
  }

  if (dsRes.status === 'fulfilled' && dsRes.value.ok) {
    const data = await dsRes.value.json();
    datasourceCount = Array.isArray(data) ? data.length : 0;
  }

  if (dashRes.status === 'rejected' && dsRes.status === 'rejected') {
    return { status: 'unreachable', summary: {} };
  }

  return {
    status: 'healthy',
    summary: { dashboardCount, datasourceCount },
  };
}

async function queryHealthEndpoint(url: string): Promise<QueryResult> {
  const res = await fetchWithTimeout(url);
  return {
    status: res.ok ? 'healthy' : 'unreachable',
    summary: { healthy: res.ok },
  };
}

async function queryKernel(): Promise<QueryResult> {
  const res = await fetchWithTimeout(`${SERVICE_URLS.kernel}/actuator/health`);
  if (!res.ok) return { status: 'unreachable', summary: { healthy: false } };
  const data = await res.json();
  const isUp = data?.status === 'UP';
  return {
    status: isUp ? 'healthy' : 'degraded',
    summary: { healthy: isUp, status: data?.status },
  };
}

function buildServiceInfo(
  name: string,
  url: string,
  description: string,
  result: PromiseSettledResult<QueryResult>
): ServiceInfo {
  const now = new Date().toISOString();
  if (result.status === 'fulfilled') {
    return {
      name,
      url,
      description,
      status: result.value.status,
      summary: result.value.summary,
      lastChecked: now,
    };
  }
  return {
    name,
    url,
    description,
    status: 'unreachable',
    summary: {},
    lastChecked: now,
    error: result.reason?.message || 'Service unreachable',
  };
}

export async function GET() {
  const timestamp = new Date().toISOString();

  const temporalUiUrl = process.env.TEMPORAL_UI_URL || 'http://localhost:8088';

  const results = await Promise.allSettled([
    queryFlink(),
    queryPrometheus(),
    queryJaeger(),
    queryGrafana(),
    queryHealthEndpoint(`${SERVICE_URLS.keycloak}/health/ready`),
    queryHealthEndpoint(`${SERVICE_URLS.minio}/minio/health/live`),
    queryHealthEndpoint(`${SERVICE_URLS.inngest}/health`),
    queryKernel(),
    queryHealthEndpoint(temporalUiUrl),
  ]);

  const services: ServiceInfo[] = [
    buildServiceInfo('Flink', SERVICE_URLS.flink, 'Stream Processing', results[0]),
    buildServiceInfo('Prometheus', SERVICE_URLS.prometheus, 'Metrics', results[1]),
    buildServiceInfo('Jaeger', SERVICE_URLS.jaeger, 'Tracing', results[2]),
    buildServiceInfo('Grafana', SERVICE_URLS.grafana, 'Dashboards', results[3]),
    buildServiceInfo('Temporal', temporalUiUrl, 'Workflows', results[8]),
    buildServiceInfo('Keycloak', SERVICE_URLS.keycloak, 'Authentication', results[4]),
    buildServiceInfo('MinIO', SERVICE_URLS.minio.replace('9000', '9001'), 'Object Storage', results[5]),
    buildServiceInfo('Inngest', SERVICE_URLS.inngest, 'Background Jobs', results[6]),
    buildServiceInfo('Kernel', `${SERVICE_URLS.kernel}/swagger-ui.html`, 'Core API', results[7]),
  ];

  return NextResponse.json({ services, timestamp });
}
