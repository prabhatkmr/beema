export type FieldType =
  | 'text'
  | 'number'
  | 'email'
  | 'date'
  | 'select'
  | 'checkbox'
  | 'radio'
  | 'textarea'
  | 'file';

export interface FieldBlock {
  id: string;
  type: FieldType;
  label: string;
  name: string;
  required: boolean;
  placeholder?: string;
  defaultValue?: any;
  validation?: ValidationRule[];
  options?: SelectOption[];
  gridColumn?: string;
  gridRow?: string;
  width?: 'full' | 'half' | 'third' | 'quarter';
}

export interface ValidationRule {
  type: 'required' | 'min' | 'max' | 'pattern' | 'email' | 'custom';
  value?: any;
  message: string;
}

export interface SelectOption {
  label: string;
  value: string;
}

export interface SysLayout {
  layout_id: string;
  layout_name: string;
  layout_type: 'form' | 'table' | 'dashboard';
  market_context: 'RETAIL' | 'COMMERCIAL' | 'LONDON_MARKET';
  fields: FieldBlock[];
  metadata: {
    version: number;
    created_at: string;
    updated_at: string;
    created_by: string;
  };
  grid_config?: {
    columns: number;
    gap: number;
  };
}

export interface FieldBlockDefinition {
  type: FieldType;
  icon: string;
  label: string;
  defaultProps: Partial<FieldBlock>;
}
