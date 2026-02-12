import React, { useState } from 'react';
import {
  DndContext,
  DragOverlay,
  DragStartEvent,
  DragEndEvent,
  closestCenter,
  PointerSensor,
  useSensor,
  useSensors,
} from '@dnd-kit/core';
import { Save, Download, Upload, Eye, Code } from 'lucide-react';
import { FieldsSidebar } from './FieldsSidebar';
import { Canvas } from './Canvas';
import { FieldPropertiesPanel } from './FieldPropertiesPanel';
import { useLayoutStore } from '../../stores/layoutStore';
import { FieldDefinition, FieldTemplate } from './types';
import { Button } from '../ui/Button';
import { Modal } from '../ui/Modal';
import toast from 'react-hot-toast';

export const LayoutBuilder: React.FC = () => {
  const [activeId, setActiveId] = useState<string | null>(null);
  const [isPreviewMode, setIsPreviewMode] = useState(false);
  const [showExportModal, setShowExportModal] = useState(false);
  const [showImportModal, setShowImportModal] = useState(false);
  const [importJson, setImportJson] = useState('');

  const {
    currentLayout,
    selectedFieldId,
    addField,
    updateField,
    removeField,
    reorderFields,
    selectField,
    saveLayout,
    exportLayout,
    importLayout,
  } = useLayoutStore();

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 8,
      },
    })
  );

  const handleDragStart = (event: DragStartEvent) => {
    setActiveId(event.active.id as string);
  };

  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;
    setActiveId(null);

    if (!over) return;

    // Check if dragging from sidebar (template)
    if (active.id.toString().startsWith('template-')) {
      const template = active.data.current as FieldTemplate;
      const newField: Omit<FieldDefinition, 'id'> = {
        ...template.defaultConfig,
        type: template.type,
        label: template.defaultConfig.label || template.label,
        name:
          template.defaultConfig.name ||
          `${template.type}Field${(currentLayout?.fields.length || 0) + 1}`,
        required: template.defaultConfig.required || false,
      } as FieldDefinition;

      addField(newField);
      toast.success(`Added ${template.label} to canvas`);
      return;
    }

    // Handle reordering within canvas
    if (over.id === 'canvas') return;

    if (active.id !== over.id && currentLayout) {
      reorderFields(active.id as string, over.id as string);
    }
  };

  const handleSave = () => {
    saveLayout();
    toast.success('Layout saved successfully!');
  };

  const handleExport = () => {
    const json = exportLayout();
    if (json) {
      setShowExportModal(true);
    } else {
      toast.error('No layout to export');
    }
  };

  const handleImport = () => {
    try {
      importLayout(importJson);
      toast.success('Layout imported successfully!');
      setShowImportModal(false);
      setImportJson('');
    } catch (error) {
      toast.error('Failed to import layout. Please check the JSON format.');
    }
  };

  const handleCopyExport = () => {
    const json = exportLayout();
    if (json) {
      navigator.clipboard.writeText(json);
      toast.success('Layout JSON copied to clipboard!');
    }
  };

  const handleDownloadExport = () => {
    const json = exportLayout();
    if (json && currentLayout) {
      const blob = new Blob([json], { type: 'application/json' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `${currentLayout.name.replace(/\s+/g, '-').toLowerCase()}-layout.json`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      toast.success('Layout downloaded!');
    }
  };

  const selectedField = currentLayout?.fields.find((f) => f.id === selectedFieldId) || null;

  if (!currentLayout) {
    return (
      <div className="h-full flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <h2 className="text-2xl font-bold text-gray-800 mb-2">No Layout Selected</h2>
          <p className="text-gray-600 mb-4">Create or load a layout to start building</p>
          <Button onClick={() => useLayoutStore.getState().createLayout('New Layout')}>
            Create New Layout
          </Button>
        </div>
      </div>
    );
  }

  return (
    <>
      <div className="h-full flex flex-col">
        {/* Toolbar */}
        <div className="bg-white border-b border-gray-200 px-6 py-3 flex items-center justify-between">
          <div>
            <h1 className="text-xl font-bold text-gray-800">{currentLayout.name}</h1>
            <p className="text-sm text-gray-500">
              Last updated: {new Date(currentLayout.metadata.updatedAt).toLocaleString()}
            </p>
          </div>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              onClick={() => setIsPreviewMode(!isPreviewMode)}
              className="flex items-center gap-2"
            >
              {isPreviewMode ? (
                <>
                  <Code className="w-4 h-4" />
                  Edit Mode
                </>
              ) : (
                <>
                  <Eye className="w-4 h-4" />
                  Preview
                </>
              )}
            </Button>
            <Button
              variant="outline"
              onClick={() => setShowImportModal(true)}
              className="flex items-center gap-2"
            >
              <Upload className="w-4 h-4" />
              Import
            </Button>
            <Button
              variant="outline"
              onClick={handleExport}
              className="flex items-center gap-2"
            >
              <Download className="w-4 h-4" />
              Export
            </Button>
            <Button onClick={handleSave} className="flex items-center gap-2">
              <Save className="w-4 h-4" />
              Save Layout
            </Button>
          </div>
        </div>

        {/* Main Content */}
        <div className="flex-1 flex overflow-hidden">
          <DndContext
            sensors={sensors}
            collisionDetection={closestCenter}
            onDragStart={handleDragStart}
            onDragEnd={handleDragEnd}
          >
            {!isPreviewMode && <FieldsSidebar />}
            <Canvas
              fields={currentLayout.fields}
              selectedFieldId={selectedFieldId}
              onSelectField={selectField}
              onDeleteField={removeField}
            />
            {!isPreviewMode && (
              <FieldPropertiesPanel
                selectedField={selectedField}
                onUpdate={(updates) => {
                  if (selectedFieldId) {
                    updateField(selectedFieldId, updates);
                  }
                }}
                onClose={() => selectField(null)}
              />
            )}
            <DragOverlay>
              {activeId ? (
                <div className="bg-white p-4 rounded-lg shadow-lg border-2 border-blue-500 opacity-90">
                  <div className="font-medium text-gray-800">
                    {activeId.startsWith('template-')
                      ? activeId.replace('template-', '').toUpperCase()
                      : 'Moving field...'}
                  </div>
                </div>
              ) : null}
            </DragOverlay>
          </DndContext>
        </div>
      </div>

      {/* Export Modal */}
      {showExportModal && (
        <Modal
          isOpen={showExportModal}
          onClose={() => setShowExportModal(false)}
          title="Export Layout"
        >
          <div className="space-y-4">
            <p className="text-sm text-gray-600">
              Copy the JSON below or download it as a file:
            </p>
            <pre className="bg-gray-50 border border-gray-200 rounded-lg p-4 text-xs overflow-auto max-h-96">
              {exportLayout()}
            </pre>
            <div className="flex gap-2">
              <Button onClick={handleCopyExport} variant="outline" className="flex-1">
                Copy to Clipboard
              </Button>
              <Button onClick={handleDownloadExport} className="flex-1">
                Download JSON
              </Button>
            </div>
          </div>
        </Modal>
      )}

      {/* Import Modal */}
      {showImportModal && (
        <Modal
          isOpen={showImportModal}
          onClose={() => {
            setShowImportModal(false);
            setImportJson('');
          }}
          title="Import Layout"
        >
          <div className="space-y-4">
            <p className="text-sm text-gray-600">Paste the layout JSON below:</p>
            <textarea
              value={importJson}
              onChange={(e) => setImportJson(e.target.value)}
              className="w-full h-64 px-3 py-2 border border-gray-300 rounded-md text-sm font-mono"
              placeholder="Paste JSON here..."
            />
            <div className="flex gap-2">
              <Button
                onClick={() => {
                  setShowImportModal(false);
                  setImportJson('');
                }}
                variant="outline"
                className="flex-1"
              >
                Cancel
              </Button>
              <Button onClick={handleImport} className="flex-1" disabled={!importJson.trim()}>
                Import Layout
              </Button>
            </div>
          </div>
        </Modal>
      )}
    </>
  );
};
