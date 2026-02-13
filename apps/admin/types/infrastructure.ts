export type ServiceStatus = 'healthy' | 'degraded' | 'unreachable';

export interface ServiceInfo {
  name: string;
  status: ServiceStatus;
  url: string;
  description: string;
  summary: Record<string, unknown>;
  lastChecked: string;
  error?: string;
}

export interface InfrastructureStatusResponse {
  services: ServiceInfo[];
  timestamp: string;
}
