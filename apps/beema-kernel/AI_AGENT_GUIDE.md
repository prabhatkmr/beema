# AI Agent Integration Guide

## Overview
Beema Kernel integrates AI-powered claim analysis using **OpenRouter**, which provides access to multiple LLM providers through a single API:
- **OpenAI** (GPT-4, GPT-4 Turbo, GPT-4o)
- **Anthropic** (Claude 3 Opus, Sonnet, Haiku)
- **Google** (Gemini Pro 1.5)
- **Meta** (Llama 3.1)
- **Perplexity** (Sonar models)
- And many more...

## Why OpenRouter?

- **Multiple Providers**: Switch between OpenAI, Anthropic, Google without code changes
- **Cost Optimization**: Choose cheaper models for simple tasks
- **Fallback Support**: Automatically failover if a model is unavailable
- **Rate Limiting**: Better rate limit management across providers
- **Single API Key**: No need to manage multiple API keys

## Architecture

### Components

1. **InsuranceTools** (`com.beema.kernel.ai.tools.InsuranceTools`)
   - Exposes Beema services as AI-callable functions
   - Provides 5 tools for the LLM to interact with the insurance domain

2. **ClaimAnalyzerService** (`com.beema.kernel.ai.service.ClaimAnalyzerService`)
   - Orchestrates AI analysis with tool calling
   - Parses AI responses into actionable recommendations

3. **AgentActivities** (`com.beema.kernel.workflow.claim.AgentActivities`)
   - Temporal activity for workflow integration
   - Enables long-running, durable AI analysis

4. **ClaimWorkflow** (`com.beema.kernel.workflow.claim.ClaimWorkflow`)
   - Temporal workflow for asynchronous claim processing
   - Routes claims based on AI recommendations

## Available AI Tools

The LLM has access to these tools during claim analysis:

### 1. getFieldMetadata
Retrieves field definitions and validation rules for an agreement type.

**Parameters:**
- `agreementType`: Agreement type code (e.g., "motor_comprehensive")
- `marketContext`: RETAIL, COMMERCIAL, or LONDON_MARKET
- `tenantId`: Tenant UUID

**Returns:** JSON with attribute schema, validation rules, and calculation rules

### 2. evaluateExpression
Executes JEXL expressions against claim data.

**Parameters:**
- `expression`: JEXL expression (e.g., "claimAmount > 10000")
- `context`: Map of variables for expression evaluation

**Returns:** Expression result with type information

### 3. validateSchema
Validates claim attributes against agreement type schema.

**Parameters:**
- `agreementTypeId`: Agreement type UUID
- `attributes`: Map of claim attributes to validate

**Returns:** Validation result with error details

### 4. getValidationRules
Retrieves all validation rules for an agreement type.

**Parameters:**
- `agreementType`: Agreement type code
- `marketContext`: Market context
- `tenantId`: Tenant UUID

**Returns:** JSON validation rules

### 5. calculateClaimMetrics
Calculates claim ratios and severity.

**Parameters:**
- `claimAmount`: Claim amount
- `sumInsured`: Policy sum insured
- `excess`: Optional excess/deductible

**Returns:** Claim ratio, payable amount, and severity rating

## Model Comparison for Claim Analysis

| Model | Best For | Cost | Speed | Notes |
|-------|----------|------|-------|-------|
| GPT-4 Turbo | Complex claims, high accuracy | $$$ | Medium | Best for complex reasoning |
| Claude 3 Opus | Insurance domain expertise | $$$$ | Slow | Highest accuracy, best for high-value claims |
| Claude 3 Sonnet | Balanced performance | $$ | Fast | Best for most production workloads |
| Gemini Pro 1.5 | Large document analysis | $$ | Fast | 1M token context window |
| Llama 3.1 70B | Cost-effective production | $ | Fast | Open source, 95% cheaper |

**Recommendation**: Use Claude 3 Sonnet for production (balanced cost/performance).

## Configuration

### Environment Variables

