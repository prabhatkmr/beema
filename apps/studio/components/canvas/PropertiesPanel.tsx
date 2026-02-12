'use client';

import { FieldBlock, SelectOption } from '@/types/layout';
import { Button, Input, Label } from '@beema/ui';
import { useState } from 'react';

interface PropertiesPanelProps {
  field: FieldBlock | null;
  onUpdate: (updates: Partial<FieldBlock>) => void;
  onClose: () => void;
}

export function PropertiesPanel({ field, onUpdate, onClose }: PropertiesPanelProps) {
  const [options, setOptions] = useState<SelectOption[]>(field?.options || []);

  if (!field) {
    return (
      <div className="w-80 border-l bg-gray-50 p-6">
        <p className="text-gray-400 text-center mt-20">
          Select a field to edit properties
        </p>
      </div>
    );
  }

  const handleAddOption = () => {
    const newOptions = [...options, { label: 'New Option', value: `option${options.length + 1}` }];
    setOptions(newOptions);
    onUpdate({ options: newOptions });
  };

  const handleOptionChange = (index: number, key: 'label' | 'value', value: string) => {
    const newOptions = [...options];
    newOptions[index][key] = value;
    setOptions(newOptions);
    onUpdate({ options: newOptions });
  };

  const handleRemoveOption = (index: number) => {
    const newOptions = options.filter((_, i) => i !== index);
    setOptions(newOptions);
    onUpdate({ options: newOptions });
  };

  return (
    <div className="w-80 border-l bg-white p-6 overflow-y-auto">
      <div className="flex justify-between items-center mb-6">
        <h3 className="text-lg font-bold">Field Properties</h3>
        <button onClick={onClose} className="text-gray-500 hover:text-gray-700">
          ✕
        </button>
      </div>

      <div className="space-y-4">
        <div>
          <Label required>Label</Label>
          <Input
            value={field.label}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => onUpdate({ label: e.target.value })}
          />
        </div>

        <div>
          <Label required>Field Name</Label>
          <Input
            value={field.name}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => onUpdate({ name: e.target.value })}
          />
          <p className="text-xs text-gray-500 mt-1">Used for data binding</p>
        </div>

        <div>
          <Label>Placeholder</Label>
          <Input
            value={field.placeholder || ''}
            onChange={(e: React.ChangeEvent<HTMLInputElement>) => onUpdate({ placeholder: e.target.value })}
          />
        </div>

        <div>
          <Label>Width</Label>
          <select
            value={field.width || 'full'}
            onChange={(e: React.ChangeEvent<HTMLSelectElement>) => onUpdate({ width: e.target.value as any })}
            className="w-full border rounded px-3 py-2"
          >
            <option value="full">Full Width</option>
            <option value="half">Half Width</option>
            <option value="third">One Third</option>
            <option value="quarter">One Quarter</option>
          </select>
        </div>

        <div>
          <label className="flex items-center gap-2">
            <input
              type="checkbox"
              checked={field.required}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => onUpdate({ required: e.target.checked })}
            />
            <span className="text-sm font-medium">Required Field</span>
          </label>
        </div>

        {(field.type === 'select' || field.type === 'radio') && (
          <div>
            <Label>Options</Label>
            <div className="space-y-2 mt-2">
              {options.map((option, index) => (
                <div key={index} className="flex gap-2">
                  <Input
                    placeholder="Label"
                    value={option.label}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => handleOptionChange(index, 'label', e.target.value)}
                  />
                  <Input
                    placeholder="Value"
                    value={option.value}
                    onChange={(e: React.ChangeEvent<HTMLInputElement>) => handleOptionChange(index, 'value', e.target.value)}
                  />
                  <button
                    onClick={() => handleRemoveOption(index)}
                    className="text-red-500 hover:text-red-700"
                  >
                    ✕
                  </button>
                </div>
              ))}
            </div>
            <Button
              size="sm"
              variant="outline"
              onClick={handleAddOption}
              className="mt-2 w-full"
            >
              + Add Option
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}
