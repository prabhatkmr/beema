# Layout Validation Integration Guide

## Overview

This document describes the layout validation system that integrates the Beema Studio Canvas with the beema-kernel metadata schema.

## Architecture

```
┌─────────────────┐
│  Canvas Page    │
│  (User Input)   │
└────────┬────────┘
         │
         │ generateSysLayout()
         ▼
┌─────────────────────┐
│  Save Layout API    │
│  /api/layouts/save  │
└──────────┬──────────┘
           │
           ├─────────────────────┐
           │                     │
           ▼                     ▼
┌──────────────────┐   ┌─────────────────┐
│  Validate API    │   │  Beema Kernel   │
│  /api/layouts/   │   │  POST /layouts  │
│  validate        │   └─────────────────┘
└──────────────────┘
           │
           ├──────────────┬──────────────┐
           │              │              │
           ▼              ▼              ▼
┌─────────────┐  ┌──────────────┐  ┌─────────────┐
│   Local     │  │   Kernel     │  │  Fallback   │
│ Validation  │  │  Validation  │  │  (offline)  │
└─────────────┘  └──────────────┘  └─────────────┘
```

## Components

### 1. API Routes

#### `/app/api/layouts/validate/route.ts`
- **Purpose**: Validates layout JSON against local rules and beema-kernel schema
- **Method**: POST
- **Input**: SysLayout JSON
- **Output**: Validation result with errors/warnings
- **Features**:
  - Local validation (required fields, name format, duplicates)
  - Remote validation (beema-kernel metadata schema)
  - Graceful degradation if kernel unavailable

#### `/app/api/layouts/save/route.ts`
- **Purpose**: Validates and saves layout to beema-kernel
- **Method**: POST
- **Input**: SysLayout JSON
- **Output**: Success/failure with layout_id
- **Features**:
  - Calls validate endpoint first
  - Saves to beema-kernel if validation passes
  - Fallback to local storage if kernel unavailable

#### `/app/api/layouts/route.ts`
- **Purpose**: Fetches layouts from beema-kernel
- **Method**: GET
- **Query Parameters**: marketContext, layoutType
- **Output**: Array of layouts
- **Features**:
  - Filter by market context and layout type
  - Returns empty array if kernel unavailable

### 2. Validation Logic

#### Local Validation Rules
- Layout name is required
- Layout type is required
- Market context is required
- Fields must be an array
- Each field must have:
  - type
  - name (alphanumeric + underscores only)
  - label
- No duplicate field names
- Select/radio fields must have options

#### Kernel Validation
- Validates against metadata schema in beema-kernel
- Checks field compatibility with market context
- Validates layout structure
- Returns specific error messages

### 3. UI Components

#### `ValidationPanel.tsx`
- Displays validation errors and warnings
- Dismissible panel in bottom-right corner
- Color-coded (red for errors, yellow for warnings)

#### Updated `Canvas Page`
- Integrated save functionality
- Real-time validation feedback
- Loading state during save
- Error/warning display

### 4. React Hook

#### `useLayoutValidation.ts`
- Reusable validation hook
- Returns: `{ validate, isValidating, validationResult }`
- Can be used in any component for validation

## Environment Configuration

### `.env.local` (not committed)
```bash
BEEMA_KERNEL_URL=http://localhost:8080
NEXT_PUBLIC_API_URL=http://localhost:3000
```

### `.env.example` (committed)
```bash
BEEMA_KERNEL_URL=http://localhost:8080
NEXT_PUBLIC_API_URL=http://localhost:3000
```

## Error Handling

### Graceful Degradation
1. If beema-kernel is unavailable (404, 503, or network error):
   - Validation: Uses local validation only, adds warning
   - Save: Saves locally, adds warning about persistence
   - Fetch: Returns empty array with message

2. If validation fails:
   - Displays specific error messages
   - Prevents save operation
   - Shows validation panel with errors

3. If save fails:
   - Displays error message
   - Layout remains in canvas (not lost)
   - User can fix issues and retry

## Testing

### Manual Testing Steps

1. **Test Local Validation**
   ```bash
   cd /Users/prabhatkumar/Desktop/dev-directory/beema/apps/studio
   pnpm dev
   ```
   - Open http://localhost:3000/canvas
   - Add fields without names → Should show error
   - Add duplicate field names → Should show error
   - Add select field without options → Should show error

2. **Test Kernel Integration (Online Mode)**
   ```bash
   # Terminal 1: Start beema-kernel
   cd /Users/prabhatkumar/Desktop/dev-directory/beema/apps/beema-kernel
   ./mvnw spring-boot:run

   # Terminal 2: Start Studio
   cd /Users/prabhatkumar/Desktop/dev-directory/beema/apps/studio
   pnpm dev
   ```
   - Create valid layout → Should save successfully
   - Check kernel database for saved layout

