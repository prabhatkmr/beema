import { NextRequest, NextResponse } from 'next/server';

const KERNEL_URL = process.env.BEEMA_KERNEL_URL || 'http://localhost:8080';
const TENANT_ID = process.env.DEFAULT_TENANT_ID || 'default';

export async function POST(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  const { id } = await params;

  try {
    const body = await request.json().catch(() => ({}));
    const response = await fetch(`${KERNEL_URL}/api/v1/schedules/${id}/trigger`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Tenant-ID': TENANT_ID,
      },
      body: JSON.stringify(body),
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      return NextResponse.json(
        { error: 'Failed to trigger schedule', details: error },
        { status: response.status }
      );
    }

    return NextResponse.json({ message: 'Schedule triggered successfully' });
  } catch (error) {
    console.error('Error triggering schedule:', error);
    return NextResponse.json(
      { error: 'Failed to connect to kernel service' },
      { status: 503 }
    );
  }
}
