import { FieldMapping } from './blueprint';

export interface DragItem {
  id: string;
  type: 'source-field' | 'target-field';
  field: string;
  path: string;
  dataType: string;
}

export interface DropResult {
  targetField: string;
  sourceField?: string;
}

export interface MappingConnection {
  id: string;
  mapping: FieldMapping;
  sourcePosition: Position;
  targetPosition: Position;
}

export interface Position {
  x: number;
  y: number;
}

export interface CanvasState {
  zoom: number;
  pan: Position;
}

export interface JexlContext {
  source: Record<string, any>;
  target?: Record<string, any>;
  constants?: Record<string, any>;
}

export interface JexlValidationResult {
  isValid: boolean;
  error?: string;
  result?: any;
}
