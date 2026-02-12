'use client';

import * as React from 'react';
import { FieldDefinition } from '../../types/layout';

interface CheckboxWidgetProps {
  field: FieldDefinition;
  value: any;
  onChange: (value: any) => void;
  disabled?: boolean;
}

export function CheckboxWidget({ field, value, onChange, disabled }: CheckboxWidgetProps) {
  return (
    <div className="flex items-center space-x-2">
      <input
        type="checkbox"
        id={field.id}
        checked={!!value}
        onChange={(e) => onChange(e.target.checked)}
        disabled={disabled}
        className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-2 focus:ring-blue-500"
      />
      <label
        htmlFor={field.id}
        className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
      >
        {field.label}
        {field.required && <span className="text-red-500 ml-1">*</span>}
      </label>
    </div>
  );
}
