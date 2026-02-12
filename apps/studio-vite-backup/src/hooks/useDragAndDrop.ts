import { useState, useCallback } from 'react';
import {
  DndContext,
  DragEndEvent,
  DragStartEvent,
  DragOverlay,
  useSensor,
  useSensors,
  PointerSensor,
  KeyboardSensor,
} from '@dnd-kit/core';
import { DragItem } from '../types/mapping';

export const useDragAndDrop = () => {
  const [activeItem, setActiveItem] = useState<DragItem | null>(null);

  const sensors = useSensors(
    useSensor(PointerSensor, {
      activationConstraint: {
        distance: 8,
      },
    }),
    useSensor(KeyboardSensor)
  );

  const handleDragStart = useCallback((event: DragStartEvent) => {
    setActiveItem(event.active.data.current as DragItem);
  }, []);

  const handleDragEnd = useCallback((event: DragEndEvent) => {
    setActiveItem(null);
    return event;
  }, []);

  const handleDragCancel = useCallback(() => {
    setActiveItem(null);
  }, []);

  return {
    sensors,
    activeItem,
    handleDragStart,
    handleDragEnd,
    handleDragCancel,
  };
};

export const createDragItem = (
  id: string,
  type: 'source-field' | 'target-field',
  field: string,
  path: string,
  dataType: string
): DragItem => ({
  id,
  type,
  field,
  path,
  dataType,
});
