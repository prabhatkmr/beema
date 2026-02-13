# Killer Features

## Feature #1: Virtual Fields (The Formula Engine)

### The Problem

Users need derived data (e.g., Underwriting Score, Days Until Renewal) without asking developers to write code.

**The Guidewire Limitation:** Requires Gosu code, redeployment, and database column additions.

### Architecture Strategy

We will support two types of Virtual Fields:

1. **Dynamic (Runtime)**: Calculated on the fly when the record is read. Good for time-sensitive data (e.g., "Current Age").

2. **Materialized (Stored)**: Calculated on save and persisted in the JSONB. Good for searching/indexing (e.g., "Total Insured Value").

We will use **JEXL** (Java Expression Language) or **Spring SpEL** for safe, sandboxed execution.

### Database Schema

```sql
ALTER TABLE sys_fields ADD COLUMN is_virtual BOOLEAN DEFAULT FALSE;
ALTER TABLE sys_fields ADD COLUMN is_materialized BOOLEAN DEFAULT FALSE; -- If TRUE, save result to DB
ALTER TABLE sys_fields ADD COLUMN formula_expression TEXT;
-- Example: "risk.building_limit + risk.content_limit"
```

### Java Implementation (Formula Service)

**FormulaService.java**

```java
@Service
public class FormulaService {

    private final JexlEngine jexl = new JexlBuilder().cache(512).strict(true).create();

    public Map<String, Object> calculateVirtualFields(Map<String, Object> recordData, List<SysField> fields) {
        JexlContext context = new MapContext();
        context.set("data", recordData);
        // Add helper functions (e.g., Date math)
        context.set("dates", new DateUtils());

        for (SysField field : fields) {
            if (field.isVirtual() && !field.isMaterialized()) {
                try {
                    JexlExpression e = jexl.createExpression(field.getFormulaExpression());
                    Object result = e.evaluate(context);
                    recordData.put(field.getApiName(), result);
                } catch (Exception ex) {
                    recordData.put(field.getApiName(), null); // Fail gracefully
                }
            }
        }
        return recordData;
    }
}
```

---

## Feature #2: Metadata Webhooks (The Integration Engine)

### The Problem

Integrating with 3rd parties (General Ledger, Fraud Check, SMS Gateway) usually requires custom plugin code.

**The Guidewire Limitation:** "Messaging Plugins" are heavy, code-intensive, and require server restarts.

### Architecture Strategy

We treat integrations as **Data Subscription**:

- **Trigger**: Transaction Commit (Save)
- **Filter**: Rules engine (e.g., "Only if Premium > $10k")
- **Transform**: JSON Template (Mustache/Handlebars) to map internal data to external format
- **Transport**: Async HTTP POST

### Database Schema

```sql
CREATE TABLE sys_webhooks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID REFERENCES sys_tenants(id),
    name TEXT NOT NULL,

    -- Trigger Logic
    object_api_name VARCHAR(64) NOT NULL, -- e.g. "claim_c"
    events VARCHAR[] NOT NULL, -- ['CREATE', 'UPDATE']
    condition_script TEXT, -- "data.total_incurred > 10000"

    -- Destination
    target_url TEXT NOT NULL,
    auth_header TEXT, -- "Bearer sk_live_..." (Encrypted)

    -- Data Mapper (The "Integration" Logic)
    payload_template TEXT
    -- '{"vendor_claim_id": "{{claim_number}}", "loss_amount": {{reserve_amount}} }'
);

CREATE TABLE sys_webhook_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    webhook_id UUID REFERENCES sys_webhooks(id),
    request_payload JSONB,
    response_code INT,
    response_body TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);
```

### Java Implementation (Event Listener)

We use Spring's `ApplicationEventPublisher` to decouple the API from the Webhook engine.

**WebhookListener.java**

```java
@Component
public class WebhookListener {

    @Async // Run in background thread so UI isn't blocked
    @EventListener
    public void handleEntitySave(EntitySavedEvent event) {
        // 1. Find Webhooks for this Object
        List<SysWebhook> hooks = webhookRepo.findByObject(event.getObjectName());

        for (SysWebhook hook : hooks) {
            // 2. Check Condition (JEXL)
            if (formulaService.evaluate(hook.getConditionScript(), event.getData())) {

                // 3. Transform Payload (Mustache)
                String payload = mustacheService.compile(hook.getPayloadTemplate(), event.getData());

                // 4. Send Request
                try {
                    HttpResponse<String> response = httpClient.post(hook.getTargetUrl(), payload);
                    logService.saveSuccess(hook, payload, response);
                } catch (Exception e) {
                    logService.saveError(hook, payload, e);
                }
            }
        }
    }
}
```

---

## Feature #3: Layouts as Data (Server-Driven UI)

### The Problem

Different users (Underwriters vs. Agents) need different views of the same data.

**The Guidewire Limitation:** PCF files are static code. You cannot dynamically change the screen layout based on data (e.g., "If State=CA, show Earthquake Tab") without complex conditional logic in the code.

