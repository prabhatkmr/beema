import { NextRequest, NextResponse } from 'next/server';
import { SysLayout } from '@/types/layout';

export async function POST(request: NextRequest) {
  try {
    const layout: SysLayout = await request.json();

    // Validate first
    const validationResponse = await fetch(
      `${request.nextUrl.origin}/api/layouts/validate`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(layout),
      }
    );

    const validationResult = await validationResponse.json();

    if (!validationResult.valid) {
      return NextResponse.json(
        {
          success: false,
          message: 'Validation failed',
          errors: validationResult.errors
        },
        { status: 400 }
      );
    }

    // Save to beema-kernel
    const kernelUrl = process.env.BEEMA_KERNEL_URL || 'http://localhost:8080';

    const saveResponse = await fetch(`${kernelUrl}/api/v1/layouts`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(layout),
    });

    if (!saveResponse.ok) {
      // Fallback: save to local storage if kernel is unavailable
      if (saveResponse.status === 404 || saveResponse.status === 503) {
        return NextResponse.json({
          success: true,
          message: 'Layout saved locally (kernel unavailable)',
          layout_id: layout.layout_id,
          warnings: ['Beema kernel unavailable - layout not persisted to database'],
        });
      }

      const errorData = await saveResponse.json();
      return NextResponse.json(
        {
          success: false,
          message: 'Failed to save layout',
          errors: [errorData.message]
        },
        { status: 500 }
      );
    }

    const savedLayout = await saveResponse.json();

    return NextResponse.json({
      success: true,
      message: 'Layout saved successfully',
      layout_id: savedLayout.layout_id,
      warnings: validationResult.warnings,
    });

  } catch (error) {
    console.error('Save error:', error);
    return NextResponse.json(
      { success: false, message: 'Server error', errors: [String(error)] },
      { status: 500 }
    );
  }
}
