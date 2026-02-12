# Beema Studio - Message Blueprint Editor

A visual, drag-and-drop message mapping tool that allows users to create transformation blueprints between external message formats and Beema's internal schema.

## Features

### Core Functionality
- **Visual Drag & Drop Mapping**: Intuitive interface for mapping source fields to target fields
- **JEXL Expression Editor**: Monaco-powered editor with syntax highlighting and real-time validation
- **Multiple Mapping Types**: Support for direct, transform, constant, and conditional mappings
- **Blueprint Management**: Create, edit, clone, and delete message blueprints
- **Live Preview & Testing**: Test mappings with sample data and see real-time results
- **Validation**: Built-in validation for JEXL expressions and blueprint completeness

### User Experience
- Clean, modern UI built with Tailwind CSS
- Responsive design for desktop and laptop screens
- Real-time feedback with toast notifications
- Keyboard shortcuts for power users
- Auto-save indicators for unsaved changes

## Technology Stack

- **Frontend Framework**: React 18+ with TypeScript
- **Build Tool**: Vite for fast development and optimized production builds
- **Drag & Drop**: @dnd-kit for accessible, performant drag-and-drop
- **Code Editor**: Monaco Editor (VS Code editor) for JEXL expressions
- **State Management**: Zustand for lightweight, scalable state
- **Data Fetching**: TanStack Query (React Query) for server state
- **Styling**: Tailwind CSS for utility-first styling
- **HTTP Client**: Axios with interceptors
- **Icons**: Lucide React for consistent iconography

## Getting Started

### Prerequisites

- Node.js 18+
- pnpm 8+
- Beema Kernel API running on http://localhost:8080

### Installation

```bash
# Install dependencies
pnpm install

# Copy environment variables
cp .env.example .env

# Start development server
pnpm dev
```

The application will be available at http://localhost:3000

### Environment Variables

```env
# API base URL (default: http://localhost:8080/api/v1)
VITE_API_BASE_URL=http://localhost:8080/api/v1

# Enable development mode
VITE_DEV_MODE=true
```

## Building Blueprints

### Step 1: Create a New Blueprint

1. Click "New Blueprint" in the sidebar
2. Enter blueprint details:
   - **Name**: Descriptive name (e.g., "Lloyd's Quote to Beema Policy")
   - **Source System**: External system identifier
   - **Target Schema**: Beema internal schema name

### Step 2: Map Fields

#### Direct Mapping
Drag a source field and drop it on a target field. This creates a 1:1 mapping.

```
source.policyNumber → target.policyId
```

#### Transform Mapping
1. Create a direct mapping first
2. Select the mapping in the bottom panel
3. Write a JEXL expression in the editor

Example transformations:

```javascript
// Concatenate strings
source.firstName + " " + source.lastName

// Arithmetic operations
source.premium * 1.05

// Default values
source.field || "default_value"

// Conditional logic
source.amount > 1000 ? "high" : "low"

// Date formatting (with helper functions)
formatDate(source.effectiveDate, "YYYY-MM-DD")

// Type conversion
parseInt(source.stringNumber)
```

#### Constant Mapping
1. Select a target field
2. Change mapping type to "Constant"
3. Enter the constant value

```
"ACTIVE" → target.status
```

#### Conditional Mapping
Use JEXL ternary operators for conditional logic:

```javascript
source.type === "commercial" ? source.commercialPremium : source.retailPremium
```

### Step 3: Validate Blueprint

Click the "Validate" button in the header to check:
- All required target fields are mapped
- JEXL expressions are syntactically correct
- Data types are compatible

### Step 4: Test with Sample Data

1. Click "Test" in the header
2. Paste sample JSON data from the source system
3. Review the transformed output
4. Fix any errors and re-test

### Step 5: Save and Deploy

1. Click "Save" to persist the blueprint
2. Set status to "Active" to enable in production
3. The blueprint is now available for message processing

## JEXL Expression Syntax

### Basic Operators

```javascript
// Arithmetic
source.a + source.b    // Addition
source.a - source.b    // Subtraction
source.a * source.b    // Multiplication
source.a / source.b    // Division
source.a % source.b    // Modulus

// Comparison
source.a == source.b   // Equal
source.a != source.b   // Not equal
source.a > source.b    // Greater than
source.a >= source.b   // Greater than or equal
source.a < source.b    // Less than
source.a <= source.b   // Less than or equal

// Logical
source.a && source.b   // AND
source.a || source.b   // OR
!source.a              // NOT

// Ternary
source.condition ? "yes" : "no"
```

### Object and Array Access

```javascript
// Dot notation
source.policy.number

// Bracket notation
source["policy"]["number"]

// Array access
source.coverages[0].type

// Array filtering
source.coverages[.type == "AUTO"]
```

### Common Patterns

```javascript
// Null coalescing
source.field || "default"

// String concatenation
source.firstName + " " + source.lastName

// Nested ternary
source.type == "retail" ? source.retail.premium :
  source.type == "commercial" ? source.commercial.premium : 0

// Object construction (requires backend support)
{
  fullName: source.firstName + " " + source.lastName,
  age: 2024 - source.birthYear
}
```

## API Integration

### Endpoints Used

```
GET    /api/v1/blueprints              # List all blueprints
GET    /api/v1/blueprints/{id}         # Get blueprint by ID
POST   /api/v1/blueprints              # Create new blueprint
PUT    /api/v1/blueprints/{id}         # Update blueprint
DELETE /api/v1/blueprints/{id}         # Delete blueprint
POST   /api/v1/blueprints/{id}/clone   # Clone blueprint
POST   /api/v1/blueprints/{id}/validate # Validate blueprint
POST   /api/v1/blueprints/{id}/test    # Test with sample data

GET    /api/v1/systems/sources          # List source systems
GET    /api/v1/schemas/targets          # List target schemas
GET    /api/v1/schemas/source/{system}  # Get source schema
GET    /api/v1/schemas/target/{schema}  # Get target schema

POST   /api/v1/jexl/validate            # Validate JEXL expression
```

