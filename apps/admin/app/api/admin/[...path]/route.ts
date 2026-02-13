import { NextRequest, NextResponse } from 'next/server';

const KERNEL_URL = process.env.BEEMA_KERNEL_URL || 'http://localhost:8080';

async function proxyRequest(request: NextRequest, params: Promise<{ path: string[] }>) {
  const { path } = await params;
  const targetPath = path.join('/');
  const url = new URL(request.url);
  const targetUrl = `${KERNEL_URL}/api/v1/admin/${targetPath}${url.search}`;

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };

  const fetchOptions: RequestInit = {
    method: request.method,
    headers,
  };

  if (request.method !== 'GET' && request.method !== 'HEAD') {
    const body = await request.text();
    if (body) fetchOptions.body = body;
  }

  try {
    const response = await fetch(targetUrl, fetchOptions);
    const data = await response.text();

    return new NextResponse(data, {
      status: response.status,
      headers: { 'Content-Type': 'application/json' },
    });
  } catch (error) {
    console.error('Proxy error:', error);
    return NextResponse.json(
      { message: 'Failed to connect to backend service' },
      { status: 502 }
    );
  }
}

export async function GET(request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  return proxyRequest(request, context.params);
}

export async function POST(request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  return proxyRequest(request, context.params);
}

export async function PUT(request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  return proxyRequest(request, context.params);
}

export async function DELETE(request: NextRequest, context: { params: Promise<{ path: string[] }> }) {
  return proxyRequest(request, context.params);
}
