import React from 'react';
import { X, Plus, Trash2 } from 'lucide-react';
import { FieldDefinition, SelectOption } from './types';
import { Input } from '../ui/Input';
import { Button } from '../ui/Button';

interface FieldPropertiesPanelProps {
  selectedField: FieldDefinition | null;
  onUpdate: (updates: Partial<FieldDefinition>) => void;
  onClose: () => void;
}

export const FieldPropertiesPanel: React.FC<FieldPropertiesPanelProps> = ({
  selectedField,
  onUpdate,
  onClose,
}) => {
  if (!selectedField) {
    return (
      <div className="w-80 bg-white border-l border-gray-200 p-6">
        <div className="flex flex-col items-center justify-center h-full text-center text-gray-400">
          <svg
            className="w-12 h-12 mb-3"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={1.5}
              d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
            />
          </svg>
          <p className="text-sm font-medium">No field selected</p>
          <p className="text-xs mt-1">Select a field to edit its properties</p>
        </div>
      </div>
    );
  }

  const addOption = () => {
    const currentOptions = selectedField.options || [];
    const newOption: SelectOption = {
      label: `Option ${currentOptions.length + 1}`,
      value: `option${currentOptions.length + 1}`,
    };
    onUpdate({ options: [...currentOptions, newOption] });
  };

  const updateOption = (index: number, updates: Partial<SelectOption>) => {
    const newOptions = [...(selectedField.options || [])];
    newOptions[index] = { ...newOptions[index], ...updates };
    onUpdate({ options: newOptions });
  };

  const deleteOption = (index: number) => {
    const newOptions = (selectedField.options || []).filter((_, i) => i !== index);
    onUpdate({ options: newOptions });
  };

  const showOptions = selectedField.type === 'select' || selectedField.type === 'radio';

  return (
    <div className="w-80 bg-white border-l border-gray-200 overflow-y-auto">
      <div className="sticky top-0 bg-white border-b border-gray-200 p-4 flex items-center justify-between z-10">
        <h3 className="font-semibold text-gray-800">Field Properties</h3>
        <button
          onClick={onClose}
          className="p-1 text-gray-400 hover:text-gray-600 rounded hover:bg-gray-100"
          title="Close panel"
        >
          <X className="w-5 h-5" />
        </button>
      </div>

      <div className="p-4 space-y-4">
        {/* Field Type */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Field Type</label>
          <div className="px-3 py-2 bg-gray-50 border border-gray-200 rounded-md text-sm text-gray-600">
            {selectedField.type}
          </div>
        </div>

        {/* Label */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Label</label>
          <Input
            type="text"
            value={selectedField.label}
            onChange={(e) => onUpdate({ label: e.target.value })}
            placeholder="Field label"
          />
        </div>

        {/* Field Name */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Field Name</label>
          <Input
            type="text"
            value={selectedField.name}
            onChange={(e) => onUpdate({ name: e.target.value })}
            placeholder="fieldName"
          />
          <p className="text-xs text-gray-500 mt-1">Used as the key in form data</p>
        </div>

        {/* Placeholder (for text-based inputs) */}
        {(selectedField.type === 'text' ||
          selectedField.type === 'number' ||
          selectedField.type === 'textarea') && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1">Placeholder</label>
            <Input
              type="text"
              value={selectedField.placeholder || ''}
              onChange={(e) => onUpdate({ placeholder: e.target.value })}
              placeholder="Enter placeholder text..."
            />
          </div>
        )}

        {/* Required */}
        <div>
          <label className="flex items-center cursor-pointer">
            <input
              type="checkbox"
              checked={selectedField.required}
              onChange={(e) => onUpdate({ required: e.target.checked })}
              className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
            />
            <span className="ml-2 text-sm font-medium text-gray-700">Required field</span>
          </label>
          <p className="text-xs text-gray-500 mt-1 ml-6">
            User must provide a value for this field
          </p>
        </div>

        {/* Options (for select and radio) */}
        {showOptions && (
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">Options</label>
            <div className="space-y-2">
              {(selectedField.options || []).map((option, index) => (
                <div key={index} className="flex gap-2">
                  <Input
                    type="text"
                    value={option.label}
                    onChange={(e) => updateOption(index, { label: e.target.value })}
                    placeholder="Label"
                    className="flex-1"
                  />
                  <Input
                    type="text"
                    value={option.value}
                    onChange={(e) => updateOption(index, { value: e.target.value })}
                    placeholder="Value"
                    className="flex-1"
                  />
                  <button
                    onClick={() => deleteOption(index)}
                    className="p-2 text-red-600 hover:bg-red-50 rounded transition-colors"
                    title="Delete option"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              ))}
            </div>
            <Button
              onClick={addOption}
              variant="outline"
              className="w-full mt-2 flex items-center justify-center gap-2"
            >
              <Plus className="w-4 h-4" />
              Add Option
            </Button>
          </div>
        )}

        {/* Default Value */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Default Value</label>
          {selectedField.type === 'checkbox' ? (
            <label className="flex items-center cursor-pointer">
              <input
                type="checkbox"
                checked={selectedField.defaultValue || false}
                onChange={(e) => onUpdate({ defaultValue: e.target.checked })}
                className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
              />
              <span className="ml-2 text-sm text-gray-600">Checked by default</span>
            </label>
          ) : (
            <Input
              type={selectedField.type === 'number' ? 'number' : 'text'}
              value={selectedField.defaultValue || ''}
              onChange={(e) => onUpdate({ defaultValue: e.target.value })}
              placeholder="Default value..."
            />
          )}
        </div>

        {/* Validation Rules Section */}
        <div className="pt-4 border-t border-gray-200">
          <h4 className="text-sm font-semibold text-gray-700 mb-2">Validation</h4>
          <p className="text-xs text-gray-500 mb-3">Configure validation rules for this field</p>

          {/* Min/Max for number fields */}
          {selectedField.type === 'number' && (
            <div className="space-y-2">
              <div>
                <label className="block text-xs font-medium text-gray-600 mb-1">
                  Minimum Value
                </label>
                <Input type="number" placeholder="Min value..." />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-600 mb-1">
                  Maximum Value
                </label>
                <Input type="number" placeholder="Max value..." />
              </div>
            </div>
          )}

          {/* Min/Max length for text fields */}
          {selectedField.type === 'text' && (
            <div className="space-y-2">
              <div>
                <label className="block text-xs font-medium text-gray-600 mb-1">
                  Minimum Length
                </label>
                <Input type="number" placeholder="Min length..." />
              </div>
              <div>
                <label className="block text-xs font-medium text-gray-600 mb-1">
                  Maximum Length
                </label>
                <Input type="number" placeholder="Max length..." />
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
