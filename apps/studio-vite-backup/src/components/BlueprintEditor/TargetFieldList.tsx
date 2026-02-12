import React from 'react';
import { useDroppable } from '@dnd-kit/core';
import { Target, Check, Edit2, Trash2 } from 'lucide-react';
import { Card } from '../ui/Card';
import { TargetField } from '../../types/blueprint';
import { useBlueprintStore } from '../../stores/blueprintStore';

interface DroppableFieldProps {
  field: TargetField;
  isMapped: boolean;
  mappingType?: string;
  onEdit?: () => void;
  onDelete?: () => void;
}

const DroppableField: React.FC<DroppableFieldProps> = ({
  field,
  isMapped,
  mappingType,
  onEdit,
  onDelete,
}) => {
  const { setNodeRef, isOver } = useDroppable({
    id: `target-${field.path}`,
    data: {
      type: 'target-field',
      field: field.name,
      path: field.path,
      dataType: field.type,
    },
  });

  return (
    <div
      ref={setNodeRef}
      className={`p-3 mb-2 border rounded-lg transition-all ${
        isOver
          ? 'border-primary-500 bg-primary-50 shadow-md'
          : isMapped
          ? 'border-green-500 bg-green-50'
          : 'border-gray-200 bg-white'
      }`}
    >
      <div className="flex items-center justify-between">
        <div className="flex-1 min-w-0">
          <div className="flex items-center gap-2">
            <Target size={14} className="text-gray-400 flex-shrink-0" />
            <span className="font-medium text-gray-900 text-sm truncate">{field.name}</span>
            {field.required && (
              <span className="text-red-500 text-xs" title="Required">
                *
              </span>
            )}
            {isMapped && <Check size={14} className="text-green-600 flex-shrink-0" />}
          </div>
          <div className="flex items-center gap-2 mt-1">
            <span className="text-xs text-gray-500">{field.path}</span>
            <span className="px-1.5 py-0.5 bg-purple-100 text-purple-700 text-xs rounded">
              {field.type}
            </span>
            {mappingType && (
              <span className="px-1.5 py-0.5 bg-gray-100 text-gray-700 text-xs rounded">
                {mappingType}
              </span>
            )}
          </div>
          {field.description && (
            <p className="text-xs text-gray-500 mt-1 line-clamp-2">{field.description}</p>
          )}
        </div>

        {isMapped && (
          <div className="flex gap-1 ml-2">
            <button
              onClick={onEdit}
              className="p-1 text-gray-500 hover:text-primary-600 rounded"
              title="Edit mapping"
            >
              <Edit2 size={14} />
            </button>
            <button
              onClick={onDelete}
              className="p-1 text-gray-500 hover:text-red-600 rounded"
              title="Remove mapping"
            >
              <Trash2 size={14} />
            </button>
          </div>
        )}
      </div>
    </div>
  );
};

interface TargetFieldListProps {
  targetFields: TargetField[];
}

export const TargetFieldList: React.FC<TargetFieldListProps> = ({ targetFields }) => {
  const [searchQuery, setSearchQuery] = React.useState('');
  const { currentBlueprint, setSelectedMapping, deleteMapping } = useBlueprintStore();

  const filteredFields = targetFields.filter(
    (field) =>
      field.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      field.path.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const getMappingForField = (fieldPath: string) => {
    return currentBlueprint?.mappings.find((m) => m.targetField === fieldPath);
  };

  const handleEdit = (fieldPath: string) => {
    const mapping = getMappingForField(fieldPath);
    if (mapping) {
      setSelectedMapping(mapping);
    }
  };

  const handleDelete = (fieldPath: string) => {
    const mapping = getMappingForField(fieldPath);
    if (mapping && confirm('Are you sure you want to remove this mapping?')) {
      deleteMapping(mapping.id);
    }
  };

  return (
    <Card title="Target Fields" className="h-full flex flex-col">
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
            {filteredFields.map((field) => {
              const mapping = getMappingForField(field.path);
              return (
                <DroppableField
                  key={field.path}
                  field={field}
                  isMapped={!!mapping}
                  mappingType={mapping?.mappingType}
                  onEdit={() => handleEdit(field.path)}
                  onDelete={() => handleDelete(field.path)}
                />
              );
            })}
          </div>
        ) : (
          <div className="flex items-center justify-center h-32 text-gray-500">
            <p className="text-sm">No fields found</p>
          </div>
        )}
      </div>

      <div className="mt-4 pt-4 border-t border-gray-200">
        <div className="flex items-center justify-between text-xs text-gray-500">
          <span>
            {currentBlueprint?.mappings.length || 0} / {targetFields.length} fields mapped
          </span>
          <span>
            {targetFields.filter((f) => f.required && !getMappingForField(f.path)).length} required
            unmapped
          </span>
        </div>
      </div>
    </Card>
  );
};
