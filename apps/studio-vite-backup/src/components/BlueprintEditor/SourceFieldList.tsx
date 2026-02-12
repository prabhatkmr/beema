import React from 'react';
import { useDraggable } from '@dnd-kit/core';
import { Database, ChevronRight } from 'lucide-react';
import { Card } from '../ui/Card';
import { SourceField } from '../../types/blueprint';

interface DraggableFieldProps {
  field: SourceField;
}

const DraggableField: React.FC<DraggableFieldProps> = ({ field }) => {
  const { attributes, listeners, setNodeRef, isDragging } = useDraggable({
    id: `source-${field.path}`,
    data: {
      id: `source-${field.path}`,
      type: 'source-field',
      field: field.name,
      path: field.path,
      dataType: field.type,
    },
  });

  return (
    <div
      ref={setNodeRef}
      {...listeners}
      {...attributes}
      className={`p-3 mb-2 bg-white border rounded-lg cursor-move hover:shadow-md transition-shadow ${
        isDragging ? 'opacity-50 shadow-lg' : 'border-gray-200'
      }`}
    >
      <div className="flex items-center justify-between">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2">
            <Database size={14} className="text-gray-400 flex-shrink-0" />
            <span className="font-medium text-gray-900 text-sm truncate">{field.name}</span>
          </div>
          <div className="flex items-center gap-2 mt-1">
            <span className="text-xs text-gray-500">{field.path}</span>
            <span className="px-1.5 py-0.5 bg-blue-100 text-blue-700 text-xs rounded">
              {field.type}
            </span>
          </div>
          {field.description && (
            <p className="text-xs text-gray-500 mt-1 line-clamp-2">{field.description}</p>
          )}
        </div>
        <ChevronRight size={16} className="text-gray-400 flex-shrink-0 ml-2" />
      </div>
    </div>
  );
};

interface SourceFieldListProps {
  sourceFields: SourceField[];
}

export const SourceFieldList: React.FC<SourceFieldListProps> = ({ sourceFields }) => {
  const [searchQuery, setSearchQuery] = React.useState('');

  const filteredFields = sourceFields.filter(
    (field) =>
      field.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      field.path.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <Card title="Source Fields" className="h-full flex flex-col">
      <div className="mb-4">
        <input
          type="text"
          placeholder="Search fields..."
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:outline-none focus:ring-2 focus:ring-primary-500"
        />
      </div>

      <div className="flex-1 overflow-y-auto">
        {filteredFields.length > 0 ? (
          <div>
            {filteredFields.map((field) => (
              <DraggableField key={field.path} field={field} />
            ))}
          </div>
        ) : (
          <div className="flex items-center justify-center h-32 text-gray-500">
            <p className="text-sm">No fields found</p>
          </div>
        )}
      </div>

      <div className="mt-4 pt-4 border-t border-gray-200">
        <p className="text-xs text-gray-500">
          Drag fields to the target panel to create mappings
        </p>
      </div>
    </Card>
  );
};
