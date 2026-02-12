'use client';

import { useState } from 'react';
import { DndContext, DragEndEvent, DragOverEvent } from '@dnd-kit/core';
import { arrayMove } from '@dnd-kit/sortable';
import { FieldBlock, SysLayout } from '@/types/layout';
import { FIELD_DEFINITIONS } from '@/lib/fieldDefinitions';
import { FieldBlocksSidebar } from '@/components/canvas/FieldBlocksSidebar';
import { Canvas } from '@/components/canvas/Canvas';
import { PropertiesPanel } from '@/components/canvas/PropertiesPanel';
import { ValidationPanel } from '@/components/canvas/ValidationPanel';
import { Button } from '@beema/ui';
import { v4 as uuidv4 } from 'uuid';

export default function CanvasPage() {
  const [fields, setFields] = useState<FieldBlock[]>([]);
  const [selectedFieldId, setSelectedFieldId] = useState<string | null>(null);
  const [layoutName, setLayoutName] = useState('Untitled Layout');
  const [validationErrors, setValidationErrors] = useState<string[]>([]);
  const [validationWarnings, setValidationWarnings] = useState<string[]>([]);
  const [isSaving, setIsSaving] = useState(false);

  const selectedField = fields.find(f => f.id === selectedFieldId) || null;

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;

    if (!over) return;

    // Dragging from sidebar to canvas
    if (active.id.toString().startsWith('draggable-')) {
      const fieldType = active.data.current?.type;
      const fieldDef = FIELD_DEFINITIONS.find(f => f.type === fieldType);

      if (fieldDef) {
        const newField: FieldBlock = {
          id: uuidv4(),
          type: fieldDef.type,
          name: `field_${fields.length + 1}`,
          ...fieldDef.defaultProps,
        } as FieldBlock;

        setFields([...fields, newField]);
      }
      return;
    }

    // Reordering within canvas
    if (over.id !== active.id) {
      const oldIndex = fields.findIndex(f => f.id === active.id);
      const newIndex = fields.findIndex(f => f.id === over.id);

      if (oldIndex !== -1 && newIndex !== -1) {
        setFields(arrayMove(fields, oldIndex, newIndex));
      }
    }
  };

  const handleUpdateField = (updates: Partial<FieldBlock>) => {
    if (!selectedFieldId) return;

    setFields(fields.map(f =>
      f.id === selectedFieldId ? { ...f, ...updates } : f
    ));
  };

  const handleDeleteField = (id: string) => {
    setFields(fields.filter(f => f.id !== id));
    if (selectedFieldId === id) {
      setSelectedFieldId(null);
    }
  };

  const generateSysLayout = (): SysLayout => {
    return {
      layout_id: uuidv4(),
      layout_name: layoutName,
      layout_type: 'form',
      market_context: 'RETAIL',
      fields: fields,
      metadata: {
        version: 1,
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString(),
        created_by: 'studio-user',
      },
      grid_config: {
        columns: 12,
        gap: 16,
      },
    };
  };

  const handleExportJSON = () => {
    const layout = generateSysLayout();
    const json = JSON.stringify(layout, null, 2);
    const blob = new Blob([json], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${layoutName.replace(/\s+/g, '_')}.json`;
    a.click();
  };

  const handleCopyJSON = () => {
    const layout = generateSysLayout();
    const json = JSON.stringify(layout, null, 2);
    navigator.clipboard.writeText(json);
    alert('Layout JSON copied to clipboard!');
  };

  const handleSaveLayout = async () => {
    const layout = generateSysLayout();
    setIsSaving(true);
    setValidationErrors([]);
    setValidationWarnings([]);

    try {
      const response = await fetch('/api/layouts/save', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(layout),
      });

      const result = await response.json();

      if (result.success) {
        setValidationWarnings(result.warnings || []);
        alert('Layout saved successfully!' +
          (result.warnings?.length ? '\n\nWarnings:\n' + result.warnings.join('\n') : ''));
      } else {
        setValidationErrors(result.errors || ['Failed to save layout']);
      }
    } catch (error) {
      setValidationErrors(['Error saving layout: ' + error]);
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div className="flex flex-col h-screen">
      {/* Toolbar */}
      <div className="border-b p-4 flex justify-between items-center bg-white">
        <div className="flex items-center gap-4">
          <input
            type="text"
            value={layoutName}
            onChange={(e) => setLayoutName(e.target.value)}
            className="text-lg font-semibold border-none focus:outline-none focus:ring-2 focus:ring-blue-500 rounded px-2"
          />
          <span className="text-sm text-gray-500">{fields.length} fields</span>
        </div>

        <div className="flex gap-2">
          <Button variant="outline" onClick={handleCopyJSON}>
            Copy JSON
          </Button>
          <Button variant="outline" onClick={handleExportJSON}>
            Export JSON
          </Button>
          <Button
            variant="primary"
            onClick={handleSaveLayout}
            disabled={isSaving}
          >
            {isSaving ? 'Saving...' : 'Save Layout'}
          </Button>
        </div>
      </div>

      {/* Main Content */}
      <div className="flex flex-1 overflow-hidden">
        <DndContext onDragEnd={handleDragEnd}>
          <FieldBlocksSidebar />
          <Canvas
            fields={fields}
            onEdit={setSelectedFieldId}
            onDelete={handleDeleteField}
          />
        </DndContext>
        <PropertiesPanel
          field={selectedField}
          onUpdate={handleUpdateField}
          onClose={() => setSelectedFieldId(null)}
        />
      </div>

      {/* Validation Panel */}
      <ValidationPanel
        errors={validationErrors}
        warnings={validationWarnings}
        onClose={() => {
          setValidationErrors([]);
          setValidationWarnings([]);
        }}
      />
    </div>
  );
}
