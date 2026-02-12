# Quick Start Guide: Layout Validation

## Setup

1. **Install dependencies**
   ```bash
   cd /Users/prabhatkumar/Desktop/dev-directory/beema/apps/studio
   pnpm install
   ```

2. **Configure environment**
   ```bash
   # .env.local is already created with default values
   # Edit if needed:
   nano .env.local
   ```

3. **Start the development server**
   ```bash
   pnpm dev
   ```

   Studio will be available at: http://localhost:3000

## Testing

### Option 1: Visual Testing (Recommended)

1. Open http://localhost:3000
2. Click "Open Canvas"
3. Drag and drop fields onto the canvas
4. Fill in field properties
5. Click "Save Layout"
6. Check for validation errors or success message

### Option 2: Automated API Testing

```bash
# Make sure Studio is running (pnpm dev)
cd /Users/prabhatkumar/Desktop/dev-directory/beema/apps/studio
./test-api.sh
```

This will run 8 automated tests covering:
- Valid layout validation
- Invalid layout validation
- Save operations
- Fetch operations
- Error handling

### Option 3: Manual API Testing

**Test Validation:**
```bash
curl -X POST http://localhost:3000/api/layouts/validate \
  -H "Content-Type: application/json" \
  -d @test-layout.json
```

**Test Save:**
```bash
curl -X POST http://localhost:3000/api/layouts/save \
  -H "Content-Type: application/json" \
  -d @test-layout.json
```

**Test Fetch:**
```bash
curl http://localhost:3000/api/layouts
```

## Testing with Beema Kernel

For full integration testing, start beema-kernel in a separate terminal:

```bash
# Terminal 1: Start beema-kernel
cd /Users/prabhatkumar/Desktop/dev-directory/beema/apps/beema-kernel
./mvnw spring-boot:run

# Terminal 2: Start Studio
cd /Users/prabhatkumar/Desktop/dev-directory/beema/apps/studio
pnpm dev
```

Now the validation will use both local and kernel validation!

## Common Validation Errors

### Error: "Layout name is required"
- **Fix**: Enter a layout name in the top-left input

### Error: "Field name must be alphanumeric with underscores"
- **Fix**: Use only letters, numbers, and underscores (e.g., `policy_number`)

### Error: "Duplicate field name"
- **Fix**: Ensure all field names are unique

### Error: "select requires at least one option"
- **Fix**: Add at least one option to select/radio fields

## Working Offline

If beema-kernel is not available, the Studio will still work with:
- Local validation only
- Warning messages about kernel unavailability
- Layouts saved locally (not persisted to database)

This is expected behavior and allows development without kernel dependency.

## Next Steps

1. Create your first layout in the Canvas
2. Export JSON to see the generated structure
3. Test validation with various field combinations
4. Integrate with beema-kernel for full persistence
5. Use the validation hook in custom components

## Files Created

```
apps/studio/
├── app/
│   ├── api/layouts/
│   │   ├── route.ts              # GET layouts
│   │   ├── save/route.ts         # POST save
│   │   └── validate/route.ts     # POST validate
│   └── canvas/page.tsx           # Updated with save
├── components/canvas/
│   └── ValidationPanel.tsx       # Validation UI
├── lib/hooks/
│   └── useLayoutValidation.ts    # Validation hook
├── .env.local                    # Environment config
├── .env.example                  # Template
├── test-layout.json              # Valid test data
├── test-invalid-layout.json      # Invalid test data
├── test-api.sh                   # Test script
├── QUICK_START.md               # This file
└── VALIDATION_INTEGRATION.md     # Detailed docs
```

## Support

For issues or questions:
1. Check VALIDATION_INTEGRATION.md for detailed documentation
2. Review the API README at app/api/layouts/README.md
3. Check browser console for client-side errors
4. Check terminal for server-side errors
