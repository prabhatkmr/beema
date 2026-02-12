# Recent Updates - Beema Platform

## Summary of Latest Changes (Feb 2026)

### ✨ New Features

#### 1. **OpenRouter AI Integration** (Completed)
Migrated from direct OpenAI integration to OpenRouter for multi-provider LLM access.

**Benefits:**
- Access to 10+ LLM providers through single API
- Intelligent model selection based on claim complexity
- Cost optimization (95% savings: Llama vs GPT-4)
- Fallback routing for high availability

**Configuration:**
```yaml
# apps/beema-kernel/src/main/resources/application.yml
spring:
  ai:
    openai:
      base-url: https://openrouter.ai/api/v1
      api-key: ${OPENROUTER_API_KEY}
```

**Models Available:**
- `anthropic/claude-3-opus` - Highest accuracy
- `openai/gpt-4-turbo` - Balanced performance
- `meta-llama/llama-3.1-70b` - Cost-effective

**Documentation:**
- `apps/beema-kernel/OPENROUTER_MIGRATION.md`
- `apps/beema-kernel/OPENROUTER_SETUP.md`

#### 2. **Integration Gateway with Inngest** (Completed)
Complete event-driven webhook system for publishing lifecycle events.

**Architecture:**
```
Agreement Created → DomainEventPublisher → Inngest
                                           ↓
                                    webhook-dispatcher
                                           ↓
                              [User-Configured Webhooks]
```

