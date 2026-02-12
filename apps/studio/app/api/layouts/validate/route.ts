import { NextRequest, NextResponse } from 'next/server';
import { SysLayout } from '@/types/layout';

export async function POST(request: NextRequest) {
  try {
    const layout: SysLayout = await request.json();

    // Step 1: Local validation
    const localValidation = validateLayoutLocally(layout);
    if (!localValidation.valid) {
      return NextResponse.json(
        { valid: false, errors: localValidation.errors },
        { status: 400 }
      );
    }

    // Step 2: Validate against beema-kernel metadata schema
    const kernelValidation = await validateAgainstKernelSchema(layout);

    if (!kernelValidation.valid) {
      return NextResponse.json(
        {
          valid: false,
          errors: kernelValidation.errors,
          warnings: kernelValidation.warnings
        },
        { status: 400 }
      );
    }

    return NextResponse.json({
      valid: true,
      message: 'Layout validated successfully',
      metadata: {
        field_count: layout.fields.length,
        layout_type: layout.layout_type,
        market_context: layout.market_context,
      },
    });

  } catch (error) {
    console.error('Validation error:', error);
    return NextResponse.json(
      { valid: false, errors: ['Invalid JSON or server error'] },
      { status: 500 }
    );
  }
}

function validateLayoutLocally(layout: SysLayout): { valid: boolean; errors: string[] } {
  const errors: string[] = [];

  // Check required fields
  if (!layout.layout_name || layout.layout_name.trim() === '') {
    errors.push('Layout name is required');
  }

  if (!layout.layout_type) {
    errors.push('Layout type is required');
  }

  if (!layout.market_context) {
    errors.push('Market context is required');
  }

  if (!Array.isArray(layout.fields)) {
    errors.push('Fields must be an array');
  } else {
    // Validate each field
    layout.fields.forEach((field, index) => {
      if (!field.type) {
        errors.push(`Field ${index + 1}: type is required`);
      }

      if (!field.name || field.name.trim() === '') {
        errors.push(`Field ${index + 1}: name is required`);
      }

      if (!field.label || field.label.trim() === '') {
        errors.push(`Field ${index + 1}: label is required`);
      }

      // Validate field name format (alphanumeric + underscores only)
      if (field.name && !/^[a-z_][a-z0-9_]*$/i.test(field.name)) {
        errors.push(
          `Field ${index + 1}: name must be alphanumeric with underscores (${field.name})`
        );
      }

      // Check for duplicate field names
      const duplicates = layout.fields.filter(f => f.name === field.name);
      if (duplicates.length > 1) {
        errors.push(`Field ${index + 1}: duplicate field name "${field.name}"`);
      }

      // Validate select/radio options
      if ((field.type === 'select' || field.type === 'radio') && (!field.options || field.options.length === 0)) {
        errors.push(`Field ${index + 1}: ${field.type} requires at least one option`);
      }
    });
  }

  return { valid: errors.length === 0, errors };
}

async function validateAgainstKernelSchema(layout: SysLayout): Promise<{
  valid: boolean;
  errors: string[];
  warnings: string[]
}> {
  const errors: string[] = [];
  const warnings: string[] = [];

  try {
    // Get beema-kernel URL from environment
    const kernelUrl = process.env.BEEMA_KERNEL_URL || 'http://localhost:8080';

    // Call beema-kernel metadata validation endpoint
    const response = await fetch(`${kernelUrl}/api/v1/metadata/validate-layout`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(layout),
    });

    if (!response.ok) {
      // If kernel is not available, return warnings instead of errors
      if (response.status === 404 || response.status === 503) {
        warnings.push('Beema kernel validation unavailable - layout saved with local validation only');
        return { valid: true, errors: [], warnings };
      }

      const errorData = await response.json();
      errors.push(...(errorData.errors || ['Kernel validation failed']));
      return { valid: false, errors, warnings };
    }

    const validationResult = await response.json();

    if (!validationResult.valid) {
      errors.push(...(validationResult.errors || []));
    }

    if (validationResult.warnings) {
      warnings.push(...validationResult.warnings);
    }

    return { valid: validationResult.valid, errors, warnings };

  } catch (error) {
    // Network error - kernel might not be running
    console.warn('Failed to connect to beema-kernel:', error);
    warnings.push('Could not connect to beema-kernel - layout validated locally only');
    return { valid: true, errors: [], warnings };
  }
}
