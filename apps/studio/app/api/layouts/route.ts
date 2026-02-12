import { NextRequest, NextResponse } from 'next/server';

export async function GET(request: NextRequest) {
  try {
    const kernelUrl = process.env.BEEMA_KERNEL_URL || 'http://localhost:8080';

    const searchParams = request.nextUrl.searchParams;
    const marketContext = searchParams.get('marketContext');
    const layoutType = searchParams.get('layoutType');

    let url = `${kernelUrl}/api/v1/layouts`;
    const params = new URLSearchParams();

    if (marketContext) params.append('marketContext', marketContext);
    if (layoutType) params.append('layoutType', layoutType);

    if (params.toString()) {
      url += `?${params.toString()}`;
    }

    const response = await fetch(url, {
      headers: {
        'Content-Type': 'application/json',
      },
    });

    if (!response.ok) {
      // Return empty array if kernel is unavailable
      if (response.status === 404 || response.status === 503) {
        return NextResponse.json({
          layouts: [],
          message: 'Beema kernel unavailable',
        });
      }

      return NextResponse.json(
        { layouts: [], error: 'Failed to fetch layouts' },
        { status: 500 }
      );
    }

    const layouts = await response.json();

    return NextResponse.json({
      layouts,
      count: layouts.length,
    });

  } catch (error) {
    console.error('Fetch error:', error);
    return NextResponse.json(
      { layouts: [], error: 'Server error' },
      { status: 500 }
    );
  }
}
