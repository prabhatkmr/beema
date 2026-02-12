# AI/LLM Integration Summary

This document summarizes the AI/LLM capabilities integrated into beema-kernel for intelligent claim analysis.

## Implementation Overview

All tasks from the integration specification have been completed successfully.

## Files Created

### 1. AI Tools Layer
- **`src/main/java/com/beema/kernel/ai/tools/InsuranceTools.java`**
  - 5 AI-callable functions for LLM tool calling
  - Exposes MetadataService and JexlExpressionEngine to AI
  - Tools: getFieldMetadata, evaluateExpression, validateSchema, getValidationRules, calculateClaimMetrics

### 2. Domain Models
- **`src/main/java/com/beema/kernel/domain/claim/Claim.java`**
  - Claim domain model with status enum
  - Support for bitemporal data and JSONB attributes

### 3. AI Services
- **`src/main/java/com/beema/kernel/ai/service/ClaimAnalyzerService.java`**
  - Spring AI-powered claim analysis
  - Prompt engineering for insurance claims
  - Action recommendations: APPROVE_IMMEDIATELY, REQUEST_DOCUMENTS, ESCALATE_TO_SPECIALIST, REFER_TO_INVESTIGATOR, REJECT, MANUAL_REVIEW
  - Fallback strategy for AI failures

### 4. Temporal Workflow Integration
- **`src/main/java/com/beema/kernel/workflow/claim/AgentActivities.java`**
  - Temporal activity interface for AI analysis
- **`src/main/java/com/beema/kernel/workflow/claim/AgentActivitiesImpl.java`**
  - Activity implementation with heartbeat monitoring
- **`src/main/java/com/beema/kernel/workflow/claim/ClaimWorkflow.java`**
  - Workflow interface for durable claim processing
- **`src/main/java/com/beema/kernel/workflow/claim/ClaimWorkflowImpl.java`**
  - Workflow implementation with retry policies

### 5. REST API
- **`src/main/java/com/beema/kernel/api/v1/claim/ClaimAnalysisController.java`**
  - `/api/v1/claims/analysis/analyze` - Synchronous AI analysis
  - `/api/v1/claims/analysis/process-with-workflow` - Asynchronous Temporal workflow
  - `/api/v1/claims/analysis/workflow/{workflowId}` - Get workflow results

### 6. Tests
- **`src/test/java/com/beema/kernel/ai/service/ClaimAnalyzerServiceTest.java`**
  - Unit tests for ClaimAnalyzerService
  - Test property configuration for isolated testing

### 7. Documentation
- **`AI_AGENT_GUIDE.md`**
  - Comprehensive guide for AI agent integration
  - Tool descriptions, usage examples, configuration
  - Troubleshooting and monitoring

## Files Modified

### 1. Dependencies
- **`pom.xml`**
  - Added Spring AI OpenAI starter (v1.0.0-M4)
  - Added Spring Milestones repository

### 2. Configuration
- **`src/main/resources/application.yml`**
  - Spring AI OpenAI configuration
  - Beema AI feature flags
  - Model settings (GPT-4 Turbo, temperature 0.3)

### 3. Temporal Configuration
- **`src/main/java/com/beema/kernel/config/TemporalConfig.java`**
  - Added CLAIM_TASK_QUEUE constant
  - Registered ClaimWorkflow and AgentActivities
  - Created claimWorker bean

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    REST Controller                          │
│           ClaimAnalysisController                           │
│  /analyze (sync)  |  /process-with-workflow (async)        │
└───────────────────┬─────────────────────────────────────────┘
                    │
        ┌───────────┴──────────┐
        │                      │
        ▼                      ▼
┌───────────────┐    ┌──────────────────┐
│ClaimAnalyzer  │    │ ClaimWorkflow    │
│   Service     │    │  (Temporal)      │
└───────┬───────┘    └────────┬─────────┘
        │                     │
        │              ┌──────▼──────────┐
        │              │ AgentActivities │
        │              │     (Impl)      │
        │              └──────┬──────────┘
        │                     │
        └─────────────────────┘
                    │
        ┌───────────┴────────────┐
        │                        │
        ▼                        ▼
┌──────────────┐        ┌────────────────┐
│ Spring AI    │        │ InsuranceTools │
│ ChatClient   │───────>│  (5 Tools)     │
│ + GPT-4      │        └────────┬───────┘
└──────────────┘                 │
                        ┌────────┴──────────┐
                        │                   │
                        ▼                   ▼
                ┌───────────────┐   ┌──────────────┐
                │ MetadataService│   │ JexlExpression│
                │               │   │    Engine     │
                └───────────────┘   └──────────────┘
