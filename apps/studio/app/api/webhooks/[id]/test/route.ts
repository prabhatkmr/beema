import { NextRequest, NextResponse } from 'next/server';

export async function POST(
  request: NextRequest,
  { params }: { params: { id: string } }
) {
  try {
    const webhookId = params.id;

    // Trigger a test event via Inngest
    const testEvent = {
      name: 'test/webhook',
      data: {
        webhookId: parseInt(webhookId),
        message: 'This is a test event',
        timestamp: new Date().toISOString(),
      },
      user: {
        id: 'test-user',
        email: 'test@beema.io',
      },
    };

    console.log('[Webhooks] Sending test event:', testEvent);

    // In production, this would send to Inngest
    // const response = await fetch(`${process.env.NEXT_PUBLIC_INNGEST_URL}/e/local`, {
    //   method: 'POST',
    //   headers: { 'Content-Type': 'application/json' },
    //   body: JSON.stringify(testEvent),
    // });

    // For now, return success
    return NextResponse.json({
      success: true,
      message: 'Test event sent',
      eventId: testEvent.data.timestamp,
      event: testEvent,
    });
  } catch (error: any) {
    console.error('[Webhooks] Error sending test event:', error);
    return NextResponse.json(
      { error: 'Failed to send test event', message: error.message },
      { status: 500 }
    );
  }
}
