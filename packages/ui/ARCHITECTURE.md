# LayoutRenderer Architecture

## Component Hierarchy

```
┌─────────────────────────────────────────────────────────────┐
│                    Application Layer                         │
│  (Studio, Portal, or any Next.js/React app)                 │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       │ imports
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                   @beema/ui Package                          │
│                                                              │
│  ┌────────────────────────────────────────────────────┐     │
│  │  useLayout Hook                                    │     │
│  │  ├─ Fetches layout JSON from server                │     │
│  │  ├─ Handles loading/error states                   │     │
│  │  └─ Returns { layout, loading, error }             │     │
│  └────────────────┬───────────────────────────────────┘     │
│                   │                                          │
│                   │ provides layout                          │
│                   ▼                                          │
│  ┌────────────────────────────────────────────────────┐     │
│  │  LayoutRenderer Component                          │     │
│  │  ├─ Manages form state (formData)                  │     │
│  │  ├─ Renders title                                  │     │
│  │  └─ Maps over sections[]                           │     │
│  └────────────────┬───────────────────────────────────┘     │
│                   │                                          │
│                   │ for each section                         │
│                   ▼                                          │
│  ┌────────────────────────────────────────────────────┐     │
│  │  Section Component                                 │     │
│  │  ├─ Renders Card wrapper                           │     │
│  │  ├─ Applies grid/stack layout                      │     │
│  │  ├─ Evaluates visible_if (TODO)                    │     │
│  │  └─ Maps over fields[]                             │     │
│  └────────────────┬───────────────────────────────────┘     │
│                   │                                          │
│                   │ for each field                           │
│                   ▼                                          │
│  ┌────────────────────────────────────────────────────┐     │
│  │  FieldRenderer Component                           │     │
│  │  ├─ Evaluates visible_if/editable_if (TODO)        │     │
│  │  ├─ Resolves widget type via WidgetRegistry        │     │
│  │  └─ Renders appropriate widget                     │     │
│  └────────────────┬───────────────────────────────────┘     │
│                   │                                          │
│                   │ resolves widget                          │
│                   ▼                                          │
│  ┌────────────────────────────────────────────────────┐     │
│  │  WidgetRegistry                                    │     │
│  │  ├─ getWidgetComponent(widgetType)                 │     │
│  │  ├─ Maps WidgetType → React Component              │     │
│  │  └─ Fallback to TextInputWidget                    │     │
│  └────────────────┬───────────────────────────────────┘     │
│                   │                                          │
│                   │ returns widget component                 │
│                   ▼                                          │
│  ┌────────────────────────────────────────────────────┐     │
│  │  Widget Component (e.g., TextInputWidget)          │     │
│  │  ├─ Renders Label (with required indicator)        │     │
│  │  ├─ Renders Input/Select/Checkbox/etc.             │     │
│  │  ├─ Applies validation rules                       │     │
│  │  └─ Calls onChange(value) on user input            │     │
│  └────────────────┬───────────────────────────────────┘     │
│                   │                                          │
│                   │ onChange callback                        │
│                   ▼                                          │
│  ┌────────────────────────────────────────────────────┐     │
│  │  formData State (in LayoutRenderer)                │     │
│  │  ├─ Updates internal state                         │     │
│  │  └─ Calls onChange prop (if provided)              │     │
│  └────────────────────────────────────────────────────┘     │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## Data Flow

```
┌──────────────┐
│   Server     │
│  (Backend)   │
└──────┬───────┘
       │
       │ HTTP GET /api/v1/layouts/{context}/{objectType}
       │ Headers: X-Tenant-ID, X-User-Role
       │
       ▼
┌──────────────────────────────────────────────────────────┐
│  Layout JSON Schema                                      │
│  {                                                       │
│    "title": "Form Title",                               │
│    "sections": [                                        │
│      {                                                  │
│        "id": "section-1",                               │
│        "title": "Section Title",                        │
│        "layout": "grid",                                │
│        "columns": 2,                                    │
│        "fields": [...]                                  │
│      }                                                  │
│    ]                                                    │
│  }                                                      │
└──────────────────┬───────────────────────────────────────┘
                   │
                   │ useLayout hook fetches
                   │
                   ▼
┌──────────────────────────────────────────────────────────┐
│  React Component State                                   │
│  const { layout, loading, error } = useLayout(...)       │
└──────────────────┬───────────────────────────────────────┘
                   │
                   │ passes to
                   │
                   ▼
