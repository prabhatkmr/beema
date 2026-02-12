export interface MessageBlueprint {
  id: string;
  name: string;
  description?: string;
  sourceSystem: string;
  targetSchema: string;
  mappings: FieldMapping[];
  createdAt: string;
  updatedAt: string;
  version: number;
  status: 'draft' | 'active' | 'archived';
}

export interface FieldMapping {
  id: string;
  sourceField?: string;
  targetField: string;
  mappingType: 'direct' | 'transform' | 'constant' | 'conditional';
  jexlExpression?: string;
  constantValue?: any;
  validationRules?: ValidationRule[];
  description?: string;
}

export interface ValidationRule {
  type: 'required' | 'type' | 'range' | 'pattern' | 'custom';
  value?: any;
  message: string;
}

export interface SourceField {
  name: string;
  path: string;
  type: string;
  description?: string;
  example?: any;
}

export interface TargetField {
  name: string;
  path: string;
  type: string;
  required: boolean;
  description?: string;
}

export interface BlueprintValidationResult {
  isValid: boolean;
  errors: ValidationError[];
  warnings: ValidationWarning[];
}

export interface ValidationError {
  field: string;
  message: string;
  code: string;
}

export interface ValidationWarning {
  field: string;
  message: string;
}

export interface TestResult {
  success: boolean;
  output?: any;
  errors?: TestError[];
  executionTime: number;
}

export interface TestError {
  mapping: string;
  message: string;
  input?: any;
}
