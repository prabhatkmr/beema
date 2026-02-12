# AI Claim Analysis - Quick Start Guide

## What's New: OpenRouter Integration

Beema Kernel now uses **OpenRouter** instead of OpenAI directly, giving you access to multiple AI providers through a single API:
- **OpenAI** (GPT-4, GPT-4 Turbo, GPT-4o)
- **Anthropic** (Claude 3 Opus, Sonnet, Haiku)
- **Google** (Gemini Pro 1.5)
- **Meta** (Llama 3.1)
- And many more...

**Benefits:**
- Switch between providers without code changes
- Cost optimization with model auto-selection
- Better availability and fallback options
- Single API key for all providers

## Setup (5 minutes)

### 1. Install Dependencies
```bash
cd /Users/prabhatkumar/Desktop/dev-directory/beema/apps/beema-kernel
mvn clean install -DskipTests
```

### 2. Get OpenRouter API Key

Visit [https://openrouter.ai/](https://openrouter.ai/) and create an API key.

### 3. Configure Environment

```bash
export OPENROUTER_API_KEY=sk-or-v1-your-key-here
export OPENROUTER_MODEL=openai/gpt-4-turbo-preview
```

**Alternative Models:**
```bash
# Best accuracy
export OPENROUTER_MODEL=anthropic/claude-3-opus-20240229

# Best balance
export OPENROUTER_MODEL=anthropic/claude-3-sonnet-20240229

# Best cost
export OPENROUTER_MODEL=meta-llama/llama-3.1-70b-instruct
```

### 4. Start Temporal Server (Optional - for async workflows)
```bash
temporal server start-dev
```

Or use Docker:
```bash
docker run -p 7233:7233 -p 8233:8233 temporalio/auto-setup:latest
```

### 5. Run Beema Kernel
```bash
mvn spring-boot:run
```

Application starts on http://localhost:8080

## Test AI Claim Analysis (2 minutes)

### Option 1: Synchronous Analysis (Fastest)

```bash
curl -X POST http://localhost:8080/api/v1/claims/analysis/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "claimId": "550e8400-e29b-41d4-a716-446655440000",
    "claimNumber": "CLM-001",
    "claimType": "motor_accident",
    "claimAmount": 5000.0,
    "policyNumber": "POL-001",
    "marketContext": "RETAIL",
    "status": "REPORTED",
    "description": "Minor rear-end collision, bumper damage"
  }'
```

**Expected Response:**
```json
{
  "claimId": "550e8400-e29b-41d4-a716-446655440000",
  "nextAction": "APPROVE_IMMEDIATELY",
  "confidence": 0.9,
  "reasoning": "Low-value motor claim with clear description...",
  "aiAnalysis": "..."
}
```

### Option 2: Temporal Workflow (Async, Durable)

**Start Workflow:**
```bash
curl -X POST http://localhost:8080/api/v1/claims/analysis/process-with-workflow \
  -H "Content-Type: application/json" \
  -d '{
    "claimId": "550e8400-e29b-41d4-a716-446655440000",
    "claimNumber": "CLM-002",
    "claimType": "property_damage",
    "claimAmount": 150000.0,
    "policyNumber": "POL-002",
    "marketContext": "COMMERCIAL",
    "status": "REPORTED"
  }'
```

**Response:**
```json
{
  "workflowId": "claim-ai-CLM-002",
  "status": "STARTED"
}
```

**Check Status:**
```bash
curl http://localhost:8080/api/v1/claims/analysis/workflow/claim-ai-CLM-002
```

## AI Recommendations

The AI will recommend one of these actions:

| Action | When | Confidence |
|--------|------|-----------|
| `APPROVE_IMMEDIATELY` | Low-value, straightforward | > 0.85 |
| `REQUEST_DOCUMENTS` | Missing info | > 0.8 |
| `ESCALATE_TO_SPECIALIST` | High-value/complex | > 0.75 |
| `REFER_TO_INVESTIGATOR` | Fraud indicators | > 0.7 |
| `REJECT` | Policy violation | > 0.85 |
| `MANUAL_REVIEW` | Uncertain/Error | < 0.6 |

## Test Different Claim Scenarios

### Low-Value Auto Claim (Should approve)
```json
{
  "claimAmount": 2500.0,
  "claimType": "motor_accident",
  "description": "Minor parking lot scratch"
}
```

### High-Value Commercial Claim (Should escalate)
```json
{
  "claimAmount": 500000.0,
  "claimType": "property_damage",
  "marketContext": "COMMERCIAL",
  "description": "Fire damage to warehouse"
}
```

### Suspicious Claim (Should investigate)
```json
{
  "claimAmount": 25000.0,
  "claimType": "motor_accident",
  "description": "Total loss claimed 1 day after policy inception"
}
```

## View Logs

```bash
tail -f logs/beema-kernel.log | grep "ClaimAnalyzer"
```

Expected log output:
```
INFO  c.b.k.a.s.ClaimAnalyzerService - Analyzing claim: CLM-001
INFO  c.b.k.w.c.AgentActivitiesImpl - AI Analysis Complete - Next Action: APPROVE_IMMEDIATELY, Confidence: 0.9
```

## Temporal UI (Workflow Dashboard)

Open http://localhost:8233

Search for workflow ID: `claim-ai-CLM-002`

## Troubleshooting

### Issue: "API key not found"
**Fix:**
```bash
export OPENROUTER_API_KEY=sk-or-v1-your-key
mvn spring-boot:run
```

### Issue: "Connection refused to localhost:7233"
**Fix:** Start Temporal server
```bash
temporal server start-dev
```

Or disable Temporal in application.yml:
```yaml
temporal:
  worker:
    enabled: false
```

### Issue: AI returns "MANUAL_REVIEW" for all claims
**Fix:** Check API key is valid and OpenRouter API is accessible

### Issue: "Rate limit exceeded"
**Fix:** Switch to a different model or wait. OpenRouter has per-model rate limits.

## Configuration Options

Edit `src/main/resources/application.yml`:

```yaml
beema:
  ai:
    enabled: true               # Enable/disable AI
    model-provider: openrouter  # AI provider (OpenRouter)
    model: openai/gpt-4-turbo-preview  # Model to use
    max-retries: 3              # Retry attempts
    timeout-seconds: 30         # Request timeout
    fallback-to-rules: true     # Fallback on error
    auto-select-model: false    # Auto model selection

spring:
  ai:
    openai:
      base-url: https://openrouter.ai/api/v1  # OpenRouter endpoint
      chat:
        options:
          model: openai/gpt-4-turbo-preview  # Model version
          temperature: 0.3                    # Creativity (0-1)
          max-tokens: 2000                    # Response length
```

## API Endpoints

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/v1/claims/analysis/analyze` | POST | Sync AI analysis |
| `/api/v1/claims/analysis/process-with-workflow` | POST | Async workflow |
| `/api/v1/claims/analysis/workflow/{id}` | GET | Get workflow result |
| `/api-docs` | GET | OpenAPI spec |
| `/swagger-ui` | GET | API documentation |

## Cost Estimation

### Pricing via OpenRouter

| Model | Input/1M tokens | Output/1M tokens | Per Claim Cost |
|-------|----------------|------------------|----------------|
| GPT-4 Turbo | $10 | $30 | $0.011 |
| Claude 3 Opus | $15 | $75 | $0.022 |
| Claude 3 Sonnet | $3 | $15 | $0.004 |
| GPT-4o Mini | $0.15 | $0.60 | $0.0002 |
| Llama 3.1 70B | $0.35 | $0.40 | $0.0005 |

**Example:** 1,000 claims/day with Claude Sonnet = ~$4/day = $120/month

### Optimization Tips
1. Enable auto-select-model for dynamic routing
2. Use Llama 3.1 for simple claims (95% cheaper)
3. Cache repeated analyses
4. Batch similar claims
5. Set max-tokens limit

See `OPENROUTER_SETUP.md` for detailed pricing and model selection guide.

## Next Steps

1. **OpenRouter Setup**: Read [OPENROUTER_SETUP.md](OPENROUTER_SETUP.md) for detailed configuration
2. **Full Documentation**: Read [AI_AGENT_GUIDE.md](AI_AGENT_GUIDE.md) for architecture
3. **Implementation Details**: Review [AI_INTEGRATION_SUMMARY.md](AI_INTEGRATION_SUMMARY.md)
4. **Explore Models**: Visit [https://openrouter.ai/models](https://openrouter.ai/models)
5. **Customize Prompts**: Edit `ClaimAnalyzerService.java`
6. **Auto-Selection**: Configure `ModelSelectionService.java`

## Support

- **OpenRouter Setup**: `OPENROUTER_SETUP.md`
- **AI Architecture**: `AI_AGENT_GUIDE.md`
- **Implementation**: `AI_INTEGRATION_SUMMARY.md`
- **Verification**: `VERIFICATION.md`
- **OpenRouter Docs**: [https://openrouter.ai/docs](https://openrouter.ai/docs)

## Example: Full Claim Request

```json
{
  "claimId": "550e8400-e29b-41d4-a716-446655440000",
  "claimNumber": "CLM-2026-001",
  "claimType": "motor_accident",
  "claimAmount": 5000.0,
  "policyNumber": "POL-2026-001",
  "marketContext": "RETAIL",
  "status": "REPORTED",
  "description": "Minor collision, rear bumper damage, no injuries",
  "incidentDate": "2026-02-10T10:30:00Z",
  "reportedDate": "2026-02-12T09:00:00Z",
  "claimData": {
    "vehicleMake": "Toyota",
    "vehicleModel": "Camry",
    "repairShop": "ABC Auto Body",
    "estimatedRepairCost": 4500.0
  }
}
```

Happy analyzing!