**Key Components:**
1. **DomainEventPublisher** (beema-kernel)
   - Publishes domain events asynchronously
   - Non-blocking HTTP calls with WebClient
   - Error resilience (failures don't break business logic)

2. **webhook-dispatcher** (Inngest function)
   - Queries sys_webhooks table
   - Fans out to registered URLs
   - HMAC SHA-256 signature verification
   - Retry with exponential backoff

3. **Webhooks UI** (Studio)
   - Manage webhook registrations
   - View delivery history
   - Test webhook endpoints
   - Auto-generate secure secrets

**Database Schema:**
```sql
-- sys_webhooks: Webhook registrations
-- sys_webhook_deliveries: Delivery tracking
```

**Access:** http://localhost:3000/webhooks

**Documentation:**
- `apps/beema-kernel/EVENT_PUBLISHING_GUIDE.md`
- `apps/beema-kernel/EVENTS_README.md`

#### 3. **Server-Driven UI Engine** (Completed)
Metadata-driven layout system with role-based rendering and security trimming.

**How It Works:**
```
Client Request
   ↓
GET /api/v1/layouts/{context}/{objectType}
   ↓
LayoutResolutionService (finds best matching layout)
   ↓
LayoutSecurityService (evaluates JEXL expressions)
   ↓
JSON Layout (sections/fields with permissions applied)
   ↓
LayoutRenderer Component (React)
   ↓
Rendered UI
```

**Resolution Priority:**
1. Tenant-specific + Role-specific (highest)
2. Role-specific only
3. Tenant-specific only
4. Default layout (lowest)

**Security Trimming:**
- Server-side JEXL evaluation
- `visible_if` expressions filter sections/fields
- `editable_if` expressions set readonly state
- Expressions removed before sending to client (security)

**Example Layout:**
```json
{
  "title": "Motor Policy Form",
  "sections": [
    {
      "id": "vehicle-info",
      "visible_if": "user.role == 'underwriter' || status == 'DRAFT'",
      "fields": [
        {
          "id": "vehicle_make",
          "widget": "TEXT_INPUT",
          "editable_if": "status == 'DRAFT'"
        }
      ]
    }
  ]
}
```

**Database:** `sys_layouts` table (migration V15)

**Testing:** 13 security trimming tests passing

**Documentation:**
- `apps/beema-kernel/LAYOUT_RESOLUTION_GUIDE.md`
- `apps/beema-kernel/LAYOUT_API_QUICK_START.md`
- `SECURITY_TRIMMING_GUIDE.md`

### 🐛 Bug Fixes

#### Compilation Error in MetadataRegistryImpl
**Issue:** Type mismatch when converting JSONB Object to Map<String, Object>

**Files affected:**
- `MetadataRegistryImpl.java:500` - defaultValue assignment
- `MetadataRegistryImpl.java:527` - allowedValues parameter

**Fix:** Added explicit type casting with @SuppressWarnings
```java
@SuppressWarnings("unchecked")
private FieldDefinition toFieldDefinition(...) {
    Map<String, Object> defaultValue = (Map<String, Object>) attr.getDefaultValue();
    ...
    (Map<String, Object>) attr.getAllowedValues()
}
```

**Result:** ✅ Build SUCCESS

### 📦 Infrastructure Updates

#### Docker Compose Enhancements
Added Inngest service to complete event-driven architecture:

```yaml
inngest:
  image: inngest/inngest:v0.38.0
  ports:
    - "8288:8288"
  healthcheck:
    test: ["CMD", "curl", "-f", "http://localhost:8288/health"]
```

**New Service Ports:**
- Inngest Dashboard: http://localhost:8288

#### Database Migrations
- **V15:** Layout system tables (`sys_layouts`)
  - Stores UI layouts with JSONB schemas
  - Role/tenant-based resolution
  - Priority-based selection

### 🧪 Testing & Verification

#### New Test Scripts

1. **Layout API Test** (`apps/beema-kernel/test-layout-api.sh`)
   ```bash
   ./test-layout-api.sh
   ```
   - Tests layout resolution
   - Verifies security trimming
   - Validates role-based filtering

2. **Webhook E2E Test** (`scripts/verify-webhooks-e2e.sh`)
   ```bash
   ./scripts/verify-webhooks-e2e.sh
   ```
   - Inserts test webhook
   - Creates agreement
   - Verifies delivery to httpbin.org

3. **Platform Startup** (`startup-and-verify.sh`)
   ```bash
   ./startup-and-verify.sh
   ```
   - Starts all services in correct order
   - Waits for health checks
   - Verifies connectivity
   - Shows service URLs

### 📊 Current Statistics

- **Java Source Files:** 115 classes
- **Database Migrations:** 15 applied
- **Docker Services:** 10 configured
- **Test Coverage:** 130+ tests
- **Build Status:** ✅ SUCCESS
- **Compilation Time:** ~1.7 seconds

### 🚀 Quick Start

```bash
# 1. Start all services
./startup-and-verify.sh

# 2. Access services
# - Studio UI:         http://localhost:3000
# - API Docs:          http://localhost:8080/swagger-ui/index.html
# - Temporal:          http://localhost:8088
# - Inngest:           http://localhost:8288

# 3. Run tests
cd apps/beema-kernel
./test-layout-api.sh
./test-openrouter.sh

# 4. View logs
docker-compose logs -f beema-kernel
```

### 📝 Modified Files (Need Commit)

**Configuration Changes:**
- `apps/beema-kernel/src/main/resources/application.yml` - OpenRouter config
- `apps/beema-kernel/src/main/java/com/beema/kernel/ai/config/OpenRouterConfig.java` - Custom headers
- `docker-compose.yml` - Added Inngest service

**Bug Fixes:**
- `apps/beema-kernel/src/main/java/com/beema/kernel/service/metadata/MetadataRegistryImpl.java` - Type casting

**New Features:**
- `apps/beema-kernel/src/main/java/com/beema/kernel/api/v1/layout/*` - Layout API
- `apps/beema-kernel/src/main/java/com/beema/kernel/service/layout/*` - Layout services
- `apps/beema-kernel/src/main/java/com/beema/kernel/event/*` - Event publishing
- `apps/studio/inngest/webhook-dispatcher.ts` - Webhook fan-out
- `apps/studio/app/webhooks/*` - Webhooks UI
- `packages/ui/components/LayoutRenderer.tsx` - Generic renderer

### 🎯 Recommended Next Steps

1. **Commit Current Work**
   ```bash
   git add .
   git commit -m "feat: Add OpenRouter, Inngest webhooks, and server-driven UI"
   ```

2. **Integration Testing**
   ```bash
   docker-compose up -d postgres temporal
   mvn test -Dtest=*Integration*
   ```

3. **Load Testing**
   - Test 100+ concurrent layout requests
   - Verify cache performance
   - Measure webhook delivery latency

4. **Documentation**
   - Update main README.md
   - Add architecture diagrams
   - Document deployment process

5. **Keycloak Integration**
   - Configure OAuth2/JWT flows
   - Integrate with layout role resolution
   - Multi-tenant user management

### 📚 Documentation Index

**Platform:**
- `README.md` - Main documentation
- `STATUS_UPDATE.md` - Current status
- `RECENT_UPDATES.md` - This file

**Features:**
- `LAYOUT_RESOLUTION_GUIDE.md` - Server-driven UI
- `SECURITY_TRIMMING_GUIDE.md` - JEXL security
- `EVENT_PUBLISHING_GUIDE.md` - Inngest webhooks
- `OPENROUTER_MIGRATION.md` - AI integration

**Testing:**
- `startup-and-verify.sh` - Platform startup
- `test-layout-api.sh` - Layout API tests
- `verify-webhooks-e2e.sh` - Webhook E2E

### ❓ Support

For issues:
1. Check service logs: `docker-compose logs [service]`
2. Verify health: `curl http://localhost:8080/actuator/health`
3. Review .env.example files for configuration
4. Check database: `docker exec -it beema-postgres psql -U beema -d beema_kernel`

---

**Last Updated:** 2026-02-12
**Platform Version:** 0.1.0-SNAPSHOT
**Status:** ✅ Development Ready
