export type WidgetType =
  | 'TEXT_INPUT'
  | 'NUMBER_INPUT'
  | 'CURRENCY_INPUT'
  | 'PERCENTAGE_INPUT'
  | 'DATE_PICKER'
  | 'SELECT'
  | 'CHECKBOX'
  | 'RADIO_GROUP'
  | 'TEXTAREA'
  | 'FILE_UPLOAD'
  | 'SWITCH'
  | 'SLIDER'
  | 'RICH_TEXT';

export type LayoutType = 'grid' | 'stack' | 'tabs' | 'accordion';

export interface FieldDefinition {
  id: string;
  label: string;
  widget: WidgetType;
  required?: boolean;
  visible_if?: string;
  editable_if?: string;
  placeholder?: string;
  defaultValue?: any;
  validation?: {
    min?: number;
    max?: number;
    minLength?: number;
    maxLength?: number;
    pattern?: string;
  };
  computed?: string; // JEXL expression
  options?: Array<{ label: string; value: string | number }>;
}

export interface SectionDefinition {
  id: string;
  title: string;
  visible_if?: string;
  layout: LayoutType;
  columns?: number;
  fields: FieldDefinition[];
}

export interface LayoutSchema {
  title: string;
  sections: SectionDefinition[];
  _metadata?: {
    layoutId: string;
    layoutName: string;
    version: number;
    context: string;
  };
}