┌──────────────────────────────────────────────────────────┐
│  LayoutRenderer Component                                │
│  <LayoutRenderer schema={layout} onChange={...} />       │
└──────────────────┬───────────────────────────────────────┘
                   │
                   │ renders
                   │
                   ▼
┌──────────────────────────────────────────────────────────┐
│  Interactive Form UI                                     │
│  ┌──────────────────────────────────────────────────┐   │
│  │  Section: Vehicle Details                        │   │
│  │  ┌─────────────────┐  ┌─────────────────┐       │   │
│  │  │ Registration    │  │ Make            │       │   │
│  │  │ [ABC123______]  │  │ [Select v]      │       │   │
│  │  └─────────────────┘  └─────────────────┘       │   │
│  └──────────────────────────────────────────────────┘   │
└──────────────────┬───────────────────────────────────────┘
                   │
                   │ user interaction
                   │
                   ▼
┌──────────────────────────────────────────────────────────┐
│  onChange Callback                                       │
│  onChange(fieldId, value)                                │
│  ├─ Updates formData state                              │
│  └─ Calls parent onChange (optional)                     │
└──────────────────┬───────────────────────────────────────┘
                   │
                   │ can be used for
                   │
                   ▼
┌──────────────────────────────────────────────────────────┐
│  Application Logic                                       │
│  ├─ Form validation                                     │
│  ├─ API calls (autosave, etc.)                          │
│  ├─ State management (Redux, Zustand, etc.)             │
│  └─ Analytics tracking                                  │
└──────────────────────────────────────────────────────────┘
```

## File Structure

```
packages/ui/
├── index.tsx                          # Main exports
├── package.json                       # Package configuration
├── tsconfig.json                      # TypeScript config
│
├── types/
│   └── layout.ts                      # TypeScript type definitions
│       ├── WidgetType (union type)
│       ├── LayoutType (union type)
│       ├── FieldDefinition (interface)
│       ├── SectionDefinition (interface)
│       └── LayoutSchema (interface)
│
├── components/
│   ├── LayoutRenderer.tsx             # Main recursive renderer
│   │   ├── LayoutRenderer (main component)
│   │   ├── Section (sub-component)
│   │   └── FieldRenderer (sub-component)
│   │
│   ├── WidgetRegistry.tsx             # Widget type → component mapping
│   │   ├── WidgetRegistry (Record<WidgetType, ComponentType>)
│   │   └── getWidgetComponent(widgetType)
│   │
│   ├── widgets/
│   │   ├── TextInputWidget.tsx        # Text input field
│   │   ├── NumberInputWidget.tsx      # Number input field
│   │   ├── SelectWidget.tsx           # Dropdown select
│   │   ├── DatePickerWidget.tsx       # Date picker
│   │   └── CheckboxWidget.tsx         # Checkbox toggle
│   │
│   ├── Card.tsx                       # Card UI component
│   ├── Input.tsx                      # Input UI component
│   ├── Label.tsx                      # Label UI component
│   └── Button.tsx                     # Button UI component
│
├── hooks/
│   └── useLayout.ts                   # Data fetching hook
│       └── useLayout(options) → { layout, loading, error }
│
├── examples/
│   ├── LayoutRendererExample.tsx      # Complete working example
│   └── sample-layout-schema.json      # Sample JSON schema
│
└── docs/
    ├── QUICKSTART.md                  # Quick start guide
    ├── LAYOUT_RENDERER_GUIDE.md       # Detailed usage guide
    ├── IMPLEMENTATION_SUMMARY.md      # Architecture & design
    └── ARCHITECTURE.md                # This file
```

## Type System

```typescript
// Enums/Unions
WidgetType = 'TEXT_INPUT' | 'NUMBER_INPUT' | 'SELECT' | ...
LayoutType = 'grid' | 'stack' | 'tabs' | 'accordion'

// Field Definition
FieldDefinition {
  id: string                    // Unique field ID
  label: string                 // Display label
  widget: WidgetType            // Widget to render
  required?: boolean            // Is field required?
  visible_if?: string           // JEXL expression for visibility
  editable_if?: string          // JEXL expression for editability
  placeholder?: string          // Placeholder text
  defaultValue?: any            // Default value
  validation?: {                // Validation rules
    min?: number
    max?: number
    minLength?: number
    maxLength?: number
    pattern?: string
  }
  computed?: string             // JEXL expression for computed value
  options?: Array<{             // Options for SELECT/RADIO
    label: string
    value: string | number
  }>
}

