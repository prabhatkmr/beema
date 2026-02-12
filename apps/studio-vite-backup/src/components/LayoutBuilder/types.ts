export type FieldType =
  | 'text'
  | 'number'
  | 'date'
  | 'select'
  | 'checkbox'
  | 'radio'
  | 'textarea'
  | 'file';

export interface FieldDefinition {
  id: string;
  type: FieldType;
  label: string;
  name: string;
  required: boolean;
  placeholder?: string;
  defaultValue?: any;
  validation?: ValidationRule[];
  options?: SelectOption[];
  gridColumn?: number;
  gridRow?: number;
}

export interface SelectOption {
  label: string;
  value: string;
}

export interface ValidationRule {
  type: 'min' | 'max' | 'pattern' | 'custom';
  value: any;
  message: string;
}

export interface LayoutConfig {
  id: string;
  name: string;
  fields: FieldDefinition[];
  metadata: {
    createdAt: string;
    updatedAt: string;
    version: number;
  };
}

export interface FieldTemplate {
  type: FieldType;
  icon: string;
  label: string;
  defaultConfig: Partial<FieldDefinition>;
}