```bash
# Required
export OPENROUTER_API_KEY=sk-or-v1-your-actual-key

# Model Selection
export OPENROUTER_MODEL=openai/gpt-4-turbo-preview

# Alternative models:
# export OPENROUTER_MODEL=anthropic/claude-3-opus-20240229
# export OPENROUTER_MODEL=anthropic/claude-3-sonnet-20240229
# export OPENROUTER_MODEL=google/gemini-pro-1.5
# export OPENROUTER_MODEL=meta-llama/llama-3.1-70b-instruct

# Optional
export AI_ENABLED=true
export AI_AUTO_SELECT_MODEL=false
export TEMPORAL_WORKER_ENABLED=true
```

### Application Configuration

See `application.yml`:

```yaml
beema:
  ai:
    enabled: ${AI_ENABLED:true}
    model-provider: openrouter
    model: ${OPENROUTER_MODEL:openai/gpt-4-turbo-preview}
    max-retries: 3
    timeout-seconds: 30
    fallback-to-rules: true
    auto-select-model: false
    openrouter:
      site-url: https://beema.io
      app-name: Beema Insurance Platform

spring:
  ai:
    openai:
      base-url: https://openrouter.ai/api/v1
      api-key: ${OPENROUTER_API_KEY:sk-placeholder}
      chat:
        options:
          model: ${OPENROUTER_MODEL:openai/gpt-4-turbo-preview}
          temperature: 0.3
          max-tokens: 2000
```

### Switch Models at Runtime

You can switch models without restarting:

```java
@Value("${beema.ai.model}")
private String modelName;

// Override model for specific analysis
chatClient.prompt()
    .options(ChatOptions.builder()
        .withModel("anthropic/claude-3-opus-20240229")
        .build())
    .user(prompt)
    .call();
```

## Usage

### 1. Direct API Call (Synchronous)

Analyze a claim immediately and get recommendations:

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
    "description": "Minor collision, rear bumper damage",
    "incidentDate": "2026-02-10T10:30:00Z",
    "reportedDate": "2026-02-12T09:00:00Z"
  }'
```

**Response:**
```json
{
  "claimId": "550e8400-e29b-41d4-a716-446655440000",
  "nextAction": "APPROVE_IMMEDIATELY",
  "confidence": 0.9,
  "reasoning": "Low-value claim with clear documentation. No fraud indicators.",
  "aiAnalysis": "..."
}
```

### 2. Temporal Workflow (Asynchronous)

Start a durable, long-running claim processing workflow:

```bash
curl -X POST http://localhost:8080/api/v1/claims/analysis/process-with-workflow \
  -H "Content-Type: application/json" \
  -d '{
    "claimId": "550e8400-e29b-41d4-a716-446655440000",
    "claimNumber": "CLM-2026-002",
    "claimType": "property_damage",
    "claimAmount": 150000.0,
    "policyNumber": "POL-2026-002",
    "marketContext": "COMMERCIAL",
    "status": "REPORTED"
  }'
