import { inngest } from '@/lib/inngest/client';
import axios from 'axios';
import crypto from 'crypto';

interface Webhook {
  webhook_id: number;
  webhook_name: string;
  url: string;
  secret: string;
  headers: Record<string, string>;
  retry_config: {
    maxAttempts: number;
    backoffMs: number;
  };
}

interface WebhookDelivery {
  webhook_id: number;
  event_id: string;
  event_type: string;
  status: 'success' | 'failed' | 'retrying';
  status_code?: number;
  response_body?: string;
  error_message?: string;
  attempt_number: number;
}

// Webhook dispatcher function
export const webhookDispatcher = inngest.createFunction(
  {
    id: 'webhook-dispatcher',
    name: 'Webhook Dispatcher',
    retries: 3,
  },
  { event: '*' },  // Listen to all events
  async ({ event, step }) => {
    const eventType = event.name;
    const eventData = event.data;
    const tenantId = eventData.tenantId || 'default';

    // Step 1: Fetch matching webhooks from database
    const webhooks = await step.run('fetch-webhooks', async () => {
      const response = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/webhooks/match`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          eventType,
          tenantId,
        }),
      });

      if (!response.ok) {
        throw new Error(`Failed to fetch webhooks: ${response.statusText}`);
      }

      return response.json() as Promise<Webhook[]>;
    });

    if (!webhooks || webhooks.length === 0) {
      return { message: 'No webhooks configured for this event', eventType };
    }

    // Step 2: Fan out to all matching webhooks
    const deliveryResults = await Promise.allSettled(
      webhooks.map((webhook) =>
        step.run(`deliver-to-${webhook.webhook_id}`, async () => {
          return deliverWebhook(webhook, event, eventType);
        })
      )
    );

    // Step 3: Record delivery results
    await step.run('record-deliveries', async () => {
      const deliveries = deliveryResults.map((result, index) => {
        const webhook = webhooks[index];

        if (result.status === 'fulfilled') {
          return {
            webhook_id: webhook.webhook_id,
            event_id: event.id,
            event_type: eventType,
            status: 'success' as const,
            status_code: result.value.statusCode,
            response_body: result.value.responseBody,
            attempt_number: 1,
          };
        } else {
          return {
            webhook_id: webhook.webhook_id,
            event_id: event.id,
            event_type: eventType,
            status: 'failed' as const,
            error_message: result.reason.message,
            attempt_number: 1,
          };
        }
      });

      await fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/webhooks/deliveries`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ deliveries }),
      });

      return deliveries;
    });

    return {
      message: 'Webhooks dispatched',
      eventType,
      webhooksCount: webhooks.length,
      successCount: deliveryResults.filter(r => r.status === 'fulfilled').length,
      failureCount: deliveryResults.filter(r => r.status === 'rejected').length,
    };
  }
);

async function deliverWebhook(
  webhook: Webhook,
  event: any,
  eventType: string
): Promise<{ statusCode: number; responseBody: string }> {
  // Generate HMAC signature
  const payload = JSON.stringify({
    event: eventType,
    data: event.data,
    user: event.user,
    timestamp: event.ts,
  });

  const signature = crypto
    .createHmac('sha256', webhook.secret)
    .update(payload)
    .digest('hex');

  // Make HTTP request
  try {
    const response = await axios.post(webhook.url, payload, {
      headers: {
        'Content-Type': 'application/json',
        'X-Beema-Signature': `sha256=${signature}`,
        'X-Beema-Event': eventType,
        'X-Beema-Delivery': crypto.randomUUID(),
        ...webhook.headers,
      },
      timeout: 30000,
    });

    return {
      statusCode: response.status,
      responseBody: JSON.stringify(response.data).substring(0, 1000),
    };
  } catch (error: any) {
    if (error.response) {
      throw new Error(
        `Webhook delivery failed: ${error.response.status} ${error.response.statusText}`
      );
    } else {
      throw new Error(`Webhook delivery failed: ${error.message}`);
    }
  }
}
