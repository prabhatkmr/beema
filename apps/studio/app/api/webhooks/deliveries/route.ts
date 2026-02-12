import { NextRequest, NextResponse } from 'next/server';

export async function POST(request: NextRequest) {
  try {
    const { deliveries } = await request.json();

    // Insert into sys_webhook_deliveries table
    // TODO: Replace with actual database insertion
    console.log('[Webhook Deliveries] Recording webhook deliveries:', deliveries);

    // Mock successful insert
    const insertedCount = deliveries.length;

    console.log(`[Webhook Deliveries] Recorded ${insertedCount} deliveries`);

    return NextResponse.json({ success: true, count: insertedCount });
  } catch (error: any) {
    console.error('[Webhook Deliveries] Error:', error);
    return NextResponse.json(
      { error: 'Failed to record webhook deliveries', message: error.message },
      { status: 500 }
    );
  }
}

export async function GET(request: NextRequest) {
  try {
    const { searchParams } = new URL(request.url);
    const webhookId = searchParams.get('webhook_id');
    const eventId = searchParams.get('event_id');
    const status = searchParams.get('status');
    const limit = parseInt(searchParams.get('limit') || '10');

    // Query sys_webhook_deliveries table
    // TODO: Replace with actual database query
    console.log('[Webhook Deliveries] Fetching deliveries with filters:', { webhookId, eventId, status, limit });

    // Mock deliveries - generate some sample data
    const mockDeliveries = [
      {
        delivery_id: 1,
        webhook_id: 1,
        event_id: 'evt_123',
        event_type: 'claim/opened',
        status: 'success',
        status_code: 200,
        response_body: '{"success": true}',
        attempt_number: 1,
        delivered_at: new Date(Date.now() - 5000).toISOString(),
      },
      {
        delivery_id: 2,
        webhook_id: 2,
        event_id: 'evt_124',
        event_type: 'policy/bound',
        status: 'failed',
        status_code: 500,
        error_message: 'Connection timeout',
        attempt_number: 3,
        delivered_at: new Date(Date.now() - 15000).toISOString(),
      },
      {
        delivery_id: 3,
        webhook_id: 1,
        event_id: 'evt_125',
        event_type: 'claim/updated',
        status: 'success',
        status_code: 200,
        response_body: '{"success": true}',
        attempt_number: 1,
        delivered_at: new Date(Date.now() - 25000).toISOString(),
      },
    ];

    const deliveries = mockDeliveries.slice(0, limit);

    return NextResponse.json({ deliveries });
  } catch (error: any) {
    console.error('[Webhook Deliveries] Error:', error);
    return NextResponse.json(
      { error: 'Failed to fetch webhook deliveries', message: error.message },
      { status: 500 }
    );
  }
}
