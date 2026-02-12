'use client';

import { useDraggable } from '@dnd-kit/core';
import { FIELD_DEFINITIONS } from '@/lib/fieldDefinitions';
import { FieldType } from '@/types/layout';

interface DraggableFieldBlockProps {
  type: FieldType;
  icon: string;
  label: string;
}

function DraggableFieldBlock({ type, icon, label }: DraggableFieldBlockProps) {
  const { attributes, listeners, setNodeRef, isDragging } = useDraggable({
    id: `draggable-${type}`,
    data: { type },
  });

  return (
    <div
      ref={setNodeRef}
      {...listeners}
      {...attributes}
      className={`
        flex items-center gap-3 p-3 rounded-lg border-2 border-gray-200
        bg-white cursor-move hover:border-blue-400 hover:bg-blue-50
        transition-all select-none
        ${isDragging ? 'opacity-50 scale-95' : 'opacity-100 scale-100'}
      `}
    >
      <span className="text-2xl">{icon}</span>
      <div>
        <p className="font-medium text-sm">{label}</p>
        <p className="text-xs text-gray-500">{type}</p>
      </div>
    </div>
  );
}

export function FieldBlocksSidebar() {
  return (
    <div className="w-64 border-r bg-gray-50 p-4 overflow-y-auto">
      <h2 className="text-lg font-bold mb-4">Field Blocks</h2>
      <p className="text-sm text-gray-600 mb-4">
        Drag fields to the canvas
      </p>

      <div className="space-y-2">
        {FIELD_DEFINITIONS.map((field) => (
          <DraggableFieldBlock
            key={field.type}
            type={field.type}
            icon={field.icon}
            label={field.label}
          />
        ))}
      </div>
    </div>
  );
}
