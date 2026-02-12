import React from 'react';
import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { GripVertical, Edit2, Trash2 } from 'lucide-react';
import { FieldDefinition } from './types';

interface DroppableFieldProps {
  field: FieldDefinition;
  isSelected: boolean;
  onSelect: (id: string) => void;
  onDelete: (id: string) => void;
}

const renderFieldPreview = (field: FieldDefinition) => {
  const baseInputClass = 'w-full px-3 py-2 border border-gray-300 rounded-md text-sm';

  switch (field.type) {
    case 'text':
    case 'number':
      return (
        <input
          type={field.type}
          placeholder={field.placeholder}
          disabled
          className={baseInputClass}
        />
      );
    case 'date':
      return <input type="date" disabled className={baseInputClass} />;
    case 'select':
      return (
        <select disabled className={baseInputClass}>
          <option>Select an option...</option>
          {field.options?.map((opt) => (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          ))}
        </select>
      );
    case 'checkbox':
      return (
        <div className="flex items-center">
          <input type="checkbox" disabled className="mr-2" />
          <span className="text-sm text-gray-600">Checkbox option</span>
        </div>
      );
    case 'radio':
      return (
        <div className="space-y-2">
          {field.options?.map((opt) => (
            <div key={opt.value} className="flex items-center">
              <input
                type="radio"
                name={field.name}
                value={opt.value}
                disabled
                className="mr-2"
              />
              <span className="text-sm text-gray-600">{opt.label}</span>
            </div>
          ))}
        </div>
      );
    case 'textarea':
      return (
        <textarea
          placeholder={field.placeholder}
          disabled
          rows={3}
          className={baseInputClass}
        />
      );
    case 'file':
      return (
        <input
          type="file"
          disabled
          className="w-full text-sm text-gray-500 file:mr-4 file:py-2 file:px-4 file:rounded-md file:border-0 file:text-sm file:font-semibold file:bg-blue-50 file:text-blue-700"
        />
      );
    default:
      return <div className="text-sm text-gray-400">Unknown field type</div>;
  }
};

export const DroppableField: React.FC<DroppableFieldProps> = ({
  field,
  isSelected,
  onSelect,
  onDelete,
}) => {
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: field.id,
  });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
    opacity: isDragging ? 0.5 : 1,
  };

  const handleDelete = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (window.confirm(`Delete field "${field.label}"?`)) {
      onDelete(field.id);
    }
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      onClick={() => onSelect(field.id)}
      className={`bg-white p-4 mb-3 rounded-lg shadow-sm border-2 transition-all cursor-pointer ${
        isSelected
          ? 'border-blue-500 ring-2 ring-blue-200'
          : 'border-gray-200 hover:border-blue-300'
      }`}
    >
      <div className="flex items-start gap-3">
        <div
          {...attributes}
          {...listeners}
          className="cursor-move text-gray-400 hover:text-gray-600 pt-1"
        >
          <GripVertical className="w-5 h-5" />
        </div>
        <div className="flex-1 min-w-0">
          <div className="flex justify-between items-start mb-3">
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2">
                <h3 className="font-medium text-gray-800 truncate">{field.label}</h3>
                {field.required && (
                  <span className="text-red-500 text-sm font-medium">*</span>
                )}
              </div>
              <div className="flex items-center gap-2 mt-1">
                <span className="text-xs text-gray-500 bg-gray-100 px-2 py-0.5 rounded">
                  {field.type}
                </span>
                <span className="text-xs text-gray-400">{field.name}</span>
              </div>
            </div>
            <div className="flex items-center gap-1 ml-2">
              <button
                onClick={() => onSelect(field.id)}
                className="p-1.5 text-blue-600 hover:bg-blue-50 rounded transition-colors"
                title="Edit field"
              >
                <Edit2 className="w-4 h-4" />
              </button>
              <button
                onClick={handleDelete}
                className="p-1.5 text-red-600 hover:bg-red-50 rounded transition-colors"
                title="Delete field"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          </div>
          <div className="pointer-events-none">{renderFieldPreview(field)}</div>
        </div>
      </div>
    </div>
  );
};