// Section Definition
SectionDefinition {
  id: string                    // Unique section ID
  title: string                 // Section title
  visible_if?: string           // JEXL expression for visibility
  layout: LayoutType            // Layout mode
  columns?: number              // Grid columns (for grid layout)
  fields: FieldDefinition[]     // Array of fields
}

// Layout Schema
LayoutSchema {
  title: string                 // Form title
  sections: SectionDefinition[] // Array of sections
  _metadata?: {                 // Optional metadata
    layoutId: string
    layoutName: string
    version: number
    context: string
  }
}
```

## Widget Extension Pattern

To add a new widget:

1. **Create widget component**:
```tsx
// components/widgets/RichTextWidget.tsx
export function RichTextWidget({ field, value, onChange, disabled }) {
  return <RichTextEditor value={value} onChange={onChange} />;
}
```

2. **Update WidgetType**:
```tsx
// types/layout.ts
export type WidgetType = 
  | 'TEXT_INPUT'
  | 'RICH_TEXT'  // ← Add here
  | ...
```

3. **Register in WidgetRegistry**:
```tsx
// components/WidgetRegistry.tsx
import { RichTextWidget } from './widgets/RichTextWidget';

WidgetRegistry['RICH_TEXT'] = RichTextWidget;
```

4. **Export from package**:
```tsx
// index.tsx (if needed for external use)
export { RichTextWidget } from './components/widgets/RichTextWidget';
```

## State Management Patterns

### Internal State (Default)
```tsx
<LayoutRenderer schema={layout} onChange={handleChange} />
// LayoutRenderer manages formData internally
```

### External State (Controlled)
```tsx
const [formData, setFormData] = useState({});

<LayoutRenderer
  schema={layout}
  data={formData}                    // Provide data
  onChange={(id, val) => {
    setFormData(prev => ({ ...prev, [id]: val }));
  }}
/>
```

### Integration with React Hook Form
```tsx
const { register, setValue, watch } = useForm();

<LayoutRenderer
  schema={layout}
  data={watch()}
  onChange={(id, val) => setValue(id, val)}
/>
```

## Performance Optimization Strategies

### 1. Component Memoization
```tsx
const MemoizedSection = React.memo(Section);
const MemoizedFieldRenderer = React.memo(FieldRenderer);
```

### 2. Lazy Widget Loading
```tsx
const RichTextWidget = React.lazy(() => import('./widgets/RichTextWidget'));
```

### 3. Virtual Scrolling (for large forms)
```tsx
import { useVirtualizer } from '@tanstack/react-virtual';

// Virtualize section rendering for forms with 50+ sections
```

### 4. Debounced onChange
```tsx
import { useDebouncedCallback } from 'use-debounce';

const debouncedChange = useDebouncedCallback(
  (id, val) => onChange(id, val),
  300
);
```

## Security Considerations

1. **XSS Prevention**: All user input is sanitized by React
2. **JEXL Sandbox**: Expression evaluation should be sandboxed
3. **CSRF Protection**: API calls should include CSRF tokens
4. **Content Security Policy**: Restrict inline scripts
5. **Input Validation**: Server-side validation required

## Testing Strategy

### Unit Tests
- Individual widget components
- WidgetRegistry resolution
- useLayout hook with mock fetch

### Integration Tests
- Full LayoutRenderer with complex schemas
- Multi-section forms
- onChange callbacks

### E2E Tests
- Complete form submission flows
- Different market contexts
- Role-based layout variations

## Browser Compatibility

- Chrome/Edge: ✅ Full support
- Firefox: ✅ Full support
- Safari: ✅ Full support
- IE11: ❌ Not supported (uses modern React features)

## Accessibility (a11y)

- ✅ Keyboard navigation
- ✅ Screen reader support (ARIA labels)
- ✅ Focus management
- ✅ Required field indicators
- ✅ Error announcements (TODO)

## Internationalization (i18n)

Future enhancement for multi-language support:
```json
{
  "id": "field_1",
  "label": {
    "en": "Full Name",
    "fr": "Nom Complet",
    "de": "Vollständiger Name"
  }
}
```
