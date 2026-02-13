export interface Schedule {
  id: string;
  tenant_id: string;
  schedule_id: string;
  job_type: string;
  cron_expression: string;
  is_active: boolean;
  job_params: Record<string, unknown>;
  temporal_schedule_id: string | null;
  temporal_namespace: string;
  created_by: string;
  updated_by: string;
  created_at: string;
  updated_at: string;
  version: number;
  next_run_time?: string;
}

export interface ScheduleRequest {
  schedule_id: string;
  job_type: JobType;
  cron_expression: string;
  is_active?: boolean;
  job_params?: Record<string, unknown>;
}

export interface ScheduleResponse {
  id: string;
  tenant_id: string;
  schedule_id: string;
  job_type: string;
  cron_expression: string;
  is_active: boolean;
  job_params: Record<string, unknown>;
  temporal_schedule_id: string | null;
  next_run_time: string | null;
  created_at: string;
  updated_at: string;
}

export interface TriggerRequest {
  params_override?: Record<string, unknown>;
}

export type JobType =
  | 'RENEWAL_BATCH'
  | 'GL_EXPORT'
  | 'PARQUET_EXPORT'
  | 'PREMIUM_CALC'
  | 'CLAIMS_RESERVE'
  | 'REPORT_GENERATION';

export const JOB_TYPES: { value: JobType; label: string }[] = [
  { value: 'RENEWAL_BATCH', label: 'Renewal Batch' },
  { value: 'GL_EXPORT', label: 'GL Export' },
  { value: 'PARQUET_EXPORT', label: 'Parquet Export' },
  { value: 'PREMIUM_CALC', label: 'Premium Calculation' },
  { value: 'CLAIMS_RESERVE', label: 'Claims Reserve' },
  { value: 'REPORT_GENERATION', label: 'Report Generation' },
];

export const CRON_PRESETS: { label: string; cron: string; description: string }[] = [
  { label: 'Every hour', cron: '0 * * * *', description: 'At minute 0 of every hour' },
  { label: 'Daily at 2 AM', cron: '0 2 * * *', description: 'Every day at 2:00 AM' },
  { label: 'Daily at midnight', cron: '0 0 * * *', description: 'Every day at midnight' },
  { label: 'Weekdays at 6 AM', cron: '0 6 * * 1-5', description: 'Mon-Fri at 6:00 AM' },
  { label: 'Weekly Sunday', cron: '0 0 * * 0', description: 'Every Sunday at midnight' },
  { label: 'Monthly 1st', cron: '0 0 1 * *', description: '1st of every month at midnight' },
  { label: 'Quarterly', cron: '0 0 1 1,4,7,10 *', description: '1st of Jan, Apr, Jul, Oct' },
];

export const JOB_PARAM_TEMPLATES: Record<JobType, Record<string, unknown>> = {
  RENEWAL_BATCH: {
    daysBeforeExpiry: 30,
    batchSize: 100,
    notifyBroker: true,
  },
  GL_EXPORT: {
    format: 'CSV',
    includeHeaders: true,
    accountingPeriod: 'current_month',
  },
  PARQUET_EXPORT: {
    targetBucket: 's3://beema-data-lake',
    compressionCodec: 'SNAPPY',
    partitionBy: ['year', 'month'],
  },
  PREMIUM_CALC: {
    recalculateAll: false,
    effectiveDate: 'today',
  },
  CLAIMS_RESERVE: {
    method: 'chain_ladder',
    confidenceLevel: 0.95,
  },
  REPORT_GENERATION: {
    reportType: 'monthly_summary',
    format: 'PDF',
    recipients: [],
  },
};
