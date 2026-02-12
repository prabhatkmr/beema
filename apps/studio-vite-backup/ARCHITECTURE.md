# Beema Studio - Architecture Documentation

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         Browser (Client)                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                    React Application                       │  │
│  │                                                            │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │  │
│  │  │   Header     │  │   Sidebar    │  │   Canvas     │   │  │
│  │  │              │  │              │  │              │   │  │
│  │  │ - Save       │  │ - Blueprint  │  │ - Source     │   │  │
│  │  │ - Validate   │  │   List       │  │   Fields     │   │  │
│  │  │ - Test       │  │ - Search     │  │ - Target     │   │  │
│  │  └──────────────┘  └──────────────┘  │   Fields     │   │  │
│  │                                       │ - Mapping    │   │  │
│  │  ┌──────────────────────────────────┐│   Lines      │   │  │
│  │  │   JEXL Expression Editor         ││              │   │  │
│  │  │   (Monaco Editor)                ││              │   │  │
│  │  └──────────────────────────────────┘└──────────────┘   │  │
│  │                                                            │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │           State Management Layer                   │  │  │
│  │  │                                                     │  │  │
│  │  │  ┌──────────────┐         ┌──────────────────┐   │  │  │
│  │  │  │   Zustand    │         │  React Query     │   │  │  │
│  │  │  │              │         │                  │   │  │  │
│  │  │  │ - Blueprint  │         │ - API Calls      │   │  │  │
│  │  │  │   State      │         │ - Caching        │   │  │  │
│  │  │  │ - Mappings   │         │ - Mutations      │   │  │  │
│  │  │  │ - Canvas     │         │ - Optimistic     │   │  │  │
│  │  │  │   State      │         │   Updates        │   │  │  │
│  │  │  └──────────────┘         └──────────────────┘   │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  │                                                            │  │
│  │  ┌────────────────────────────────────────────────────┐  │  │
│  │  │              API Client (Axios)                    │  │  │
│  │  │                                                     │  │  │
│  │  │  - Request Interceptors (Auth)                     │  │  │
│  │  │  - Response Interceptors (Error Handling)          │  │  │
│  │  │  - Retry Logic                                      │  │  │
│  │  └────────────────────────────────────────────────────┘  │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
                              │
                              │ HTTPS/REST
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Nginx Reverse Proxy                         │
│                                                                   │
│  - Serve static assets (React build)                             │
│  - Proxy /api/* to beema-kernel:8080                             │
│  - Gzip compression                                               │
│  - Security headers                                               │
│  - Health check endpoint                                          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Beema Kernel API                            │
│                                                                   │
│  /api/v1/blueprints          - Blueprint CRUD                    │
│  /api/v1/blueprints/:id/test - Test transformations              │
│  /api/v1/jexl/validate       - Validate JEXL expressions         │
│  /api/v1/schemas/*           - Schema definitions                │
└─────────────────────────────────────────────────────────────────┘
```

## Component Architecture

### Component Hierarchy

```
App
├── QueryClientProvider (React Query)
│   └── AppContent
│       ├── Header
│       │   ├── SaveButton
│       │   ├── ValidateButton
│       │   ├── TestButton
│       │   └── SettingsButton
│       │
│       ├── Sidebar
│       │   ├── CreateBlueprintModal
│       │   ├── SearchInput
│       │   └── BlueprintList
│       │       └── BlueprintItem[]
│       │
│       └── BlueprintCanvas
│           ├── DndContext
│           │   ├── SourceFieldList
│           │   │   └── DraggableField[]
│           │   │
│           │   └── TargetFieldList
│           │       └── DroppableField[]
│           │
│           ├── MappingLine (Canvas)
│           │
│           └── JexlExpressionEditor (conditional)
│               └── MonacoEditor
│
└── Toaster (react-hot-toast)
```

## Data Flow Architecture

### 1. Blueprint Loading Flow

```
User Clicks Blueprint
      │
      ▼
Sidebar triggers setCurrentBlueprint
      │
      ▼
Zustand Store updates currentBlueprint
      │
      ▼
React Query fetches source/target schemas
      │
      ▼
BlueprintCanvas renders with fields
      │
      ▼
User sees draggable/droppable fields
```

### 2. Mapping Creation Flow

```
User Drags Source Field
      │
      ▼
@dnd-kit detects drag start
      │
      ▼
User Drops on Target Field
      │
      ▼
handleDrop creates FieldMapping
      │
      ▼
Store.addMapping updates state
      │
      ▼
isDirty = true (unsaved changes)
      │
      ▼
UI updates to show mapping
```

### 3. JEXL Expression Flow

```
User Selects Mapping
      │
      ▼
Store.setSelectedMapping
      │
      ▼
JexlExpressionEditor renders
      │
      ▼
User Types Expression
      │
      ▼
Monaco Editor provides syntax highlighting
      │
      ▼
User Clicks "Test"
      │
      ▼
API POST /jexl/validate
      │
      ▼
Result displayed (success/error)
      │
      ▼
If valid, Store.updateMapping
```

### 4. Save Flow

```
User Clicks "Save"
      │
      ▼
Header triggers updateMutation
      │
      ▼
React Query sends PUT /blueprints/:id
      │
      ▼
Optimistic update in cache
      │
      ▼
Server responds with updated blueprint
      │
      ▼
Store.markAsSaved() (isDirty = false)
      │
      ▼
Toast notification "Saved successfully"
```

## State Management Architecture

### Zustand Store (Client State)

```typescript
BlueprintStore {
  // Current state
  currentBlueprint: MessageBlueprint | null
  originalBlueprint: MessageBlueprint | null  // For reset
  selectedMapping: FieldMapping | null
  isDirty: boolean
  isTestPanelOpen: boolean
  canvasState: { zoom, pan }

  // Actions
  setCurrentBlueprint()
  updateBlueprint()
  addMapping()
  updateMapping()
  deleteMapping()
  setSelectedMapping()
  resetBlueprint()
  markAsSaved()
}
```

### React Query Cache (Server State)

```typescript
QueryCache {
  ['blueprints'] -> MessageBlueprint[]
  ['blueprints', id] -> MessageBlueprint
  ['sourceSystems'] -> string[]
  ['targetSchemas'] -> string[]
  ['sourceSchema', system] -> Schema
  ['targetSchema', schema] -> Schema
}
```

## API Layer Architecture

### API Client Structure

```typescript
class ApiClient {
  private client: AxiosInstance

  // Interceptors
  - requestInterceptor (adds auth token)
  - responseInterceptor (handles 401)

  // Blueprint endpoints
  getBlueprints()
  getBlueprintById(id)
  createBlueprint(blueprint)
  updateBlueprint(id, updates)
  deleteBlueprint(id)
  cloneBlueprint(id, name)

  // Validation endpoints
  validateBlueprint(id)
  validateJexlExpression(expr, context)

  // Test endpoints
  testBlueprint(id, testData)

  // Schema endpoints
  getSourceSchema(system)
  getTargetSchema(schema)
  getSourceSystems()
  getTargetSchemas()
}
```

## Drag & Drop Architecture

### @dnd-kit Flow

```
┌────────────────────────────────────────────────────────┐
│                    DndContext                          │
│                                                         │
│  ┌──────────────────────────┐  ┌──────────────────┐  │
│  │   Draggable Items        │  │  Droppable Areas │  │
│  │                          │  │                  │  │
│  │  SourceField (useDrag)   │  │  TargetField     │  │
│  │  - id                    │  │  (useDroppable)  │  │
│  │  - data: {              │  │  - id            │  │
│  │      type: 'source'     │  │  - data: {       │  │
│  │      field: 'name'      │  │      type: 'tgt' │  │
│  │      path: 'src.name'   │  │      field: ...  │  │
│  │    }                    │  │    }             │  │
│  └──────────────────────────┘  └──────────────────┘  │
│                                                         │
│  Sensors:                                              │
│  - PointerSensor (mouse/touch)                         │
│  - KeyboardSensor (a11y)                               │
│                                                         │
│  Events:                                               │
│  - onDragStart -> setActiveItem                        │
│  - onDragEnd   -> createMapping                        │
│  - onDragCancel -> cleanup                             │
└────────────────────────────────────────────────────────┘
```

## Type System Architecture

### Core Domain Types

```typescript
// Blueprint types
MessageBlueprint
  ├── id: string
  ├── name: string
  ├── sourceSystem: string
  ├── targetSchema: string
  ├── mappings: FieldMapping[]
  ├── status: 'draft' | 'active' | 'archived'
  └── timestamps

FieldMapping
  ├── id: string
  ├── sourceField?: string
  ├── targetField: string
  ├── mappingType: 'direct' | 'transform' | 'constant' | 'conditional'
  ├── jexlExpression?: string
  ├── constantValue?: any
  └── validationRules?: ValidationRule[]

// Field types
SourceField
  ├── name: string
  ├── path: string
  ├── type: string
  └── description?: string

TargetField
  ├── name: string
  ├── path: string
  ├── type: string
  ├── required: boolean
  └── description?: string
```

## Build & Deployment Architecture

### Vite Build Pipeline

```
Source Code (TypeScript + React)
      │
      ▼
TypeScript Compiler (tsc)
      │
      ▼
Vite Build
  ├── Code Splitting
  │   ├── vendor.js (React, React-DOM)
  │   ├── dnd.js (@dnd-kit)
  │   └── editor.js (Monaco)
  ├── Tree Shaking
  ├── Minification
  └── CSS Processing (Tailwind)
      │
      ▼
dist/
  ├── index.html
  ├── assets/
  │   ├── index-[hash].js
  │   ├── vendor-[hash].js
  │   ├── dnd-[hash].js
  │   ├── editor-[hash].js
  │   └── index-[hash].css
  └── ...
```

### Docker Build Architecture

```
┌─────────────────────────────────────────────────┐
│           Stage 1: Builder                      │
│                                                  │
│  FROM node:20-alpine                            │
│  WORKDIR /app                                   │
│  COPY package*.json ./                          │
│  RUN pnpm install                               │
│  COPY . .                                       │
│  RUN pnpm build                                 │
│                                                  │
│  Output: /app/dist/                            │
└─────────────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────┐
│           Stage 2: Production                   │
│                                                  │
│  FROM nginx:1.25-alpine                         │
│  COPY --from=builder /app/dist /usr/share/nginx/│
│  COPY nginx.conf /etc/nginx/conf.d/default.conf│
│  EXPOSE 80                                      │
│                                                  │
│  Final Image Size: ~50MB                        │
└─────────────────────────────────────────────────┘
```

### Kubernetes Deployment Architecture

```
┌──────────────────────────────────────────────────────┐
│                   Ingress                            │
│  studio.beema.io -> TLS termination                  │
└──────────────────────────────────────────────────────┘
                    │
                    ▼
┌──────────────────────────────────────────────────────┐
│                   Service                            │
│  Type: ClusterIP                                     │
│  Port: 80                                            │
└──────────────────────────────────────────────────────┘
                    │
                    ▼
┌──────────────────────────────────────────────────────┐
│              HPA (Autoscaler)                        │
│  Min: 2, Max: 10                                     │
│  CPU: 80%, Memory: 80%                               │
└──────────────────────────────────────────────────────┘
                    │
                    ▼
┌──────────────────────────────────────────────────────┐
│                 Deployment                           │
│                                                       │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐             │
│  │  Pod 1  │  │  Pod 2  │  │  Pod N  │             │
│  │         │  │         │  │         │             │
│  │ Studio  │  │ Studio  │  │ Studio  │             │
│  │ + Nginx │  │ + Nginx │  │ + Nginx │             │
│  │         │  │         │  │         │             │
│  │ CPU:    │  │ CPU:    │  │ CPU:    │             │
│  │ 100m-   │  │ 100m-   │  │ 100m-   │             │
│  │ 500m    │  │ 500m    │  │ 500m    │             │
│  │         │  │         │  │         │             │
│  │ Mem:    │  │ Mem:    │  │ Mem:    │             │
│  │ 128Mi-  │  │ 128Mi-  │  │ 128Mi-  │             │
│  │ 512Mi   │  │ 512Mi   │  │ 512Mi   │             │
│  └─────────┘  └─────────┘  └─────────┘             │
│                                                       │
│  Anti-affinity: Spread across nodes                  │
│  Health checks: /health endpoint                     │
└──────────────────────────────────────────────────────┘
```

## Security Architecture

### Request Flow with Security

```
Browser
  │
  │ 1. User authenticates
  ▼
Keycloak (OAuth2/OIDC)
  │
  │ 2. Issues JWT token
  ▼
Browser stores token
  │
  │ 3. API request with Bearer token
  ▼
Nginx
  │
  │ 4. Security headers added
  │    - X-Frame-Options: SAMEORIGIN
  │    - X-Content-Type-Options: nosniff
  │    - X-XSS-Protection: 1; mode=block
  ▼
Beema Kernel
  │
  │ 5. Validates JWT
  │ 6. Checks permissions
  │ 7. Processes request
  ▼
Response with data
```

### Container Security

```
Container Security Features:
├── Read-only root filesystem
├── Non-root user (UID 101)
├── Dropped capabilities (ALL)
├── No privilege escalation
├── Security scanning (Trivy)
└── Minimal base image (Alpine)
```

## Performance Architecture

### Optimization Strategies

```
Frontend Optimizations
├── Code Splitting
│   ├── Route-based (future)
│   ├── Component-based
│   └── Library-based
│
├── Lazy Loading
│   ├── Monaco Editor
│   ├── Heavy components
│   └── Images
│
├── Memoization
│   ├── React.memo (components)
│   ├── useMemo (expensive calculations)
│   └── useCallback (event handlers)
│
├── Caching
│   ├── React Query (5min stale time)
│   ├── Service Worker (future)
│   └── CDN caching
│
└── Bundle Optimization
    ├── Tree shaking
    ├── Minification
    ├── Gzip compression
    └── Asset hashing
```

## Monitoring Architecture

### Observability Stack (Planned)

```
┌─────────────────────────────────────────────────────┐
│                  Application                         │
│  - Console logs (dev)                                │
│  - Structured logs (prod)                            │
│  - Error boundaries                                  │
└─────────────────────────────────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────────────┐
│              Log Aggregation                         │
│  - ELK Stack / Loki                                  │
│  - Log levels: ERROR, WARN, INFO, DEBUG              │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│              Error Tracking                          │
│  - Sentry / Rollbar                                  │
│  - Source maps                                       │
│  - User context                                      │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│              Metrics                                 │
│  - Prometheus                                        │
│    - Request count                                   │
│    - Response time                                   │
│    - Error rate                                      │
│  - Grafana dashboards                                │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│              Tracing                                 │
│  - OpenTelemetry                                     │
│  - Jaeger / Zipkin                                   │
│  - Distributed tracing                               │
└─────────────────────────────────────────────────────┘
```

## Scalability Considerations

### Horizontal Scaling

- HPA scales pods based on CPU/Memory
- Anti-affinity spreads pods across nodes
- Load balanced by K8s Service
- Stateless architecture (no local state)

### Vertical Scaling

- Resource requests: 100m CPU, 128Mi RAM
- Resource limits: 500m CPU, 512Mi RAM
- Can be adjusted based on load

### Database Considerations

- Blueprints stored in PostgreSQL (beema-kernel)
- No direct DB access from Studio
- All data via REST API
- Caching reduces DB load

## Disaster Recovery

### Backup Strategy

```
Application State:
  - No local state (stateless)
  - All data in beema-kernel DB

Configuration:
  - Helm values.yaml in git
  - ConfigMaps in K8s

Deployment:
  - Infrastructure as Code (Helm)
  - Git-based version control
  - Can redeploy from scratch
```

### High Availability

```
Component         | Replicas | Failover
------------------|----------|----------
Studio Pods       | 2-10     | Automatic (K8s)
Nginx             | 2+       | Load balanced
Beema Kernel      | 3+       | Load balanced
PostgreSQL        | 1        | Replication (planned)
```

## Development vs Production

### Development Environment

```
Development:
  - Vite dev server
  - Hot module replacement
  - Source maps
  - React Dev Tools
  - Verbose logging
  - Mock data available
```

### Production Environment

```
Production:
  - Static build (optimized)
  - Nginx serving
  - Minified code
  - No source maps
  - Error tracking
  - Performance monitoring
  - CDN (optional)
```

---

**Last Updated**: February 2024
**Version**: 0.1.0
**Maintained by**: Beema Team
