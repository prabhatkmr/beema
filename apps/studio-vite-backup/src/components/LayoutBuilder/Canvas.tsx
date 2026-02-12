import React from 'react';
import { useDroppable } from '@dnd-kit/core';
import { SortableContext, verticalListSortingStrategy } from '@dnd-kit/sortable';
import { DroppableField } from './DroppableField';
import { FieldDefinition } from './types';

interface CanvasProps {
  fields: FieldDefinition[];
  selectedFieldId: string | null;
  onSelectField: (id: string) => void;
  onDeleteField: (id: string) => void;
}

export const Canvas: React.FC<CanvasProps> = ({
  fields,
  selectedFieldId,
  onSelectField,
  onDeleteField,
}) => {
  const { setNodeRef, isOver } = useDroppable({
    id: 'canvas',
  });

  return (
    <div className="flex-1 bg-gray-50 overflow-auto">
      <div className="max-w-4xl mx-auto p-8">
        <div className="mb-6">
          <h2 className="text-2xl font-bold text-gray-800">Layout Canvas</h2>
          <p className="text-sm text-gray-500 mt-1">
            {fields.length === 0
              ? 'Drag fields from the sidebar to start building'
              : `${fields.length} field${fields.length !== 1 ? 's' : ''} in layout`}
          </p>
        </div>
        <div
          ref={setNodeRef}
          className={`min-h-[500px] bg-white rounded-lg border-2 border-dashed p-6 transition-colors ${
            isOver ? 'border-blue-500 bg-blue-50' : 'border-gray-300'
          }`}
        >
          {fields.length === 0 ? (
            <div className="flex flex-col items-center justify-center h-[400px] text-center">
              <div className="text-gray-400 mb-2">
                <svg
                  className="w-16 h-16 mx-auto mb-4"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={1.5}
                    d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"
                  />
                </svg>
              </div>
              <p className="text-lg font-medium text-gray-600">No fields yet</p>
              <p className="text-sm text-gray-400 mt-1 max-w-md">
                Drag field types from the left sidebar to start building your form layout
              </p>
            </div>
          ) : (
            <SortableContext items={fields.map((f) => f.id)} strategy={verticalListSortingStrategy}>
              {fields.map((field) => (
                <DroppableField
                  key={field.id}
                  field={field}
                  isSelected={selectedFieldId === field.id}
                  onSelect={onSelectField}
                  onDelete={onDeleteField}
                />
              ))}
            </SortableContext>
          )}
        </div>
      </div>
    </div>
  );
};
