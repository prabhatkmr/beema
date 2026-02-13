import { NextRequest, NextResponse } from 'next/server';

const KERNEL_URL = process.env.BEEMA_KERNEL_URL || 'http://localhost:8080';
const TENANT_ID = process.env.DEFAULT_TENANT_ID || 'default';

export async function GET() {
  try {
    const response = await fetch(`${KERNEL_URL}/api/v1/schedules`, {
      headers: { 'X-Tenant-ID': TENANT_ID },
    });

    if (!response.ok) {
      return NextResponse.json(
        { error: 'Failed to fetch schedules' },
        { status: response.status }
      );
    }

    const data = await response.json();
    return NextResponse.json(data);
  } catch (error) {
    console.error('Error fetching schedules:', error);
    return NextResponse.json(
      { error: 'Failed to connect to kernel service' },
      { status: 503 }
    );
  }
}

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const response = await fetch(`${KERNEL_URL}/api/v1/schedules`, {
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
        { error: 'Failed to create schedule', details: error },
        { status: response.status }
      );
    }

    const data = await response.json();
    return NextResponse.json(data, { status: 201 });
  } catch (error) {
    console.error('Error creating schedule:', error);
    return NextResponse.json(
      { error: 'Failed to connect to kernel service' },
      { status: 503 }
    );
  }
}