### Architecture Strategy

We decouple the **Data** (JSON) from the **Presentation** (Layout JSON). The Frontend becomes a **"Dumb Renderer."** It just draws what the backend tells it to.

### Database Schema

```sql
CREATE TABLE sys_layouts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    object_id UUID REFERENCES sys_objects(id),

    -- Context (Who sees this?)
    product_id UUID,   -- Specific to "Cyber Policy"
    role_id UUID,      -- Specific to "Claims Adjuster"

    -- The Blueprint
    structure JSONB NOT NULL
    -- {
    --   "tabs": [
    --     { "label": "General", "sections": [...] },
    --     { "label": "Financials", "sections": [...] }
    --   ]
    -- }
);
```

### Java Implementation (Context Resolver)

**LayoutService.java**

```java
public LayoutDTO getLayoutForContext(UUID objectId, UUID productId, UUID userRoleId) {
    // 1. Try to find Specific Match (Product + Role)
    Optional<SysLayout> specific = layoutRepo.findByObjectProductAndRole(objectId, productId, userRoleId);
    if (specific.isPresent()) return toDTO(specific.get());

    // 2. Fallback to Product Default
    Optional<SysLayout> productDefault = layoutRepo.findByObjectAndProduct(objectId, productId);
    if (productDefault.isPresent()) return toDTO(productDefault.get());

    // 3. Fallback to System Default
    return toDTO(layoutRepo.findDefault(objectId));
}
```

---

## Feature #4: Operating System Model (The App Platform)

### The Concept

In this model, your SaaS is not one giant application. It is an **Operating System (OS)** that hosts multiple "Apps."

- **Level 1 (The Springboard)**: The home screen. Icons for "Policy Manager," "Claims Adjuster," "Billing Center."

- **Level 2 (The App Instance)**: When you click "Policy Manager," you aren't just seeing a table. You launch the **Policy App**.

- **Level 3 (The Record as a Sub-App)**: When you open Policy #123, it opens as its own workspace (a "Sub-App"). It has its own navigation (Coverages, Drivers, History) that is completely distinct from the Claim #999 you also have open.

### Why This Kills Guidewire

**Guidewire** uses a global "tab bar." If you are in a Claim and click "Search," you lose your place.

In an **OS Model**, "Search" is a system-level utility, and "Claim #123" is a running process. You can multitask between them just like switching apps on an iPhone.

### Architecture: Defining the "App" Metadata

We need to define what an "App" is in the database.

#### The Metadata Schema (sys_apps)

```sql
-- ==========================================
-- APPLICATION DEFINITIONS (The "App Store")
-- ==========================================
CREATE TABLE sys_apps (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID REFERENCES sys_tenants(id),

    -- Identity
    api_name VARCHAR(64) NOT NULL, -- "policy_center_app"
    display_name TEXT NOT NULL,    -- "Policy Manager"
    icon VARCHAR(50),              -- "briefcase"
    color VARCHAR(20),             -- "#007AFF" (iOS Blue)

    -- Security (Who can see this app?)
    required_role VARCHAR(50),     -- "UNDERWRITER"

    -- Navigation Configuration (The "Menu")
    -- Defines the sidebar/bottom-bar for this specific app.
    navigation_config JSONB NOT NULL
    -- {
    --   "items": [
    --     { "label": "Dashboard", "view": "dashboard_v1" },
    --     { "label": "My Queues", "view": "list_my_policies" },
    --     { "label": "Search", "view": "global_search" }
    --   ]
    -- }
);

-- ==========================================
-- RECORD NAVIGATION (The "Sub-App" Menu)
-- When you open a specific record (e.g. Policy #123), what menu do you see?
-- ==========================================
CREATE TABLE sys_record_navigation (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    object_id UUID REFERENCES sys_objects(id),

    -- Context (Which App are we in?)
    -- You might see different tabs for a Policy if you are in "Billing App" vs "Policy App"
    app_id UUID REFERENCES sys_apps(id),

    -- The Menu Structure
    menu_structure JSONB NOT NULL
    -- {
    --   "items": [
    --     { "label": "Overview", "component": "SummaryView" },
    --     { "label": "Coverages", "component": "RelatedList", "target": "coverage_c" },
    --     { "label": "Drivers", "component": "RelatedList", "target": "driver_c" }
    --   ]
    -- }
);
```

### Java / Spring Boot Implementation

The backend needs to serve the **"Springboard"** (List of Apps) and the **"App Context"** (What happens when I launch one).

**AppService.java**

