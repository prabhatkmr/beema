import { NextRequest, NextResponse } from 'next/server';

// GET /api/webhooks - List all webhooks
export async function GET(request: NextRequest) {
  try {
    const tenantId = request.headers.get('X-Tenant-ID') || 'default';

    // Query database
    // TODO: Replace with actual database query
    console.log(`[Webhooks] Fetching webhooks for tenant: ${tenantId}`);

    const webhooks = [
      {
        webhook_id: 1,
        webhook_name: 'Slack Notifications',
        event_type: 'claim/opened',
        url: 'https://hooks.slack.com/services/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXX',
        secret: 'whsec_abc123def456ghi789jkl012mno345pqr678stu901vwx234yz',
        enabled: true,
        headers: {},
        created_at: new Date().toISOString(),
      },
      {
        webhook_id: 2,
        webhook_name: 'Policy Bound Handler',
        event_type: 'policy/bound',
        url: 'https://api.example.com/webhooks/policy',
        secret: 'whsec_def456ghi789jkl012mno345pqr678stu901vwx234yzabc123',
        enabled: true,
        headers: { 'Authorization': 'Bearer token123' },
        created_at: new Date().toISOString(),
      },
    ];

    return NextResponse.json({ webhooks });
  } catch (error: any) {
    console.error('[Webhooks] Error fetching webhooks:', error);
    return NextResponse.json(
      { error: 'Failed to fetch webhooks', message: error.message },
      { status: 500 }
    );
  }
}

// POST /api/webhooks - Create webhook
export async function POST(request: NextRequest) {
  try {
    const webhook = await request.json();

    // Validate required fields
    if (!webhook.webhook_name || !webhook.event_type || !webhook.url || !webhook.secret) {
      return NextResponse.json(
        { error: 'Missing required fields: webhook_name, event_type, url, secret' },
        { status: 400 }
      );
    }

    const tenantId = request.headers.get('X-Tenant-ID') || 'default';

    // Insert into database
    // TODO: Replace with actual database insertion
    console.log('[Webhooks] Creating webhook:', webhook);

    const newWebhook = {
      webhook_id: Math.floor(Math.random() * 10000),
      tenant_id: tenantId,
      created_by: 'system',
      enabled: webhook.enabled !== undefined ? webhook.enabled : true,
      headers: webhook.headers || {},
      retry_config: webhook.retry_config || { maxAttempts: 3, backoffMs: 1000 },
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
      ...webhook,
    };

    console.log('[Webhooks] Created webhook:', newWebhook);

    return NextResponse.json(newWebhook, { status: 201 });
  } catch (error: any) {
    console.error('[Webhooks] Error creating webhook:', error);
    return NextResponse.json(
      { error: 'Failed to create webhook', message: error.message },
      { status: 500 }
    );
  }
}

// PUT /api/webhooks/:id - Update webhook
export async function PUT(request: NextRequest) {
  try {
    const webhook = await request.json();
    const webhookId = webhook.webhook_id;

    if (!webhookId) {
      return NextResponse.json(
        { error: 'Missing webhook_id' },
        { status: 400 }
      );
    }

    // Update in database
    // TODO: Replace with actual database update
    console.log('[Webhooks] Updating webhook:', webhookId, webhook);

    const updatedWebhook = {
      ...webhook,
      updated_at: new Date().toISOString(),
    };

    console.log('[Webhooks] Updated webhook:', updatedWebhook);

    return NextResponse.json(updatedWebhook);
  } catch (error: any) {
    console.error('[Webhooks] Error updating webhook:', error);
    return NextResponse.json(
      { error: 'Failed to update webhook', message: error.message },
      { status: 500 }
    );
  }
}

// DELETE /api/webhooks/:id - Delete webhook
export async function DELETE(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url);
    const webhookId = searchParams.get('id');

    if (!webhookId) {
      return NextResponse.json(
        { error: 'Missing webhook id' },
        { status: 400 }
      );
    }

    // Delete from database
    // TODO: Replace with actual database deletion
    console.log('[Webhooks] Deleting webhook:', webhookId);

    return NextResponse.json({ success: true, message: 'Webhook deleted' });
  } catch (error: any) {
    console.error('[Webhooks] Error deleting webhook:', error);
    return NextResponse.json(
      { error: 'Failed to delete webhook', message: error.message },
      { status: 500 }
    );
  }
}
