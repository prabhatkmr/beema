import { create } from 'zustand';
import { devtools, persist } from 'zustand/middleware';
import { MessageBlueprint, FieldMapping } from '../types/blueprint';
import { CanvasState } from '../types/mapping';

interface BlueprintState {
  // Current blueprint
  currentBlueprint: MessageBlueprint | null;
  originalBlueprint: MessageBlueprint | null;

  // Canvas state
  canvasState: CanvasState;

  // UI state
  selectedMapping: FieldMapping | null;
  isDirty: boolean;
  isTestPanelOpen: boolean;

  // Actions
  setCurrentBlueprint: (blueprint: MessageBlueprint | null) => void;
  updateBlueprint: (updates: Partial<MessageBlueprint>) => void;
  addMapping: (mapping: FieldMapping) => void;
  updateMapping: (mappingId: string, updates: Partial<FieldMapping>) => void;
  deleteMapping: (mappingId: string) => void;
  setSelectedMapping: (mapping: FieldMapping | null) => void;
  setCanvasState: (state: Partial<CanvasState>) => void;
  setTestPanelOpen: (open: boolean) => void;
  resetBlueprint: () => void;
  markAsSaved: () => void;
}

export const useBlueprintStore = create<BlueprintState>()(
  devtools(
    persist(
      (set, get) => ({
        currentBlueprint: null,
        originalBlueprint: null,
        canvasState: {
          zoom: 1,
          pan: { x: 0, y: 0 },
        },
        selectedMapping: null,
        isDirty: false,
        isTestPanelOpen: false,

        setCurrentBlueprint: (blueprint) => {
          set({
            currentBlueprint: blueprint,
            originalBlueprint: blueprint ? JSON.parse(JSON.stringify(blueprint)) : null,
            isDirty: false,
            selectedMapping: null,
          });
        },

        updateBlueprint: (updates) => {
          const current = get().currentBlueprint;
          if (!current) return;

          set({
            currentBlueprint: { ...current, ...updates },
            isDirty: true,
          });
        },

        addMapping: (mapping) => {
          const current = get().currentBlueprint;
          if (!current) return;

          set({
            currentBlueprint: {
              ...current,
              mappings: [...current.mappings, mapping],
            },
            isDirty: true,
          });
        },

        updateMapping: (mappingId, updates) => {
          const current = get().currentBlueprint;
          if (!current) return;

          set({
            currentBlueprint: {
              ...current,
              mappings: current.mappings.map((m) =>
                m.id === mappingId ? { ...m, ...updates } : m
              ),
            },
            isDirty: true,
          });
        },

        deleteMapping: (mappingId) => {
          const current = get().currentBlueprint;
          if (!current) return;

          set({
            currentBlueprint: {
              ...current,
              mappings: current.mappings.filter((m) => m.id !== mappingId),
            },
            selectedMapping:
              get().selectedMapping?.id === mappingId ? null : get().selectedMapping,
            isDirty: true,
          });
        },

        setSelectedMapping: (mapping) => {
          set({ selectedMapping: mapping });
        },

        setCanvasState: (state) => {
          set({
            canvasState: { ...get().canvasState, ...state },
          });
        },

        setTestPanelOpen: (open) => {
          set({ isTestPanelOpen: open });
        },

        resetBlueprint: () => {
          const original = get().originalBlueprint;
          if (original) {
            set({
              currentBlueprint: JSON.parse(JSON.stringify(original)),
              isDirty: false,
              selectedMapping: null,
            });
          }
        },

        markAsSaved: () => {
          const current = get().currentBlueprint;
          if (current) {
            set({
              originalBlueprint: JSON.parse(JSON.stringify(current)),
              isDirty: false,
            });
          }
        },
      }),
      {
        name: 'beema-studio-storage',
        partialize: (state) => ({
          canvasState: state.canvasState,
        }),
      }
    ),
    { name: 'BlueprintStore' }
  )
);
