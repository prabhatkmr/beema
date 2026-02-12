import React, { useCallback } from 'react';
import { DndContext, DragEndEvent } from '@dnd-kit/core';
import { SourceFieldList } from './SourceFieldList';
import { TargetFieldList } from './TargetFieldList';
import { JexlExpressionEditor } from './JexlExpressionEditor';
import { Card } from '../ui/Card';
import { useDragAndDrop } from '../../hooks/useDragAndDrop';
import { useBlueprintStore } from '../../stores/blueprintStore';
import { FieldMapping, SourceField, TargetField } from '../../types/blueprint';
import { AlertCircle } from 'lucide-react';
import { v4 as uuidv4 } from 'uuid';

interface BlueprintCanvasProps {
  sourceFields: SourceField[];
  targetFields: TargetField[];
}

export const BlueprintCanvas: React.FC<BlueprintCanvasProps> = ({
  sourceFields,
  targetFields,
}) => {
  const { sensors, handleDragStart, handleDragEnd, handleDragCancel } = useDragAndDrop();
  const {
    currentBlueprint,
    selectedMapping,
    addMapping,
    updateMapping,
    setSelectedMapping,
  } = useBlueprintStore();

  const handleDrop = useCallback(
    (event: DragEndEvent) => {
      const draggedItem = event.active.data.current;
      const droppedOn = event.over?.data.current;

      if (!draggedItem || !droppedOn) return;

      if (draggedItem.type === 'source-field' && droppedOn.type === 'target-field') {
        // Check if mapping already exists
        const existingMapping = currentBlueprint?.mappings.find(
          (m) => m.targetField === droppedOn.path
        );

        if (existingMapping) {
          // Update existing mapping
          updateMapping(existingMapping.id, {
            sourceField: draggedItem.path,
            mappingType: 'direct',
            jexlExpression: undefined,
            constantValue: undefined,
          });
        } else {
          // Create new mapping
          const newMapping: FieldMapping = {
            id: uuidv4(),
            sourceField: draggedItem.path,
            targetField: droppedOn.path,
            mappingType: 'direct',
          };
          addMapping(newMapping);
        }
      }

      handleDragEnd(event);
    },
    [currentBlueprint, addMapping, updateMapping, handleDragEnd]
  );

  const handleExpressionChange = (expression: string) => {
    if (selectedMapping) {
      updateMapping(selectedMapping.id, {
        jexlExpression: expression,
        mappingType: 'transform',
      });
    }
  };

  if (!currentBlueprint) {
    return (
      <div className="flex-1 flex items-center justify-center bg-gray-50">
        <Card className="max-w-md">
          <div className="text-center py-8">
            <AlertCircle size={48} className="mx-auto text-gray-400 mb-4" />
            <h3 className="text-lg font-semibold text-gray-900 mb-2">No Blueprint Selected</h3>
            <p className="text-gray-600">
              Select a blueprint from the sidebar or create a new one to get started.
            </p>
          </div>
        </Card>
      </div>
    );
  }

  return (
    <DndContext
      sensors={sensors}
      onDragStart={handleDragStart}
      onDragEnd={handleDrop}
      onDragCancel={handleDragCancel}
    >
      <div className="flex-1 flex flex-col h-full">
        <div className="flex-1 grid grid-cols-2 gap-4 p-4 overflow-hidden">
          <div className="overflow-auto">
            <SourceFieldList sourceFields={sourceFields} />
          </div>
          <div className="overflow-auto">
            <TargetFieldList targetFields={targetFields} />
          </div>
        </div>

        {selectedMapping && (
          <div className="border-t border-gray-200 p-4 bg-white">
            <div className="max-w-4xl mx-auto">
              <div className="mb-3">
                <h3 className="text-sm font-semibold text-gray-900">
                  Mapping Details: {selectedMapping.targetField}
                </h3>
                {selectedMapping.sourceField && (
                  <p className="text-sm text-gray-600">
                    Source: {selectedMapping.sourceField}
                  </p>
                )}
              </div>

              <div className="grid grid-cols-3 gap-4">
                <div className="col-span-2">
                  <JexlExpressionEditor
                    value={selectedMapping.jexlExpression || ''}
                    onChange={handleExpressionChange}
                    testContext={{
                      source: sourceFields.reduce((acc, field) => {
                        acc[field.name] = field.example || null;
                        return acc;
                      }, {} as Record<string, any>),
                    }}
                  />
                </div>

                <div className="space-y-3">
                  <Card title="Mapping Type">
                    <select
                      value={selectedMapping.mappingType}
                      onChange={(e) =>
                        updateMapping(selectedMapping.id, {
                          mappingType: e.target.value as any,
                        })
                      }
                      className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                    >
                      <option value="direct">Direct</option>
                      <option value="transform">Transform</option>
                      <option value="constant">Constant</option>
                      <option value="conditional">Conditional</option>
                    </select>
                  </Card>

                  {selectedMapping.mappingType === 'constant' && (
                    <Card title="Constant Value">
                      <input
                        type="text"
                        value={selectedMapping.constantValue || ''}
                        onChange={(e) =>
                          updateMapping(selectedMapping.id, {
                            constantValue: e.target.value,
                          })
                        }
                        className="w-full px-3 py-2 border border-gray-300 rounded-md text-sm"
                        placeholder="Enter constant value"
                      />
                    </Card>
                  )}
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </DndContext>
  );
};
