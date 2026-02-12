import { NextRequest, NextResponse } from 'next/server';

export async function POST(request: NextRequest) {
  try {
    const { eventType, tenantId } = await request.json();

    // Query sys_webhooks table
    // This should connect to beema-kernel database or use a shared DB
    // For now, return mock data
    // TODO: Replace with actual database query
    const query = `
      SELECT webhook_id, webhook_name, url, secret, headers, retry_config
      FROM sys_webhooks
      WHERE enabled = true
        AND tenant_id = $1
        AND (event_type = $2 OR event_type = '*')
    `;

    // Mock webhooks for testing
    const webhooks = [
      {
        webhook_id: 1,
        webhook_name: 'Test Webhook',
        url: 'https://webhook.site/your-unique-url',
        secret: 'test-secret-key',
        headers: { 'X-Custom-Header': 'value' },
        retry_config: { maxAttempts: 3, backoffMs: 1000 },
      },
    ];

    console.log(`[Webhook Match] Found ${webhooks.length} webhooks for event: ${eventType}, tenant: ${tenantId}`);

    return NextResponse.json(webhooks);
  } catch (error: any) {
    console.error('[Webhook Match] Error:', error);
    return NextResponse.json(
      { error: 'Failed to fetch webhooks', message: error.message },
      { status: 500 }
    );
  }
}
