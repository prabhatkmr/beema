'use client';

import * as React from 'react';
import { Input } from '../Input';
import { Label } from '../Label';
import { FieldDefinition } from '../../types/layout';

interface DatePickerWidgetProps {
  field: FieldDefinition;
  value: any;
  onChange: (value: any) => void;
  disabled?: boolean;
}

export function DatePickerWidget({ field, value, onChange, disabled }: DatePickerWidgetProps) {
  return (
    <div className="space-y-2">
      <Label required={field.required}>
        {field.label}
      </Label>
      <Input
        type="date"
        value={value || ''}
        onChange={(e) => onChange(e.target.value)}
        disabled={disabled}
        required={field.required}
      />
    </div>
  );
}