```

## Key Features

### 1. Function Calling / Tool Use
The LLM has access to 5 specialized insurance tools:
- **getFieldMetadata**: Retrieves schema and validation rules
- **evaluateExpression**: Executes JEXL business rules
- **validateSchema**: Validates claim data
- **getValidationRules**: Gets validation rules for claim type
- **calculateClaimMetrics**: Computes claim ratios and severity

### 2. Intelligent Claim Routing
AI analyzes claims and recommends:
- Immediate approval for low-risk claims
- Document requests for incomplete claims
- Specialist escalation for complex cases
- Investigator referral for fraud indicators
- Rejection for policy violations

### 3. Temporal Workflow Integration
- Durable execution with retry policies
- Activity heartbeat monitoring
- Asynchronous processing for long-running analysis
- Workflow state persistence

### 4. Fallback Strategy
If AI fails:
- Returns MANUAL_REVIEW action
- Logs error details
- Ensures no claims are lost
- Confidence score set to 0.0

### 5. Security
- JEXL expression sandboxing (JexlPermissions.RESTRICTED)
- API key protection via environment variables
- Input validation on all claim data
- Audit trail with workflow IDs

## Configuration

### Required Environment Variables
```bash
export OPENAI_API_KEY=sk-your-actual-api-key
```

### Optional Configuration
```bash
export AI_ENABLED=true                # Enable/disable AI features
export TEMPORAL_WORKER_ENABLED=true   # Enable/disable Temporal workers
```

## Quick Start

### 1. Build the Project
```bash
cd /Users/prabhatkumar/Desktop/dev-directory/beema/apps/beema-kernel
mvn clean install
```

### 2. Set OpenAI API Key
```bash
export OPENAI_API_KEY=sk-your-key-here
```

### 3. Start Temporal Server (if not running)
```bash
temporal server start-dev
```

### 4. Run the Application
```bash
mvn spring-boot:run
```

### 5. Test AI Claim Analysis
```bash
curl -X POST http://localhost:8080/api/v1/claims/analysis/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "claimId": "550e8400-e29b-41d4-a716-446655440000",
    "claimNumber": "CLM-2026-001",
    "claimType": "motor_accident",
    "claimAmount": 5000.0,
    "policyNumber": "POL-2026-001",
    "marketContext": "RETAIL",
    "status": "REPORTED",
    "description": "Minor collision, rear bumper damage"
  }'
```

## Testing

### Run Unit Tests
```bash
mvn test -Dtest=ClaimAnalyzerServiceTest
```

Note: Tests are configured to not require actual OpenAI API key (AI disabled in test profile).

## Monitoring

### Check Application Logs
```bash
tail -f logs/beema-kernel.log | grep ClaimAnalyzer
```

### Temporal UI
```
http://localhost:8233
```
View workflow executions with workflowId: `claim-ai-{claimNumber}`

## Next Steps

### Recommended Enhancements
1. **Vector Database**: Integrate Pinecone/Weaviate for RAG on historical claims
2. **Fine-tuning**: Train GPT-4 on insurance-specific claims data
3. **Multi-Agent**: Add specialist agents for fraud detection, underwriting
4. **Real-time Learning**: Feedback loop from claim adjusters

### Production Checklist
- [ ] Set up API key rotation for OpenAI
- [ ] Configure rate limiting for AI calls
- [ ] Set up monitoring alerts for AI failures
- [ ] Implement cost tracking for OpenAI API usage
- [ ] Add unit tests with mocked AI responses
- [ ] Load test Temporal workflow capacity
- [ ] Document runbook for AI service outages

## Dependencies Added

```xml
<!-- Spring AI OpenAI Starter -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
    <version>1.0.0-M4</version>
</dependency>
```

Repository:
```xml
<repository>
    <id>spring-milestones</id>
    <name>Spring Milestones</name>
    <url>https://repo.spring.io/milestone</url>
</repository>
```

## Deliverables Checklist

- [x] Task 1: Add Spring AI Dependencies (pom.xml, application.yml)
- [x] Task 2: Create InsuranceTools Class (5 tools)
- [x] Task 3: Create Claim Domain Model
- [x] Task 4: Create AI Claim Analyzer Service
- [x] Task 5: Create Temporal Activity
- [x] Task 6: Create Claim Workflow
- [x] Task 7: Register Worker in Temporal Config
- [x] Task 8: Create REST Controller (3 endpoints)
- [x] Task 9: Create Tests
- [x] Task 10: Documentation (AI_AGENT_GUIDE.md)

## Support

For questions or issues:
- Review `AI_AGENT_GUIDE.md` for detailed documentation
- Check application logs for error details
- Verify OpenAI API key is set correctly
- Ensure Temporal server is running

## License

Part of the Beema Unified Insurance Platform. All rights reserved.
