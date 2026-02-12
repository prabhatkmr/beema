import * as React from 'react';
import { LayoutSchema, SectionDefinition, FieldDefinition } from '../types/layout';
import { getWidgetComponent } from './WidgetRegistry';
import { Card, CardHeader, CardTitle, CardContent } from './Card';

export interface LayoutRendererProps {
  schema: LayoutSchema;
  data?: Record<string, any>;
  onChange?: (fieldId: string, value: any) => void;
  readOnly?: boolean;
}

export function LayoutRenderer({ schema, data = {}, onChange, readOnly }: LayoutRendererProps) {
  const [formData, setFormData] = React.useState<Record<string, any>>(data);

  React.useEffect(() => {
    setFormData(data);
  }, [data]);

  const handleFieldChange = (fieldId: string, value: any) => {
    const newData = { ...formData, [fieldId]: value };
    setFormData(newData);
    onChange?.(fieldId, value);
  };

  return (
    <div className="space-y-6">
      {/* Title */}
      {schema.title && (
        <h1 className="text-3xl font-bold">{schema.title}</h1>
      )}

      {/* Sections */}
      {schema.sections.map((section) => (
        <Section
          key={section.id}
          section={section}
          data={formData}
          onChange={handleFieldChange}
          readOnly={readOnly}
        />
      ))}
    </div>
  );
}

interface SectionProps {
  section: SectionDefinition;
  data: Record<string, any>;
  onChange: (fieldId: string, value: any) => void;
  readOnly?: boolean;
}

function Section({ section, data, onChange, readOnly }: SectionProps) {
  // TODO: Evaluate visible_if expression
  const isVisible = true; // Placeholder

  if (!isVisible) {
    return null;
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle>{section.title}</CardTitle>
      </CardHeader>
      <CardContent>
        <div
          className={`grid gap-4 ${
            section.layout === 'grid' ? `grid-cols-${section.columns || 1}` : 'grid-cols-1'
          }`}
        >
          {section.fields.map((field) => (
            <FieldRenderer
              key={field.id}
              field={field}
              value={data[field.id]}
              onChange={(value) => onChange(field.id, value)}
              readOnly={readOnly}
            />
          ))}
        </div>
      </CardContent>
    </Card>
  );
}

interface FieldRendererProps {
  field: FieldDefinition;
  value: any;
  onChange: (value: any) => void;
  readOnly?: boolean;
}

function FieldRenderer({ field, value, onChange, readOnly }: FieldRendererProps) {
  // TODO: Evaluate visible_if and editable_if expressions
  const isVisible = true; // Placeholder
  const isEditable = !readOnly; // Placeholder

  if (!isVisible) {
    return null;
  }

  const WidgetComponent = getWidgetComponent(field.widget);

  return (
    <WidgetComponent
      field={field}
      value={value}
      onChange={onChange}
      disabled={!isEditable}
    />
  );
}
