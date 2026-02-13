export interface Tenant {
  id: string;
  tenantId: string;
  name: string;
  slug: string;
  status: 'ACTIVE' | 'SUSPENDED' | 'PROVISIONING' | 'DEACTIVATED';
  tier: 'STANDARD' | 'PREMIUM' | 'ENTERPRISE';
  regionCode: string;
  contactEmail?: string;
  config: Record<string, unknown>;
  datasourceKey?: string;
  createdBy: string;
  updatedBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface TenantRequest {
  tenantId: string;
  name: string;
  slug: string;
  tier?: string;
  regionCode?: string;
  contactEmail?: string;
  config?: Record<string, unknown>;
  datasourceKey?: string;
  createdBy?: string;
}

export interface Region {
  id: string;
  code: string;
  name: string;
  description?: string;
  dataResidencyRules: Record<string, unknown>;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface RegionRequest {
  code: string;
  name: string;
  description?: string;
  dataResidencyRules?: Record<string, unknown>;
  isActive?: boolean;
}

export interface DatasourceConfig {
  id: string;
  name: string;
  url: string;
  username: string;
  poolSize: number;
  status: 'ACTIVE' | 'INACTIVE' | 'MAINTENANCE';
  config: Record<string, unknown>;
  createdAt: string;
  updatedAt: string;
}

export interface DatasourceRequest {
  name: string;
  url: string;
  username: string;
  poolSize?: number;
  status?: string;
  config?: Record<string, unknown>;
}

export interface DashboardStats {
  totalTenants: number;
  activeTenants: number;
  suspendedTenants: number;
  totalRegions: number;
  activeRegions: number;
  totalDatasources: number;
  tierBreakdown: Record<string, number>;
}