```

**Response:**
```json
{
  "workflowId": "claim-ai-CLM-2026-002",
  "status": "STARTED"
}
```

### 3. Check Workflow Status

Retrieve workflow execution result:

```bash
curl http://localhost:8080/api/v1/claims/analysis/workflow/claim-ai-CLM-2026-002
```

**Response:**
```json
{
  "claimId": "550e8400-e29b-41d4-a716-446655440000",
  "finalStatus": "ESCALATED",
  "aiAnalysis": {
    "nextAction": "ESCALATE_TO_SPECIALIST",
    "confidence": 0.85,
    "reasoning": "High-value commercial claim requires specialist review."
  }
}
```

## AI Recommendations

The AI provides one of these next actions:

| Action | Description | Typical Confidence |
|--------|-------------|-------------------|
| `APPROVE_IMMEDIATELY` | Straightforward, low-value claim | > 0.85 |
| `REQUEST_DOCUMENTS` | Missing documentation | > 0.8 |
| `ESCALATE_TO_SPECIALIST` | High-value or complex | > 0.75 |
| `REFER_TO_INVESTIGATOR` | Fraud indicators detected | > 0.7 |
| `REJECT` | Clear policy violation | > 0.85 |
| `MANUAL_REVIEW` | Uncertain or AI failure | < 0.6 |

## Fallback Strategy

If AI analysis fails (API error, timeout, etc.), the system automatically returns:

```json
{
  "nextAction": "MANUAL_REVIEW",
  "confidence": 0.0,
  "reasoning": "AI analysis failed: [error message]. Requires manual review."
}
```

This ensures claims are never lost or auto-rejected due to technical issues.

## Development

### Running Tests

```bash
cd /Users/prabhatkumar/Desktop/dev-directory/beema/apps/beema-kernel
mvn test -Dtest=ClaimAnalyzerServiceTest
```

Note: Tests require mocking or a valid OpenRouter API key.

### Adding New Tools

To add a new AI-callable tool:

1. Create a record in `InsuranceTools.java`:
   ```java
   public record MyNewToolRequest(
       @JsonProperty(required = true)
       @JsonPropertyDescription("Parameter description")
       String parameter
   ) {}
   ```

2. Implement the function:
   ```java
   public Function<MyNewToolRequest, String> myNewTool() {
       return request -> {
           // Implementation
           return "Result";
       };
   }
   ```

3. Register in `ClaimAnalyzerService`:
   ```java
   .functions(
       "myNewTool", insuranceTools.myNewTool(),
       // ... other tools
   )
   ```

## Monitoring

### Metrics

The AI analysis service exposes these metrics:

- `claim_analysis_duration_seconds`: Time taken for AI analysis
- `claim_analysis_total`: Total number of analyses
- `claim_analysis_failures`: Number of AI failures

### Logs

AI analysis logs include model information:

```
INFO  c.b.k.a.s.ClaimAnalyzerService - Analyzing claim CLM-2026-001 using OpenRouter with model: anthropic/claude-3-sonnet-20240229
INFO  c.b.k.w.c.AgentActivitiesImpl - AI Analysis Complete - Next Action: APPROVE_IMMEDIATELY, Confidence: 0.9
```

### OpenRouter Usage Monitoring

Monitor your usage at [https://openrouter.ai/activity](https://openrouter.ai/activity):
- API calls by model
- Cost per model
- Rate limit status
- Error rates

## Security Considerations

1. **API Key Protection**: Never commit `OPENROUTER_API_KEY` to source control
2. **Expression Sandboxing**: JEXL engine uses restricted permissions
3. **Input Validation**: All claim data is validated before AI processing
4. **Audit Trail**: All AI recommendations are logged with workflow IDs and model names
5. **Model Selection**: Use trusted models from reputable providers

## Troubleshooting

### Issue: AI returns "MANUAL_REVIEW" for all claims

**Solution:** Check OpenRouter API key is valid, has credits, and rate limits are not exceeded.

### Issue: Workflow times out

**Solution:** Increase workflow timeout in `ClaimWorkflowImpl.java`:
```java
.setStartToCloseTimeout(Duration.ofMinutes(10))
```

### Issue: Tools not available to LLM

**Solution:** Verify `InsuranceTools` component is autowired in `ClaimAnalyzerService`.

## Future Enhancements

1. **Vector Database Integration**: Store historical claim decisions for RAG
2. **Fine-tuned Models**: Train domain-specific model on insurance claims
3. **Multi-Agent Workflows**: Specialist agents for fraud, underwriting, etc.
4. **Real-time Feedback Loop**: Learn from adjuster overrides
5. **Dynamic Model Selection**: A/B test different models for performance

## Additional Resources

- **OpenRouter Setup**: See [OPENROUTER_SETUP.md](OPENROUTER_SETUP.md) for detailed configuration
- **Quick Start**: See [AI_QUICK_START.md](AI_QUICK_START.md) for getting started
- **OpenRouter Models**: Browse at [https://openrouter.ai/models](https://openrouter.ai/models)
- **OpenRouter Docs**: [https://openrouter.ai/docs](https://openrouter.ai/docs)

## Support

For issues or questions:
- Beema Platform team
- OpenRouter support: support@openrouter.ai
- OpenRouter Discord: [https://discord.gg/openrouter](https://discord.gg/openrouter)