```java
@Service
@RequiredArgsConstructor
public class AppService {

    private final AppRepository appRepo;
    private final RecordNavigationRepository navRepo;

    /**
     * THE SPRINGBOARD (Home Screen)
     * Returns the list of apps the current user is allowed to see.
     */
    public List<AppDTO> getSpringboard(UUID tenantId, UserContext user) {
        return appRepo.findByTenantId(tenantId).stream()
            .filter(app -> user.hasRole(app.getRequiredRole()))
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * THE APP CONTEXT
     * "I just clicked 'Policy #123'. What is my menu?"
     */
    public RecordMenuDTO getRecordMenu(UUID objectId, UUID appId) {
        // 1. Find the specific navigation for this Object within this App
        Optional<SysRecordNavigation> nav = navRepo.findByObjectAndApp(objectId, appId);

        if (nav.isPresent()) {
            return toDTO(nav.get());
        }

        // 2. Fallback: Default Object Navigation
        return toDTO(navRepo.findDefault(objectId));
    }
}
```

### The Frontend Implications ("The Stack")

To make this feel like an iPhone, your Frontend Router (React/Vue) needs to handle a **Stack Navigation model**, not just URL routing.

#### The Navigation State

```javascript
// Redux/Context State
{
  "activeApp": "policy_center_app",
  "stack": [
    { "type": "dashboard", "title": "Home" },
    { "type": "list", "entity": "policy_c", "filter": "my_open" },
    { "type": "record", "id": "pol-123", "title": "Policy #123 (Stark Industries)" } // Currently Visible
  ]
}
```

#### The UI Layout

- **Global Dock (Left/Bottom)**: System-level switching (Home, Notifications, Settings)
- **App Container**: The active App (Policy Center)
- **Breadcrumb / Back Button**: Derived from the Stack (Home > My Policies > Policy #123)

---

## Feature #5: Workflow Orchestration (The Time Engine)

### The Problem

Insurance processes are long-running state machines (e.g., "Wait for Police Report," "Manager Approval").

**The Guidewire Limitation:** Workflow engines are often proprietary, heavy, and difficult to test or version control.

### Architecture Strategy

We use **Temporal** as the state engine, but we need a **"Bridge"** for human interaction. Temporal handles the "Wait," and our Postgres DB handles the "UI Inbox."

### Database Schema

```sql
CREATE TABLE sys_tasks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID REFERENCES sys_tenants(id),
    
    -- Link to the Business Object
    object_id UUID REFERENCES sys_objects(id),
    
    -- Link to Temporal (The Bridge)
    workflow_id VARCHAR(255) NOT NULL,
    run_id VARCHAR(255) NOT NULL,
    signal_name VARCHAR(255) DEFAULT 'HumanApproval',

    -- Task Details
    assignee_role VARCHAR(50), -- "MANAGER"
    status VARCHAR(20) DEFAULT 'OPEN', -- 'OPEN', 'COMPLETED'
    outcome VARCHAR(50), -- 'APPROVED', 'REJECTED'
    
    created_at TIMESTAMP DEFAULT NOW()
);
```

### Java Implementation (The Bridge)

**TaskService.java**

```java
@Service
@RequiredArgsConstructor
public class TaskService {

    private final WorkflowClient temporalClient;
    private final TaskRepository taskRepo;

    /**
     * Called by the UI when a user clicks "Approve"
     */
    public void completeTask(UUID taskId, String outcome) {
        // 1. Update the Database (Clear the Inbox)
        SysTask task = taskRepo.findById(taskId).orElseThrow();
        task.setStatus("COMPLETED");
        task.setOutcome(outcome);
        taskRepo.save(task);

        // 2. Signal Temporal (Wake up the Workflow)
        // This bridges the gap between the synchronous UI and async Workflow
        WorkflowStub workflow = temporalClient.newWorkflowStub(
            task.getWorkflowId(), 
            Optional.of(task.getRunId())
        );
        
        workflow.signal(task.getSignalName(), outcome);
    }
}
```

---

## Final Architecture Summary (The "OS" Vision)

You have now assembled a complete, next-generation Insurance Platform architecture.

### The Foundation (Data)

- **Metadata Dictionary**: `sys_objects`, `sys_fields`
- **Hybrid Storage**: `data_rows` (JSONB) + `financial_transactions` (SQL)
- **Bitemporality**: `valid_time`, `transaction_time` for perfect history

### The Logic (Intelligence)

- **Product Patterns**: `sys_patterns` to configure products per line of business
- **Virtual Fields**: `sys_formulas` for runtime calculations
- **Webhooks**: `sys_webhooks` for "No-Code" integration
- **Workflows**: **Temporal** + `sys_tasks` for long-running orchestration

### The Experience (Presentation)

- **Dynamic Layouts**: `sys_layouts` for Role-based screens
- **App Model**: `sys_apps` for "OS-like" navigation and multitasking

---

## Next Steps

This architecture is ready to be documented into a formal Technical Specification:

1. **Step 1**: Create the SQL DDL scripts for all tables
2. **Step 2**: Scaffold the Spring Boot project with these Entities
3. **Step 3**: Build the "Builder API" (to let you define the metadata)

Would you like me to generate the Master SQL Script that creates this entire database structure in one go?
