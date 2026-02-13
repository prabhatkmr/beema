import type {
  Tenant, TenantRequest,
  Region, RegionRequest,
  DatasourceConfig, DatasourceRequest,
  DashboardStats,
} from '@/types/admin';

const BASE_URL = '/api/admin';

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: response.statusText }));
    throw new Error(error.message || `Request failed with status ${response.status}`);
  }
  return response.json();
}

// Dashboard
export async function getDashboardStats(): Promise<DashboardStats> {
  const response = await fetch(`${BASE_URL}/dashboard/stats`);
  return handleResponse<DashboardStats>(response);
}

// Tenants
export async function listTenants(status?: string, region?: string): Promise<Tenant[]> {
  const params = new URLSearchParams();
  if (status) params.set('status', status);
  if (region) params.set('region', region);
  const query = params.toString();
  const response = await fetch(`${BASE_URL}/tenants${query ? '?' + query : ''}`);
  return handleResponse<Tenant[]>(response);
}

export async function getTenant(id: string): Promise<Tenant> {
  const response = await fetch(`${BASE_URL}/tenants/${id}`);
  return handleResponse<Tenant>(response);
}

export async function createTenant(data: TenantRequest): Promise<Tenant> {
  const response = await fetch(`${BASE_URL}/tenants`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  return handleResponse<Tenant>(response);
}

export async function updateTenant(id: string, data: TenantRequest): Promise<Tenant> {
  const response = await fetch(`${BASE_URL}/tenants/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  return handleResponse<Tenant>(response);
}

export async function activateTenant(id: string): Promise<Tenant> {
  const response = await fetch(`${BASE_URL}/tenants/${id}/activate`, { method: 'POST' });
  return handleResponse<Tenant>(response);
}

export async function suspendTenant(id: string): Promise<Tenant> {
  const response = await fetch(`${BASE_URL}/tenants/${id}/suspend`, { method: 'POST' });
  return handleResponse<Tenant>(response);
}

export async function deactivateTenant(id: string): Promise<Tenant> {
  const response = await fetch(`${BASE_URL}/tenants/${id}/deactivate`, { method: 'POST' });
  return handleResponse<Tenant>(response);
}

// Regions
export async function listRegions(): Promise<Region[]> {
  const response = await fetch(`${BASE_URL}/regions`);
  return handleResponse<Region[]>(response);
}

export async function createRegion(data: RegionRequest): Promise<Region> {
  const response = await fetch(`${BASE_URL}/regions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  return handleResponse<Region>(response);
}

export async function updateRegion(id: string, data: RegionRequest): Promise<Region> {
  const response = await fetch(`${BASE_URL}/regions/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  return handleResponse<Region>(response);
}

// Datasources
export async function listDatasources(): Promise<DatasourceConfig[]> {
  const response = await fetch(`${BASE_URL}/datasources`);
  return handleResponse<DatasourceConfig[]>(response);
}

export async function createDatasource(data: DatasourceRequest): Promise<DatasourceConfig> {
  const response = await fetch(`${BASE_URL}/datasources`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  return handleResponse<DatasourceConfig>(response);
}

export async function updateDatasource(id: string, data: DatasourceRequest): Promise<DatasourceConfig> {
  const response = await fetch(`${BASE_URL}/datasources/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  return handleResponse<DatasourceConfig>(response);
}
