import { describe, it, expect, beforeEach } from 'vitest';
import { useBlueprintStore } from '../blueprintStore';
import { MessageBlueprint, FieldMapping } from '../../types/blueprint';

describe('BlueprintStore', () => {
  beforeEach(() => {
    // Reset store before each test
    useBlueprintStore.setState({
      currentBlueprint: null,
      originalBlueprint: null,
      selectedMapping: null,
      isDirty: false,
      isTestPanelOpen: false,
      canvasState: { zoom: 1, pan: { x: 0, y: 0 } },
    });
  });

  it('should set current blueprint', () => {
    const mockBlueprint: MessageBlueprint = {
      id: '1',
      name: 'Test Blueprint',
      sourceSystem: 'test-source',
      targetSchema: 'test-target',
      mappings: [],
      createdAt: '2024-01-01',
      updatedAt: '2024-01-01',
      version: 1,
      status: 'draft',
    };

    useBlueprintStore.getState().setCurrentBlueprint(mockBlueprint);

    const state = useBlueprintStore.getState();
    expect(state.currentBlueprint).toEqual(mockBlueprint);
    expect(state.originalBlueprint).toEqual(mockBlueprint);
    expect(state.isDirty).toBe(false);
  });

  it('should mark as dirty when updating blueprint', () => {
    const mockBlueprint: MessageBlueprint = {
      id: '1',
      name: 'Test Blueprint',
      sourceSystem: 'test-source',
      targetSchema: 'test-target',
      mappings: [],
      createdAt: '2024-01-01',
      updatedAt: '2024-01-01',
      version: 1,
      status: 'draft',
    };

    useBlueprintStore.getState().setCurrentBlueprint(mockBlueprint);
    useBlueprintStore.getState().updateBlueprint({ name: 'Updated Name' });

    const state = useBlueprintStore.getState();
    expect(state.currentBlueprint?.name).toBe('Updated Name');
    expect(state.isDirty).toBe(true);
  });

  it('should add mapping', () => {
    const mockBlueprint: MessageBlueprint = {
      id: '1',
      name: 'Test Blueprint',
      sourceSystem: 'test-source',
      targetSchema: 'test-target',
      mappings: [],
      createdAt: '2024-01-01',
      updatedAt: '2024-01-01',
      version: 1,
      status: 'draft',
    };

    const newMapping: FieldMapping = {
      id: 'mapping-1',
      sourceField: 'source.field',
      targetField: 'target.field',
      mappingType: 'direct',
    };

    useBlueprintStore.getState().setCurrentBlueprint(mockBlueprint);
    useBlueprintStore.getState().addMapping(newMapping);

    const state = useBlueprintStore.getState();
    expect(state.currentBlueprint?.mappings).toHaveLength(1);
    expect(state.currentBlueprint?.mappings[0]).toEqual(newMapping);
    expect(state.isDirty).toBe(true);
  });

  it('should update mapping', () => {
    const mockMapping: FieldMapping = {
      id: 'mapping-1',
      sourceField: 'source.field',
      targetField: 'target.field',
      mappingType: 'direct',
    };

    const mockBlueprint: MessageBlueprint = {
      id: '1',
      name: 'Test Blueprint',
      sourceSystem: 'test-source',
      targetSchema: 'test-target',
      mappings: [mockMapping],
      createdAt: '2024-01-01',
      updatedAt: '2024-01-01',
      version: 1,
      status: 'draft',
    };

    useBlueprintStore.getState().setCurrentBlueprint(mockBlueprint);
    useBlueprintStore.getState().updateMapping('mapping-1', {
      mappingType: 'transform',
      jexlExpression: 'source.field * 2',
    });

    const state = useBlueprintStore.getState();
    expect(state.currentBlueprint?.mappings[0].mappingType).toBe('transform');
    expect(state.currentBlueprint?.mappings[0].jexlExpression).toBe('source.field * 2');
    expect(state.isDirty).toBe(true);
  });

  it('should delete mapping', () => {
    const mockMapping: FieldMapping = {
      id: 'mapping-1',
      sourceField: 'source.field',
      targetField: 'target.field',
      mappingType: 'direct',
    };

    const mockBlueprint: MessageBlueprint = {
      id: '1',
      name: 'Test Blueprint',
      sourceSystem: 'test-source',
      targetSchema: 'test-target',
      mappings: [mockMapping],
      createdAt: '2024-01-01',
      updatedAt: '2024-01-01',
      version: 1,
      status: 'draft',
    };

    useBlueprintStore.getState().setCurrentBlueprint(mockBlueprint);
    useBlueprintStore.getState().deleteMapping('mapping-1');

    const state = useBlueprintStore.getState();
    expect(state.currentBlueprint?.mappings).toHaveLength(0);
    expect(state.isDirty).toBe(true);
  });

  it('should reset blueprint to original', () => {
    const mockBlueprint: MessageBlueprint = {
      id: '1',
      name: 'Test Blueprint',
      sourceSystem: 'test-source',
      targetSchema: 'test-target',
      mappings: [],
      createdAt: '2024-01-01',
      updatedAt: '2024-01-01',
      version: 1,
      status: 'draft',
    };

    useBlueprintStore.getState().setCurrentBlueprint(mockBlueprint);
    useBlueprintStore.getState().updateBlueprint({ name: 'Updated Name' });
    useBlueprintStore.getState().resetBlueprint();

    const state = useBlueprintStore.getState();
    expect(state.currentBlueprint?.name).toBe('Test Blueprint');
    expect(state.isDirty).toBe(false);
  });

  it('should mark as saved', () => {
    const mockBlueprint: MessageBlueprint = {
      id: '1',
      name: 'Test Blueprint',
      sourceSystem: 'test-source',
      targetSchema: 'test-target',
      mappings: [],
      createdAt: '2024-01-01',
      updatedAt: '2024-01-01',
      version: 1,
      status: 'draft',
    };

    useBlueprintStore.getState().setCurrentBlueprint(mockBlueprint);
    useBlueprintStore.getState().updateBlueprint({ name: 'Updated Name' });
    useBlueprintStore.getState().markAsSaved();

    const state = useBlueprintStore.getState();
    expect(state.isDirty).toBe(false);
    expect(state.originalBlueprint?.name).toBe('Updated Name');
  });
});
