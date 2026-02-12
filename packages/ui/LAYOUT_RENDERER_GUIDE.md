# LayoutRenderer Component Guide

## Overview
Generic, recursive component that renders any layout JSON from the server.

## Usage

```tsx
import { LayoutRenderer, useLayout } from '@beema/ui';

function MyForm() {
  const { layout } = useLayout({
    context: 'policy',
    objectType: 'motor_comprehensive',
  });

  return <LayoutRenderer schema={layout} onChange={handleChange} />;
}
```

## Supported Widgets

- TEXT_INPUT - Single-line text
- NUMBER_INPUT - Numeric input with min/max
- CURRENCY_INPUT - Currency formatting
- DATE_PICKER - Date selection
- SELECT - Dropdown selection
- CHECKBOX - Boolean toggle
- RADIO_GROUP - Single selection
- TEXTAREA - Multi-line text

## Props

```typescript
interface LayoutRendererProps {
  schema: LayoutSchema;         // Layout JSON from server
  data?: Record<string, any>;   // Initial/current form data
  onChange?: (fieldId, value) => void;  // Change handler
  readOnly?: boolean;            // Disable all inputs
}
```

## Layout JSON Structure

```json
{
  "title": "Form Title",
  "sections": [
    {
      "id": "section-1",
      "title": "Section Title",
      "layout": "grid",
      "columns": 2,
      "fields": [
        {
          "id": "field_1",
          "label": "Field Label",
          "widget": "TEXT_INPUT",
          "required": true
        }
      ]
    }
  ]
}
```

## Adding Custom Widgets

1. Create widget component:
```tsx
export function CustomWidget({ field, value, onChange, disabled }) {
  return <div>...</div>;
}
```

2. Register in WidgetRegistry:
```tsx
import { CustomWidget } from './widgets/CustomWidget';

WidgetRegistry['CUSTOM_WIDGET'] = CustomWidget;
```
