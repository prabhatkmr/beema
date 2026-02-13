import { NextRequest, NextResponse } from 'next/server';

const KERNEL_URL = process.env.BEEMA_KERNEL_URL || 'http://localhost:8080';
const TENANT_ID = process.env.DEFAULT_TENANT_ID || 'default';

export async function POST(
  _request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  const { id } = await params;

  try {
    const response = await fetch(`${KERNEL_URL}/api/v1/schedules/${id}/unpause`, {
      method: 'POST',
      headers: { 'X-Tenant-ID': TENANT_ID },
    });

    if (!response.ok) {
      const error = await response.json().catch(() => ({}));
      return NextResponse.json(
        { error: 'Failed to unpause schedule', details: error },
        { status: response.status }
      );
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error('Error unpausing schedule:', error);
    return NextResponse.json(
      { error: 'Failed to connect to kernel service' },
      { status: 503 }
    );
  }
}
