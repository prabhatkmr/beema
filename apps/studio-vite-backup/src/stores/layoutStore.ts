import { create } from 'zustand';
import { v4 as uuidv4 } from 'uuid';
import { FieldDefinition, LayoutConfig } from '../components/LayoutBuilder/types';

interface LayoutStore {
  layouts: LayoutConfig[];
  currentLayout: LayoutConfig | null;
  selectedFieldId: string | null;

  // Layout operations
  createLayout: (name: string) => void;
  loadLayout: (id: string) => void;
  saveLayout: () => void;
  deleteLayout: (id: string) => void;

  // Field operations
  addField: (field: Omit<FieldDefinition, 'id'>) => void;
  updateField: (id: string, updates: Partial<FieldDefinition>) => void;
  removeField: (id: string) => void;
  reorderFields: (activeId: string, overId: string) => void;
  selectField: (id: string | null) => void;

  // Persistence
  exportLayout: () => string | null;
  importLayout: (json: string) => void;
}

const STORAGE_KEY = 'beema-layouts';

const loadLayoutsFromStorage = (): LayoutConfig[] => {
  try {
    const stored = localStorage.getItem(STORAGE_KEY);
    return stored ? JSON.parse(stored) : [];
  } catch (error) {
    console.error('Failed to load layouts from storage:', error);
    return [];
  }
};

const saveLayoutsToStorage = (layouts: LayoutConfig[]) => {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(layouts));
  } catch (error) {
    console.error('Failed to save layouts to storage:', error);
  }
};

export const useLayoutStore = create<LayoutStore>((set, get) => ({
  layouts: loadLayoutsFromStorage(),
  currentLayout: null,
  selectedFieldId: null,

  createLayout: (name: string) => {
    const newLayout: LayoutConfig = {
      id: uuidv4(),
      name,
      fields: [],
      metadata: {
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
        version: 1,
      },
    };

    set((state) => {
      const layouts = [...state.layouts, newLayout];
      saveLayoutsToStorage(layouts);
      return {
        layouts,
        currentLayout: newLayout,
        selectedFieldId: null,
      };
    });
  },

  loadLayout: (id: string) => {
    const { layouts } = get();
    const layout = layouts.find((l) => l.id === id);
    if (layout) {
      set({ currentLayout: layout, selectedFieldId: null });
    }
  },

  saveLayout: () => {
    const { currentLayout, layouts } = get();
    if (!currentLayout) return;

    const updatedLayout = {
      ...currentLayout,
      metadata: {
        ...currentLayout.metadata,
        updatedAt: new Date().toISOString(),
        version: currentLayout.metadata.version + 1,
      },
    };

    const updatedLayouts = layouts.map((l) =>
      l.id === updatedLayout.id ? updatedLayout : l
    );

    saveLayoutsToStorage(updatedLayouts);
    set({ layouts: updatedLayouts, currentLayout: updatedLayout });
  },

  deleteLayout: (id: string) => {
    set((state) => {
      const layouts = state.layouts.filter((l) => l.id !== id);
      saveLayoutsToStorage(layouts);
      return {
        layouts,
        currentLayout: state.currentLayout?.id === id ? null : state.currentLayout,
      };
    });
  },

  addField: (fieldData: Omit<FieldDefinition, 'id'>) => {
    const { currentLayout } = get();
    if (!currentLayout) return;

    const newField: FieldDefinition = {
      ...fieldData,
      id: uuidv4(),
    };

    const updatedLayout = {
      ...currentLayout,
      fields: [...currentLayout.fields, newField],
    };

    set({ currentLayout: updatedLayout });
  },

  updateField: (id: string, updates: Partial<FieldDefinition>) => {
    const { currentLayout } = get();
    if (!currentLayout) return;

    const updatedFields = currentLayout.fields.map((field) =>
      field.id === id ? { ...field, ...updates } : field
    );

    const updatedLayout = {
      ...currentLayout,
      fields: updatedFields,
    };

    set({ currentLayout: updatedLayout });
  },

  removeField: (id: string) => {
    const { currentLayout, selectedFieldId } = get();
    if (!currentLayout) return;

    const updatedFields = currentLayout.fields.filter((field) => field.id !== id);

    const updatedLayout = {
      ...currentLayout,
      fields: updatedFields,
    };

    set({
      currentLayout: updatedLayout,
      selectedFieldId: selectedFieldId === id ? null : selectedFieldId,
    });
  },

  reorderFields: (activeId: string, overId: string) => {
    const { currentLayout } = get();
    if (!currentLayout) return;

    const fields = [...currentLayout.fields];
    const activeIndex = fields.findIndex((f) => f.id === activeId);
    const overIndex = fields.findIndex((f) => f.id === overId);

    if (activeIndex === -1 || overIndex === -1) return;

    const [removed] = fields.splice(activeIndex, 1);
    fields.splice(overIndex, 0, removed);

    const updatedLayout = {
      ...currentLayout,
      fields,
    };

    set({ currentLayout: updatedLayout });
  },

  selectField: (id: string | null) => {
    set({ selectedFieldId: id });
  },

  exportLayout: () => {
    const { currentLayout } = get();
    if (!currentLayout) return null;

    return JSON.stringify(currentLayout, null, 2);
  },

  importLayout: (json: string) => {
    try {
      const layout: LayoutConfig = JSON.parse(json);

      // Validate the layout structure
      if (!layout.id || !layout.name || !Array.isArray(layout.fields)) {
        throw new Error('Invalid layout format');
      }

      // Assign new IDs to avoid conflicts
      const importedLayout: LayoutConfig = {
        ...layout,
        id: uuidv4(),
        metadata: {
          ...layout.metadata,
          createdAt: new Date().toISOString(),
          updatedAt: new Date().toISOString(),
          version: 1,
        },
      };

      set((state) => {
        const layouts = [...state.layouts, importedLayout];
        saveLayoutsToStorage(layouts);
        return {
          layouts,
          currentLayout: importedLayout,
          selectedFieldId: null,
        };
      });
    } catch (error) {
      console.error('Failed to import layout:', error);
      throw error;
    }
  },
}));
