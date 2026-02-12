export { Button } from './components/Button';
export type { ButtonProps } from './components/Button';

export { Card, CardHeader, CardTitle, CardContent } from './components/Card';
export type { CardProps } from './components/Card';

export { Input } from './components/Input';
export type { InputProps } from './components/Input';

export { Label } from './components/Label';
export type { LabelProps } from './components/Label';

// Layout exports (legacy)
export { LayoutRenderer } from './components/LayoutRenderer';
export type { LayoutRendererProps } from './components/LayoutRenderer';

// Dynamic Layout Renderer (sys_layouts integration)
export { LayoutRenderer as DynamicLayoutRenderer } from './components/dynamic/LayoutRenderer';
export type { LayoutRendererProps as DynamicLayoutRendererProps } from './components/dynamic/LayoutRenderer';

export { useLayout } from './hooks/useLayout';
export type { UseLayoutOptions } from './hooks/useLayout';

export type {
  WidgetType,
  LayoutType,
  FieldDefinition,
  SectionDefinition,
  LayoutSchema,
  DynamicWidgetType,
  DynamicFieldDefinition,
  DynamicSectionDefinition,
  RegionDefinition,
  DynamicLayoutSchema,
} from './types/layout';
