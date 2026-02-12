import React, { useState } from 'react';
import { useLocation } from 'react-router-dom';
import { Plus, FileText, Copy, Trash2, Search, Layout, FolderOpen } from 'lucide-react';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { Spinner } from '../ui/Spinner';
import { Modal } from '../ui/Modal';
import {
  useBlueprintsQuery,
  useDeleteBlueprintMutation,
  useCloneBlueprintMutation,
} from '../../hooks/useBlueprintQuery';
import { useBlueprintStore } from '../../stores/blueprintStore';
import { useLayoutStore } from '../../stores/layoutStore';
import { MessageBlueprint } from '../../types/blueprint';
import toast from 'react-hot-toast';

interface CreateBlueprintModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const CreateBlueprintModal: React.FC<CreateBlueprintModalProps> = ({ isOpen, onClose }) => {
  // Implementation will be added later
  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Create New Blueprint">
      <p>Create blueprint form will be implemented here</p>
    </Modal>
  );
};

interface CreateLayoutModalProps {
  isOpen: boolean;
  onClose: () => void;
}

const CreateLayoutModal: React.FC<CreateLayoutModalProps> = ({ isOpen, onClose }) => {
  const [layoutName, setLayoutName] = useState('');
  const { createLayout } = useLayoutStore();

  const handleCreate = () => {
    if (layoutName.trim()) {
      createLayout(layoutName);
      toast.success(`Layout "${layoutName}" created!`);
      onClose();
      setLayoutName('');
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Create New Layout">
      <div className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Layout Name</label>
          <Input
            type="text"
            value={layoutName}
            onChange={(e) => setLayoutName(e.target.value)}
            placeholder="Enter layout name..."
            onKeyPress={(e) => {
              if (e.key === 'Enter') {
                handleCreate();
              }
            }}
          />
        </div>
        <div className="flex gap-2">
          <Button onClick={onClose} variant="outline" className="flex-1">
            Cancel
          </Button>
          <Button onClick={handleCreate} className="flex-1" disabled={!layoutName.trim()}>
            Create Layout
          </Button>
        </div>
      </div>
    </Modal>
  );
};

export const Sidebar: React.FC = () => {
  const location = useLocation();
  const [searchQuery, setSearchQuery] = useState('');
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);

  const isBlueprintPage = location.pathname === '/blueprints';
  const isLayoutBuilderPage = location.pathname === '/layout-builder';
  const isHomePage = location.pathname === '/';

  // Blueprint-related state
  const { data: blueprints, isLoading: isBlueprintsLoading } = useBlueprintsQuery();
  const deleteMutation = useDeleteBlueprintMutation();
  const cloneMutation = useCloneBlueprintMutation();
  const { currentBlueprint, setCurrentBlueprint } = useBlueprintStore();

  // Layout-related state
  const { layouts, currentLayout, loadLayout, deleteLayout } = useLayoutStore();

  const filteredBlueprints = blueprints?.filter((bp) =>
    bp.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const filteredLayouts = layouts.filter((layout) =>
    layout.name.toLowerCase().includes(searchQuery.toLowerCase())
  );

  const handleSelectBlueprint = (blueprint: MessageBlueprint) => {
    setCurrentBlueprint(blueprint);
  };

  const handleCloneBlueprint = (e: React.MouseEvent, blueprint: MessageBlueprint) => {
    e.stopPropagation();
    const newName = prompt('Enter name for cloned blueprint:', `${blueprint.name} (Copy)`);
    if (newName) {
      cloneMutation.mutate({ id: blueprint.id, newName });
    }
  };

  const handleDeleteBlueprint = (e: React.MouseEvent, blueprintId: string) => {
    e.stopPropagation();
    if (confirm('Are you sure you want to delete this blueprint?')) {
      deleteMutation.mutate(blueprintId);
    }
  };

  const handleDeleteLayout = (e: React.MouseEvent, layoutId: string) => {
    e.stopPropagation();
    if (confirm('Are you sure you want to delete this layout?')) {
      deleteLayout(layoutId);
      toast.success('Layout deleted!');
    }
  };

  // Don't show sidebar on home page
  if (isHomePage) {
    return null;
  }

  return (
    <>
      <aside className="w-80 bg-gray-50 border-r border-gray-200 flex flex-col h-full">
        <div className="p-4 border-b border-gray-200">
          <Button className="w-full" onClick={() => setIsCreateModalOpen(true)}>
            <Plus size={18} className="mr-2" />
            {isBlueprintPage ? 'New Blueprint' : 'New Layout'}
          </Button>
        </div>

        <div className="p-4 border-b border-gray-200">
          <div className="relative">
            <Search
              className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400"
              size={18}
            />
            <Input
              type="text"
              placeholder={isBlueprintPage ? 'Search blueprints...' : 'Search layouts...'}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-10"
            />
          </div>
        </div>

        <div className="flex-1 overflow-y-auto">
          {isBlueprintPage ? (
            // Blueprint sidebar content
            isBlueprintsLoading ? (
              <div className="flex items-center justify-center h-32">
                <Spinner />
              </div>
            ) : filteredBlueprints && filteredBlueprints.length > 0 ? (
              <div className="p-2">
                {filteredBlueprints.map((blueprint) => (
                  <div
                    key={blueprint.id}
                    className={`group p-3 mb-2 rounded-lg cursor-pointer transition-colors ${
                      currentBlueprint?.id === blueprint.id
                        ? 'bg-primary-100 border-2 border-primary-500'
                        : 'bg-white hover:bg-gray-100 border-2 border-transparent'
                    }`}
                    onClick={() => handleSelectBlueprint(blueprint)}
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2">
                          <FileText size={16} className="text-gray-500 flex-shrink-0" />
                          <h3 className="font-medium text-gray-900 truncate">{blueprint.name}</h3>
                        </div>
                        <p className="text-xs text-gray-500 mt-1 truncate">
                          {blueprint.sourceSystem} → {blueprint.targetSchema}
                        </p>
                        <div className="flex items-center gap-2 mt-2">
                          <span
                            className={`inline-flex items-center px-2 py-0.5 rounded text-xs font-medium ${
                              blueprint.status === 'active'
                                ? 'bg-green-100 text-green-800'
                                : blueprint.status === 'draft'
                                ? 'bg-yellow-100 text-yellow-800'
                                : 'bg-gray-100 text-gray-800'
                            }`}
                          >
                            {blueprint.status}
                          </span>
                          <span className="text-xs text-gray-500">
                            {blueprint.mappings.length} mappings
                          </span>
                        </div>
                      </div>
                      <div className="flex gap-1 ml-2 opacity-0 group-hover:opacity-100 transition-opacity">
                        <button
                          onClick={(e) => handleCloneBlueprint(e, blueprint)}
                          className="p-1 text-gray-500 hover:text-primary-600 rounded"
                          title="Clone"
                        >
                          <Copy size={14} />
                        </button>
                        <button
                          onClick={(e) => handleDeleteBlueprint(e, blueprint.id)}
                          className="p-1 text-gray-500 hover:text-red-600 rounded"
                          title="Delete"
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="flex flex-col items-center justify-center h-32 text-gray-500">
                <FileText size={32} className="mb-2" />
                <p className="text-sm">No blueprints found</p>
              </div>
            )
          ) : isLayoutBuilderPage ? (
            // Layout sidebar content
            filteredLayouts.length > 0 ? (
              <div className="p-2">
                {filteredLayouts.map((layout) => (
                  <div
                    key={layout.id}
                    className={`group p-3 mb-2 rounded-lg cursor-pointer transition-colors ${
                      currentLayout?.id === layout.id
                        ? 'bg-indigo-100 border-2 border-indigo-500'
                        : 'bg-white hover:bg-gray-100 border-2 border-transparent'
                    }`}
                    onClick={() => loadLayout(layout.id)}
                  >
                    <div className="flex items-start justify-between">
                      <div className="flex-1 min-w-0">
                        <div className="flex items-center gap-2">
                          <Layout size={16} className="text-gray-500 flex-shrink-0" />
                          <h3 className="font-medium text-gray-900 truncate">{layout.name}</h3>
                        </div>
                        <p className="text-xs text-gray-500 mt-1">
                          {layout.fields.length} field{layout.fields.length !== 1 ? 's' : ''}
                        </p>
                        <p className="text-xs text-gray-400 mt-1">
                          Updated: {new Date(layout.metadata.updatedAt).toLocaleDateString()}
                        </p>
                      </div>
                      <div className="flex gap-1 ml-2 opacity-0 group-hover:opacity-100 transition-opacity">
                        <button
                          onClick={(e) => handleDeleteLayout(e, layout.id)}
                          className="p-1 text-gray-500 hover:text-red-600 rounded"
                          title="Delete"
                        >
                          <Trash2 size={14} />
                        </button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="flex flex-col items-center justify-center h-32 text-gray-500">
                <FolderOpen size={32} className="mb-2" />
                <p className="text-sm">No layouts found</p>
              </div>
            )
          ) : null}
        </div>
      </aside>

      {isBlueprintPage ? (
        <CreateBlueprintModal
          isOpen={isCreateModalOpen}
          onClose={() => setIsCreateModalOpen(false)}
        />
      ) : (
        <CreateLayoutModal
          isOpen={isCreateModalOpen}
          onClose={() => setIsCreateModalOpen(false)}
        />
      )}
    </>
  );
};
