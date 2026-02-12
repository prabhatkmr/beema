import { NextRequest, NextResponse } from 'next/server';

// PUT /api/webhooks/:id - Update specific webhook
export async function PUT(
  request: NextRequest,
  { params }: { params: { id: string } }
) {
  try {
    const webhookId = params.id;
    const updates = await request.json();

    // Update in database
    // TODO: Replace with actual database update
    console.log('[Webhooks] Updating webhook:', webhookId, updates);

    const updatedWebhook = {
      webhook_id: parseInt(webhookId),
      ...updates,
      updated_at: new Date().toISOString(),
    };

    console.log('[Webhooks] Updated webhook:', updatedWebhook);

    return NextResponse.json({
      success: true,
      webhook: updatedWebhook,
    });
  } catch (error: any) {
    console.error('[Webhooks] Error updating webhook:', error);
    return NextResponse.json(
      { error: 'Failed to update webhook', message: error.message },
      { status: 500 }
    );
  }
}

// DELETE /api/webhooks/:id - Delete specific webhook
export async function DELETE(
  request: NextRequest,
  { params }: { params: { id: string } }
) {
  try {
    const webhookId = params.id;

    // Delete from database
    // TODO: Replace with actual database deletion
    console.log('[Webhooks] Deleting webhook:', webhookId);

    return NextResponse.json({
      success: true,
      message: 'Webhook deleted',
      webhook_id: parseInt(webhookId),
    });
  } catch (error: any) {
    console.error('[Webhooks] Error deleting webhook:', error);
    return NextResponse.json(
      { error: 'Failed to delete webhook', message: error.message },
      { status: 500 }
    );
  }
}

// GET /api/webhooks/:id - Get specific webhook
export async function GET(
  request: NextRequest,
  { params }: { params: { id: string } }
) {
  try {
    const webhookId = params.id;

    // Fetch from database
    // TODO: Replace with actual database query
    console.log('[Webhooks] Fetching webhook:', webhookId);

    const webhook = {
      webhook_id: parseInt(webhookId),
      webhook_name: 'Example Webhook',
      event_type: 'claim/opened',
      url: 'https://example.com/webhook',
      secret: 'whsec_example',
      enabled: true,
      headers: {},
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    };

    return NextResponse.json({
      success: true,
      webhook,
    });
  } catch (error: any) {
    console.error('[Webhooks] Error fetching webhook:', error);
    return NextResponse.json(
      { error: 'Failed to fetch webhook', message: error.message },
      { status: 500 }
    );
  }
}