3. **Test Offline Mode**
   - Ensure beema-kernel is NOT running
   - Create layout and save
   - Should show warning: "Beema kernel unavailable - layout validated locally only"
   - Should still allow save with local validation

4. **Test Validation API Directly**
   ```bash
   curl -X POST http://localhost:3000/api/layouts/validate \
     -H "Content-Type: application/json" \
     -d '{
       "layout_name": "Test",
       "layout_type": "form",
       "market_context": "RETAIL",
       "fields": []
     }'
   ```

5. **Test Save API Directly**
   ```bash
   curl -X POST http://localhost:3000/api/layouts/save \
     -H "Content-Type: application/json" \
     -d '{
       "layout_id": "123e4567-e89b-12d3-a456-426614174000",
       "layout_name": "Test Layout",
       "layout_type": "form",
       "market_context": "RETAIL",
       "fields": [{
         "id": "f1",
         "type": "text",
         "name": "test_field",
         "label": "Test Field",
         "required": false
       }],
       "metadata": {
         "version": 1,
         "created_at": "2026-02-12T00:00:00Z",
         "updated_at": "2026-02-12T00:00:00Z",
         "created_by": "test"
       }
     }'
   ```

6. **Test Fetch API**
   ```bash
   # Fetch all layouts
   curl http://localhost:3000/api/layouts

   # Filter by market context
   curl "http://localhost:3000/api/layouts?marketContext=RETAIL"

   # Filter by layout type
   curl "http://localhost:3000/api/layouts?layoutType=form"
   ```

## File Structure

```
apps/studio/
├── app/
│   ├── api/
│   │   └── layouts/
│   │       ├── README.md           # API documentation
│   │       ├── route.ts            # GET layouts
│   │       ├── save/
│   │       │   └── route.ts        # POST save layout
│   │       └── validate/
│   │           └── route.ts        # POST validate layout
│   └── canvas/
│       └── page.tsx                # Updated with save functionality
├── components/
│   └── canvas/
│       └── ValidationPanel.tsx     # Validation feedback UI
├── lib/
│   └── hooks/
│       └── useLayoutValidation.ts  # Validation hook
├── .env.local                      # Local environment (not committed)
├── .env.example                    # Environment template
└── VALIDATION_INTEGRATION.md       # This file
```

## Integration with Beema Kernel

The Studio expects the following beema-kernel endpoints:

### Validate Layout
```
POST /api/v1/metadata/validate-layout
Content-Type: application/json

{
  "layout_name": "...",
  "layout_type": "...",
  "market_context": "...",
  "fields": [...]
}

Response:
{
  "valid": true/false,
  "errors": ["..."],
  "warnings": ["..."]
}
```

### Save Layout
```
POST /api/v1/layouts
Content-Type: application/json

{
  "layout_id": "...",
  "layout_name": "...",
  ...
}

Response:
{
  "layout_id": "...",
  "layout_name": "...",
  ...
}
```

### Get Layouts
```
GET /api/v1/layouts?marketContext=RETAIL&layoutType=form

Response:
[
  {
    "layout_id": "...",
    "layout_name": "...",
    ...
  }
]
```

## Best Practices

1. **Always validate before saving**: The save endpoint automatically validates, but you can call validate independently for real-time feedback.

2. **Handle warnings gracefully**: Warnings don't prevent save operations but inform users of potential issues.

3. **Test offline mode**: Ensure your layouts work even when beema-kernel is unavailable.

4. **Use the validation hook**: For custom validation logic, use `useLayoutValidation` hook.

5. **Field naming convention**: Use lowercase with underscores (e.g., `policy_number`, not `policyNumber`).

## Troubleshooting

### Issue: "Could not connect to beema-kernel"
- **Cause**: Beema kernel is not running
- **Solution**: Start beema-kernel or work in offline mode

### Issue: "Field name must be alphanumeric with underscores"
- **Cause**: Invalid field name format
- **Solution**: Use only letters, numbers, and underscores

### Issue: "Duplicate field name"
- **Cause**: Multiple fields with the same name
- **Solution**: Ensure all field names are unique

### Issue: "select requires at least one option"
- **Cause**: Select/radio field has no options
- **Solution**: Add at least one option to the field

## Future Enhancements

1. Real-time validation as user types
2. Auto-save drafts to local storage
3. Version history and rollback
4. Layout templates and presets
5. Bulk validation for multiple layouts
6. Schema validation UI (show expected vs actual)
7. Import layouts from JSON files
8. Collaborative editing support
