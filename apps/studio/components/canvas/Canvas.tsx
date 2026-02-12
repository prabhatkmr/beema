'use client';

import { useDroppable } from '@dnd-kit/core';
import {
  SortableContext,
  verticalListSortingStrategy,
  useSortable,
} from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import { FieldBlock } from '@/types/layout';
import { Button } from '@beema/ui';

interface SortableFieldBlockProps {
  field: FieldBlock;
  onEdit: (id: string) => void;
  onDelete: (id: string) => void;
}

function SortableFieldBlock({ field, onEdit, onDelete }: SortableFieldBlockProps) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: field.id });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  };

  return (
    <div
      ref={setNodeRef}
      style={style}
      {...attributes}
      className={`
        p-4 mb-2 bg-white border-2 rounded-lg
        ${isDragging ? 'border-blue-500 shadow-lg' : 'border-gray-300'}
        hover:border-blue-400 transition-colors
      `}
    >
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <div className="flex items-center gap-2 mb-2">
            <button
              {...listeners}
              className="cursor-move p-1 hover:bg-gray-100 rounded"
            >
              <svg className="w-5 h-5 text-gray-400" fill="currentColor" viewBox="0 0 20 20">
                <path d="M7 2a2 2 0 1 0 .001 4.001A2 2 0 0 0 7 2zm0 6a2 2 0 1 0 .001 4.001A2 2 0 0 0 7 8zm0 6a2 2 0 1 0 .001 4.001A2 2 0 0 0 7 14zm6-8a2 2 0 1 0-.001-4.001A2 2 0 0 0 13 6zm0 2a2 2 0 1 0 .001 4.001A2 2 0 0 0 13 8zm0 6a2 2 0 1 0 .001 4.001A2 2 0 0 0 13 14z" />
              </svg>
            </button>
            <div>
              <p className="font-semibold">{field.label}</p>
              <p className="text-sm text-gray-500">
                {field.type} {field.required && <span className="text-red-500">*</span>}
              </p>
            </div>
          </div>
          <p className="text-xs text-gray-600">Field name: {field.name}</p>
        </div>

        <div className="flex gap-2">
          <Button
            size="sm"
            variant="outline"
            onClick={() => onEdit(field.id)}
          >
            Edit
          </Button>
          <Button
            size="sm"
            variant="outline"
            onClick={() => onDelete(field.id)}
            className="text-red-600 hover:bg-red-50"
          >
            Delete
          </Button>
        </div>
      </div>

      {/* Field Preview */}
      <div className="mt-3 p-3 bg-gray-50 rounded border border-gray-200">
        <FieldPreview field={field} />
      </div>
    </div>
  );
}

function FieldPreview({ field }: { field: FieldBlock }) {
  switch (field.type) {
    case 'text':
    case 'email':
    case 'number':
      return <input type={field.type} placeholder={field.placeholder} className="w-full border rounded px-3 py-2" disabled />;
    case 'textarea':
      return <textarea placeholder={field.placeholder} className="w-full border rounded px-3 py-2" rows={3} disabled />;
    case 'select':
      return (
        <select className="w-full border rounded px-3 py-2" disabled>
          <option>Select...</option>
          {field.options?.map(opt => (
            <option key={opt.value} value={opt.value}>{opt.label}</option>
          ))}
        </select>
      );
    case 'checkbox':
      return <input type="checkbox" disabled className="w-4 h-4" />;
    case 'radio':
      return (
        <div className="space-y-2">
          {field.options?.map(opt => (
            <label key={opt.value} className="flex items-center gap-2">
              <input type="radio" name={field.name} disabled />
              <span>{opt.label}</span>
            </label>
          ))}
        </div>
      );
    case 'date':
      return <input type="date" className="w-full border rounded px-3 py-2" disabled />;
    case 'file':
      return <input type="file" className="w-full" disabled />;
    default:
      return <div className="text-gray-400">Preview not available</div>;
  }
}

interface CanvasProps {
  fields: FieldBlock[];
  onEdit: (id: string) => void;
  onDelete: (id: string) => void;
}

export function Canvas({ fields, onEdit, onDelete }: CanvasProps) {
  const { setNodeRef, isOver } = useDroppable({
    id: 'canvas-drop-zone',
  });

  return (
    <div
      ref={setNodeRef}
      className={`
        flex-1 p-8 overflow-y-auto
        ${isOver ? 'bg-blue-50' : 'bg-white'}
        transition-colors
      `}
    >
      <SortableContext items={fields} strategy={verticalListSortingStrategy}>
        {fields.length === 0 ? (
          <div className="flex items-center justify-center h-full">
            <div className="text-center text-gray-400">
              <svg className="w-16 h-16 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
              </svg>
              <p className="text-lg font-medium">Drop fields here to build your form</p>
              <p className="text-sm mt-2">Drag field blocks from the sidebar</p>
            </div>
          </div>
        ) : (
          <div>
            {fields.map((field) => (
              <SortableFieldBlock
                key={field.id}
                field={field}
                onEdit={onEdit}
                onDelete={onDelete}
              />
            ))}
          </div>
        )}
      </SortableContext>
    </div>
  );
}
