'use client';

import * as React from 'react';
import { Label } from '../Label';
import { FieldDefinition } from '../../types/layout';

interface SelectWidgetProps {
  field: FieldDefinition;
  value: any;
  onChange: (value: any) => void;
  disabled?: boolean;
}

export function SelectWidget({ field, value, onChange, disabled }: SelectWidgetProps) {
  return (
    <div className="space-y-2">
      <Label required={field.required}>
        {field.label}
      </Label>
      <select
        value={value || ''}
        onChange={(e) => onChange(e.target.value)}
        disabled={disabled}
        required={field.required}
        className="flex h-10 w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
      >
        <option value="">Select...</option>
        {field.options?.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
    </div>
  );
}
