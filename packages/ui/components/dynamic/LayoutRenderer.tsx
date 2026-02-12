'use client';

import * as React from 'react';
import { Parser } from 'expr-eval';
import {
  DynamicLayoutSchema,
  RegionDefinition,
  DynamicSectionDefinition,
  DynamicFieldDefinition,
} from '../../types/layout';
import { Input } from '../Input';
import { Label } from '../Label';
import { Card, CardHeader, CardTitle, CardContent } from '../Card';

export interface LayoutRendererProps {
  layout: DynamicLayoutSchema;
  data: Record<string, any>;
  onChange: (fieldId: string, value: any) => void;
  readOnly?: boolean;
}

const GRID_COLS: Record<number, string> = {
  1: 'grid-cols-1',
  2: 'grid-cols-2',
  3: 'grid-cols-3',
  4: 'grid-cols-4',
  5: 'grid-cols-5',
  6: 'grid-cols-6',
};

function getGridClass(layout?: 'grid' | 'stack', columns?: number): string {
  if (layout === 'grid' && columns) {
    return GRID_COLS[columns] || 'grid-cols-1';
  }
  return 'grid-cols-1';
}

export function LayoutRenderer({
  layout,
  data,
  onChange,
  readOnly = false,
}: LayoutRendererProps) {
  const parser = React.useMemo(() => new Parser(), []);

  const isVisible = React.useCallback(
    (expression?: string): boolean => {
      if (!expression) return true;
      try {
        return !!parser.evaluate(expression, data);
      } catch {
        return true;
      }
    },
    [parser, data],
  );

  const renderWidget = (
    field: DynamicFieldDefinition,
    value: any,
    isReadOnly: boolean,
  ) => {
    switch (field.widget_type) {
      case 'TEXT':
        return (
          <Input
            id={field.id}
            type="text"
            value={value || ''}
            onChange={(e) => onChange(field.id, e.target.value)}
            disabled={isReadOnly}
            aria-required={field.required}
          />
        );

      case 'TEXTAREA':
        return (
          <textarea
            id={field.id}
            value={value || ''}
            onChange={(e) => onChange(field.id, e.target.value)}
            disabled={isReadOnly}
            aria-required={field.required}
            rows={4}
            className="flex min-h-[80px] w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-blue-500 focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
          />
        );

      case 'NUMBER':
        return (
          <Input
            id={field.id}
            type="number"
            value={value ?? ''}
            onChange={(e) =>
              onChange(
                field.id,
                e.target.value ? parseFloat(e.target.value) : null,
              )
            }
            disabled={isReadOnly}
            aria-required={field.required}
            min={field.validation?.min}
            max={field.validation?.max}
          />
        );

      case 'CURRENCY':
        return (
          <Input
            id={field.id}
            type="number"
            step="0.01"
            value={value ?? ''}
            onChange={(e) =>
              onChange(
                field.id,
                e.target.value ? parseFloat(e.target.value) : null,
              )
            }
            disabled={isReadOnly}
            aria-required={field.required}
            min={field.validation?.min}
            max={field.validation?.max}
          />
        );

      case 'DATE':
        return (
          <Input
            id={field.id}
            type="date"
            value={value || ''}
            onChange={(e) => onChange(field.id, e.target.value)}
            disabled={isReadOnly}
            aria-required={field.required}
          />
        );

      case 'SELECT':
        return (
          <select
            id={field.id}
            value={value || ''}
            onChange={(e) => onChange(field.id, e.target.value)}
            disabled={isReadOnly}
            aria-required={field.required}
            className="flex h-10 w-full rounded-md border border-gray-300 bg-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <option value="">Select...</option>
            {field.options?.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        );

      case 'CHECKBOX':
        return (
          <input
            type="checkbox"
            id={field.id}
            checked={!!value}
            onChange={(e) => onChange(field.id, e.target.checked)}
            disabled={isReadOnly}
            aria-required={field.required}
            className="h-4 w-4 rounded border-gray-300 text-blue-600 focus:ring-2 focus:ring-blue-500"
          />
        );

      default:
        return (
          <Input
            id={field.id}
            value={value || ''}
            onChange={(e) => onChange(field.id, e.target.value)}
            disabled={isReadOnly}
          />
        );
    }
  };

  const renderField = (field: DynamicFieldDefinition) => {
    if (!isVisible(field.visible_if)) return null;

    const value = data[field.id];
    const isReadOnly = readOnly || !!field.readonly;

    // Checkboxes render with inline label for better UX
    if (field.widget_type === 'CHECKBOX') {
      return (
        <div key={field.id} className="flex items-center space-x-2">
          {renderWidget(field, value, isReadOnly)}
          <Label htmlFor={field.id} required={field.required}>
            {field.label}
          </Label>
        </div>
      );
    }

    return (
      <div key={field.id} className="space-y-2">
        <Label htmlFor={field.id} required={field.required}>
          {field.label}
        </Label>
        {renderWidget(field, value, isReadOnly)}
      </div>
    );
  };

  const renderSection = (section: DynamicSectionDefinition) => {
    if (!isVisible(section.visible_if)) return null;

    return (
      <Card key={section.id}>
        <CardHeader>
          <CardTitle>{section.title}</CardTitle>
        </CardHeader>
        <CardContent>
          <div className={`grid gap-4 ${getGridClass(section.layout, section.columns)}`}>
            {section.fields.map(renderField)}
          </div>
        </CardContent>
      </Card>
    );
  };

  const renderRegion = (region: RegionDefinition) => {
    if (!isVisible(region.visible_if)) return null;

    return (
      <div key={region.id} className="space-y-4">
        {region.title && (
          <h2 className="text-2xl font-bold">{region.title}</h2>
        )}
        <div className={`grid gap-4 ${getGridClass(region.layout, region.columns)}`}>
          {region.sections.map(renderSection)}
        </div>
      </div>
    );
  };

  return (
    <div className="space-y-6">
      {layout.title && <h1 className="text-3xl font-bold">{layout.title}</h1>}
      {layout.regions?.map(renderRegion)}
      {layout.sections?.map(renderSection)}
    </div>
  );
}
