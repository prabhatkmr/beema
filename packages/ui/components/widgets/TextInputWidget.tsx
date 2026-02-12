import * as React from 'react';
import { Input } from '../Input';
import { Label } from '../Label';
import { FieldDefinition } from '../../types/layout';

interface TextInputWidgetProps {
  field: FieldDefinition;
  value: any;
  onChange: (value: any) => void;
  disabled?: boolean;
}

export function TextInputWidget({ field, value, onChange, disabled }: TextInputWidgetProps) {
  return (
    <div className="space-y-2">
      <Label required={field.required}>
        {field.label}
      </Label>
      <Input
        type="text"
        value={value || ''}
        onChange={(e) => onChange(e.target.value)}
        placeholder={field.placeholder}
        disabled={disabled}
        required={field.required}
        minLength={field.validation?.minLength}
        maxLength={field.validation?.maxLength}
      />
    </div>
  );
}
