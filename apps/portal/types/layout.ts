export type FieldType = 'TEXT' | 'TEXTAREA' | 'CURRENCY' | 'SELECT' | 'TOGGLE' | 'DATE' | 'DATE_PICKER';

export interface Field {
  id: string;
  label: string;
  type: FieldType;
  required: boolean;
  options?: string[];
  placeholder?: string;
  defaultValue?: any;
}

export interface Region {
  id: string;
  label: string;
  columns: number;
  fields: Field[];
}

export interface Layout {
  regions: Region[];
}

export interface LayoutResponse {
  id: string;
  product: string;
  version: number;
  layout: Layout;
}
