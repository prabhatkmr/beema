// Webhook types for Studio

export interface Webhook {
  webhook_id: number;
  webhook_name: string;
  tenant_id: string;
  event_type: string;
  url: string;
  secret: string;
  enabled: boolean;
  headers: Record<string, string>;
  retry_config: {
    maxAttempts: number;
    backoffMs: number;
  };
  created_at: string;
  updated_at: string;
  created_by: string;
}

export interface WebhookDelivery {
  delivery_id: number;
  webhook_id: number;
  event_id: string;
  event_type: string;
  status: 'success' | 'failed' | 'retrying';
  status_code?: number;
  response_body?: string;
  error_message?: string;
  attempt_number: number;
  delivered_at: string;
}

export interface CreateWebhookRequest {
  webhook_name: string;
  event_type: string;
  url: string;
  secret: string;
  enabled?: boolean;
  headers?: Record<string, string>;
  retry_config?: {
    maxAttempts: number;
    backoffMs: number;
  };
}

export interface UpdateWebhookRequest {
  webhook_id: number;
  webhook_name?: string;
  event_type?: string;
  url?: string;
  secret?: string;
  enabled?: boolean;
  headers?: Record<string, string>;
  retry_config?: {
    maxAttempts: number;
    backoffMs: number;
  };
}

export type EventType =
  | 'policy/bound'
  | 'claim/opened'
  | 'claim/settled'
  | 'agreement/updated'
  | '*';