### Request/Response Examples

#### Create Blueprint

```http
POST /api/v1/blueprints
Content-Type: application/json

{
  "name": "Lloyd's Quote Mapper",
  "sourceSystem": "lloyds-api",
  "targetSchema": "beema-policy-v1",
  "mappings": [],
  "status": "draft"
}
```

#### Test Blueprint

```http
POST /api/v1/blueprints/{id}/test
Content-Type: application/json

{
  "policyNumber": "POL-12345",
  "premium": 1500.00,
  "insured": {
    "firstName": "John",
    "lastName": "Doe"
  }
}
```

## Development

### Project Structure

```
apps/studio/
├── src/
│   ├── components/
│   │   ├── BlueprintEditor/
│   │   │   ├── BlueprintCanvas.tsx    # Main canvas component
│   │   │   ├── SourceFieldList.tsx    # Draggable source fields
│   │   │   ├── TargetFieldList.tsx    # Droppable target fields
│   │   │   ├── MappingLine.tsx        # Visual connection lines
│   │   │   └── JexlExpressionEditor.tsx # Monaco editor
│   │   ├── Layout/
│   │   │   ├── Header.tsx             # Top navigation bar
│   │   │   └── Sidebar.tsx            # Blueprint list sidebar
│   │   └── ui/                        # Reusable UI components
│   ├── hooks/
│   │   ├── useBlueprintQuery.ts       # React Query hooks
│   │   └── useDragAndDrop.ts          # Drag & drop logic
│   ├── services/
│   │   └── api.ts                     # API client
│   ├── stores/
│   │   └── blueprintStore.ts          # Zustand store
│   ├── types/
│   │   ├── blueprint.ts               # Blueprint types
│   │   └── mapping.ts                 # Mapping types
│   ├── App.tsx                        # Root component
│   ├── main.tsx                       # Entry point
│   └── index.css                      # Global styles
├── public/                            # Static assets
├── Dockerfile                         # Production container
├── nginx.conf                         # Nginx configuration
├── package.json                       # Dependencies
├── vite.config.ts                     # Vite configuration
├── tailwind.config.js                 # Tailwind configuration
└── tsconfig.json                      # TypeScript configuration
```

### Scripts

```bash
# Development
pnpm dev          # Start dev server with HMR

# Building
pnpm build        # Build for production
pnpm preview      # Preview production build locally

# Code Quality
pnpm lint         # Run ESLint
pnpm format       # Format with Prettier
pnpm test         # Run tests with Vitest

# Cleanup
pnpm clean        # Remove build artifacts
```

### Adding New Features

1. **New Mapping Type**: Add to `FieldMapping.mappingType` enum in `types/blueprint.ts`
2. **New UI Component**: Create in `components/ui/` following existing patterns
3. **New API Endpoint**: Add method to `services/api.ts` and create React Query hook

## Docker Deployment

### Build Image

```bash
docker build -t beema/studio:latest .
```

### Run Container

```bash
docker run -d \
  --name beema-studio \
  -p 3000:80 \
  -e VITE_API_BASE_URL=http://beema-kernel:8080/api/v1 \
  beema/studio:latest
```

### Docker Compose

Add to `docker-compose.yml`:

```yaml
studio:
  build:
    context: ./apps/studio
    dockerfile: Dockerfile
  ports:
    - "3000:80"
  environment:
    - VITE_API_BASE_URL=http://beema-kernel:8080/api/v1
  depends_on:
    - beema-kernel
  networks:
    - beema-network
```

## Kubernetes Deployment

### Helm Chart Structure

```
helm/studio/
├── Chart.yaml
├── values.yaml
└── templates/
    ├── deployment.yaml
    ├── service.yaml
    ├── ingress.yaml
    └── configmap.yaml
```

### Deploy with Helm

```bash
helm install beema-studio ./helm/studio \
  --set image.tag=latest \
  --set ingress.host=studio.beema.io \
  --set api.baseUrl=http://beema-kernel:8080/api/v1
```

## Troubleshooting

### Common Issues

**Issue**: "Cannot connect to API"
- Check that `VITE_API_BASE_URL` is set correctly
- Verify beema-kernel is running
- Check CORS configuration in beema-kernel

**Issue**: "JEXL validation failing"
- Ensure expression syntax is correct
- Check that all referenced fields exist in source schema
- Verify data types match

**Issue**: "Drag and drop not working"
- Clear browser cache
- Check console for JavaScript errors
- Ensure @dnd-kit is properly installed

**Issue**: "Monaco editor not loading"
- Check network tab for CDN failures
- Verify @monaco-editor/react is installed
- Try clearing node_modules and reinstalling

## Performance Optimization

- **Code Splitting**: Vendor, DND, and Editor chunks are separated
- **Lazy Loading**: Monaco editor loads on-demand
- **Memoization**: React.memo and useMemo used for expensive renders
- **Virtual Scrolling**: Considered for large field lists (100+ fields)

## Security Considerations

- All API calls include authentication tokens
- CORS configured in nginx
- XSS protection headers enabled
- Input sanitization for JEXL expressions
- No eval() or Function() used in expression parsing

## Browser Support

- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

## Contributing

1. Create feature branch from `main`
2. Follow existing code style and patterns
3. Add tests for new functionality
4. Update documentation
5. Submit pull request

## License

Copyright (c) 2024 Beema Platform. All rights reserved.

## Support

- Documentation: https://docs.beema.io/studio
- Issues: https://github.com/prabhatkmr/beema/issues
- Slack: #beema-studio
