import type { Schedule, ScheduleRequest, ScheduleResponse, TriggerRequest } from '@/types/batch-schedule';

const BASE_URL = '/api/batch-schedules';

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: response.statusText }));
    throw new Error(error.message || `Request failed with status ${response.status}`);
  }
  return response.json();
}

export async function listSchedules(): Promise<ScheduleResponse[]> {
  const response = await fetch(BASE_URL);
  return handleResponse<ScheduleResponse[]>(response);
}

export async function getSchedule(id: string): Promise<ScheduleResponse> {
  const response = await fetch(`${BASE_URL}/${id}`);
  return handleResponse<ScheduleResponse>(response);
}

export async function createSchedule(data: ScheduleRequest): Promise<ScheduleResponse> {
  const response = await fetch(BASE_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  return handleResponse<ScheduleResponse>(response);
}

export async function updateSchedule(id: string, data: ScheduleRequest): Promise<ScheduleResponse> {
  const response = await fetch(`${BASE_URL}/${id}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });
  return handleResponse<ScheduleResponse>(response);
}

export async function deleteSchedule(id: string): Promise<void> {
  const response = await fetch(`${BASE_URL}/${id}`, { method: 'DELETE' });
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: response.statusText }));
    throw new Error(error.message || 'Delete failed');
  }
}

export async function triggerSchedule(id: string, data?: TriggerRequest): Promise<void> {
  const response = await fetch(`${BASE_URL}/${id}/trigger`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data || {}),
  });
  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: response.statusText }));
    throw new Error(error.message || 'Trigger failed');
  }
}

export async function pauseSchedule(id: string): Promise<ScheduleResponse> {
  const response = await fetch(`${BASE_URL}/${id}/pause`, { method: 'POST' });
  return handleResponse<ScheduleResponse>(response);
}

export async function unpauseSchedule(id: string): Promise<ScheduleResponse> {
  const response = await fetch(`${BASE_URL}/${id}/unpause`, { method: 'POST' });
  return handleResponse<ScheduleResponse>(response);
}
