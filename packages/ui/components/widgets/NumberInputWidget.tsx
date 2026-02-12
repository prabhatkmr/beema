'use client';

import * as React from 'react';
import { Input } from '../Input';
import { Label } from '../Label';
import { FieldDefinition } from '../../types/layout';

interface NumberInputWidgetProps {
  field: FieldDefinition;
  value: any;
  onChange: (value: any) => void;
  disabled?: boolean;
}

export function NumberInputWidget({ field, value, onChange, disabled }: NumberInputWidgetProps) {
  return (
    <div className="space-y-2">
      <Label required={field.required}>
        {field.label}
      </Label>
      <Input
        type="number"
        value={value || ''}
        onChange={(e) => onChange(parseFloat(e.target.value) || null)}
        placeholder={field.placeholder}
        disabled={disabled}
        required={field.required}
        min={field.validation?.min}
        max={field.validation?.max}
      />
    </div>
  );
}
