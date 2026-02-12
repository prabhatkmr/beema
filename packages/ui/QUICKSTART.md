# LayoutRenderer Quick Start Guide

## 1. Installation

The `@beema/ui` package is already available in your monorepo. Import it in your application:

```tsx
import { LayoutRenderer, useLayout } from '@beema/ui';
```

## 2. Basic Usage (3 Steps)

### Step 1: Fetch Layout Schema

```tsx
const { layout, loading, error } = useLayout({
  context: 'policy',
  objectType: 'motor_comprehensive',
  marketContext: 'RETAIL',
});
```

### Step 2: Handle Loading & Error States

```tsx
if (loading) return <div>Loading form...</div>;
if (error) return <div>Error: {error.message}</div>;
if (!layout) return <div>No layout found</div>;
```

### Step 3: Render the Layout

```tsx
return <LayoutRenderer schema={layout} onChange={handleChange} />;
```

## 3. Complete Example

```tsx
'use client';

import React from 'react';
import { LayoutRenderer, useLayout } from '@beema/ui';

export default function PolicyFormPage() {
  // Fetch layout from server
  const { layout, loading, error } = useLayout({
    context: 'policy',
    objectType: 'motor_comprehensive',
    marketContext: 'RETAIL',
  });

  // Track form data
  const [formData, setFormData] = React.useState({});

  // Handle field changes
  const handleChange = (fieldId: string, value: any) => {
    setFormData(prev => ({ ...prev, [fieldId]: value }));
    console.log(`Field ${fieldId} changed to:`, value);
  };

  // Loading state
  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-lg">Loading policy form...</div>
      </div>
    );
  }

  // Error state
  if (error) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-red-500">Error: {error.message}</div>
      </div>
    );
  }

  // No layout found
  if (!layout) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div>No layout configuration found</div>
      </div>
    );
  }

  // Render form
  return (
    <div className="container mx-auto py-8 px-4">
      <LayoutRenderer
        schema={layout}
        data={formData}
        onChange={handleChange}
      />

      {/* Optional: Debug panel */}
      {process.env.NODE_ENV === 'development' && (
        <div className="mt-8 p-4 bg-gray-100 rounded">
          <h3 className="font-bold mb-2">Form Data (Debug)</h3>
          <pre className="text-xs overflow-auto">
            {JSON.stringify(formData, null, 2)}
          </pre>
        </div>
      )}
    </div>
  );
}
```

## 4. Advanced Features

### Read-Only Mode

```tsx
<LayoutRenderer
  schema={layout}
  data={formData}
  readOnly={true}  // Disables all inputs
/>
```

### Multi-Tenant Support

```tsx
const { layout } = useLayout({
  context: 'policy',
  objectType: 'motor_comprehensive',
  tenantId: 'tenant-123',      // Multi-tenant isolation
  userRole: 'underwriter',      // Role-based layouts
});
```

### External State Management (React Hook Form)

```tsx
import { useForm } from 'react-hook-form';

function PolicyForm() {
  const { register, setValue, watch } = useForm();
  const formData = watch();

  const handleChange = (fieldId: string, value: any) => {
    setValue(fieldId, value);
  };

  return (
    <LayoutRenderer
      schema={layout}
      data={formData}
      onChange={handleChange}
    />
  );
}
```

### Server-Side Rendering (Next.js)

```tsx
// app/policy/new/page.tsx
import { LayoutRenderer } from '@beema/ui';

async function fetchLayout() {
  const res = await fetch(
    'http://localhost:8080/api/v1/layouts/policy/motor_comprehensive',
    { cache: 'no-store' }
  );
  return res.json();
}

export default async function NewPolicyPage() {
  const layout = await fetchLayout();

  return <LayoutRenderer schema={layout} />;
}
```

## 5. Sample Layout Schema

See `/Users/prabhatkumar/Desktop/dev-directory/beema/packages/ui/examples/sample-layout-schema.json` for a complete example of what the server should return.

## 6. API Endpoint Format

The `useLayout` hook expects the following endpoint:

```
GET /api/v1/layouts/{context}/{objectType}?marketContext={marketContext}
Headers:
  X-Tenant-ID: {tenantId}
  X-User-Role: {userRole}

Response:
{
  "title": "Form Title",
  "sections": [...],
  "_metadata": {...}
}
```

## 7. Environment Variables

Set the API URL in your application:

```bash
# .env.local
NEXT_PUBLIC_API_URL=http://localhost:8080
```

## 8. Customization

### Adding Custom Widgets

1. Create your widget component:

```tsx
// components/widgets/CustomWidget.tsx
import { FieldDefinition } from '@beema/ui';

export function CustomWidget({ field, value, onChange, disabled }) {
  return (
    <div>
      <label>{field.label}</label>
      <input
        value={value}
        onChange={(e) => onChange(e.target.value)}
        disabled={disabled}
      />
    </div>
  );
}
```

2. Register in WidgetRegistry:

```tsx
import { WidgetRegistry } from '@beema/ui/components/WidgetRegistry';
import { CustomWidget } from './widgets/CustomWidget';

WidgetRegistry['CUSTOM_WIDGET'] = CustomWidget;
```

## 9. TypeScript Support

Full type safety with IntelliSense:

```tsx
import type {
  LayoutSchema,
  FieldDefinition,
  SectionDefinition,
  WidgetType,
} from '@beema/ui';

const schema: LayoutSchema = {
  title: 'My Form',
  sections: [
    {
      id: 'section-1',
      title: 'Section Title',
      layout: 'grid',
      columns: 2,
      fields: [
        {
          id: 'field_1',
          label: 'Field Label',
          widget: 'TEXT_INPUT',
          required: true,
        },
      ],
    },
  ],
};
```

## 10. Troubleshooting

### Layout not loading?
- Check `NEXT_PUBLIC_API_URL` environment variable
- Verify API endpoint is accessible
- Check browser console for CORS errors

### Widget not rendering?
- Verify widget type is in `WidgetType` union
- Check WidgetRegistry for mapping
- Falls back to TextInput if widget not found

### Styling issues?
- Ensure Tailwind CSS is configured
- Check className overrides aren't conflicting
- Verify design system components (Card, Input, Label) are styled

## 11. Next Steps

- Review `/Users/prabhatkumar/Desktop/dev-directory/beema/packages/ui/LAYOUT_RENDERER_GUIDE.md` for detailed documentation
- Check `/Users/prabhatkumar/Desktop/dev-directory/beema/packages/ui/examples/LayoutRendererExample.tsx` for complete example
- See `/Users/prabhatkumar/Desktop/dev-directory/beema/packages/ui/IMPLEMENTATION_SUMMARY.md` for architecture details

## Need Help?

Refer to the documentation files:
- `LAYOUT_RENDERER_GUIDE.md` - Component usage guide
- `IMPLEMENTATION_SUMMARY.md` - Architecture and design decisions
- `examples/` - Working code examples
