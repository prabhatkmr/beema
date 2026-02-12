# OpenRouter AI Integration - README

## Quick Overview

Beema Kernel now uses **OpenRouter** for AI-powered claim analysis, providing access to multiple LLM providers through a single API.

## What Changed?

The AI agent integration has been updated to use OpenRouter instead of OpenAI directly. This gives you access to:

- **OpenAI** (GPT-4, GPT-4 Turbo, GPT-4o)
- **Anthropic** (Claude 3 Opus, Sonnet, Haiku)
- **Google** (Gemini Pro 1.5)
- **Meta** (Llama 3.1)
- And many more providers...

## Getting Started (3 Steps)

### 1. Get OpenRouter API Key

Visit [https://openrouter.ai/](https://openrouter.ai/) and create an account to get your API key.

### 2. Configure Environment

```bash
export OPENROUTER_API_KEY=sk-or-v1-your-key-here
export OPENROUTER_MODEL=openai/gpt-4-turbo-preview
```

### 3. Run & Test

```bash
# Start the application
mvn spring-boot:run

# Test the integration (in another terminal)
./test-openrouter.sh
```

## Documentation

| Document | Description |
|----------|-------------|
| **[OPENROUTER_SETUP.md](OPENROUTER_SETUP.md)** | Comprehensive setup and configuration guide |
| **[OPENROUTER_MIGRATION.md](OPENROUTER_MIGRATION.md)** | Complete migration summary and technical details |
| **[AI_QUICK_START.md](AI_QUICK_START.md)** | Quick start guide for AI claim analysis |
| **[AI_AGENT_GUIDE.md](AI_AGENT_GUIDE.md)** | Detailed architecture and usage guide |
| **[test-openrouter.sh](test-openrouter.sh)** | Test script to verify the integration |

## Key Features

### Multi-Provider Access
Switch between providers without code changes:

```bash
# Use Claude 3 Sonnet (recommended for production)
export OPENROUTER_MODEL=anthropic/claude-3-sonnet-20240229

# Use GPT-4o Mini (faster & cheaper)
export OPENROUTER_MODEL=openai/gpt-4o-mini

# Use Llama 3.1 (open source & cost-effective)
export OPENROUTER_MODEL=meta-llama/llama-3.1-70b-instruct
```

### Auto Model Selection
Enable automatic model selection based on claim characteristics:

```yaml
beema:
  ai:
    auto-select-model: true
```

This will:
- Use Claude 3 Opus for high-value claims (> $100k)
- Use Claude 3 Sonnet for complex claim types
- Use GPT-4o Mini for standard claims

### Cost Optimization

| Model | Cost/Claim | Best For |
|-------|------------|----------|
| Claude 3 Opus | $0.022 | High-value, complex claims |
| Claude 3 Sonnet | $0.004 | Production (recommended) |
| GPT-4o Mini | $0.0002 | High-volume, simple claims |
| Llama 3.1 70B | $0.0005 | Cost-conscious deployments |

Example: 1,000 claims/day with Claude Sonnet = ~$120/month

## File Structure

### New Java Classes

```
src/main/java/com/beema/kernel/ai/
├── config/
│   └── OpenRouterConfig.java          # Spring AI configuration for OpenRouter
├── service/
│   ├── ClaimAnalyzerService.java      # Updated with model logging
│   └── ModelSelectionService.java     # Auto model selection logic
└── util/
    └── OpenRouterModels.java          # Model constants and recommendations
```

### Configuration Files

```
apps/beema-kernel/
├── .env.example                       # Environment variable template
├── src/main/resources/
│   └── application.yml                # Updated Spring configuration
└── src/test/java/com/beema/kernel/ai/
    └── service/
        └── ClaimAnalyzerServiceTest.java  # Updated tests
```

### Documentation

```
apps/beema-kernel/
├── README_OPENROUTER.md               # This file
├── OPENROUTER_SETUP.md                # Setup guide
├── OPENROUTER_MIGRATION.md            # Migration details
├── AI_QUICK_START.md                  # Quick start (updated)
├── AI_AGENT_GUIDE.md                  # Architecture guide (updated)
└── test-openrouter.sh                 # Test script
```

## Environment Variables

| Variable | Required | Default | Description |
|----------|----------|---------|-------------|
| `OPENROUTER_API_KEY` | Yes | - | Your OpenRouter API key |
| `OPENROUTER_MODEL` | No | `openai/gpt-4-turbo-preview` | Model to use |
| `OPENROUTER_BASE_URL` | No | `https://openrouter.ai/api/v1` | OpenRouter endpoint |
| `OPENROUTER_REFERER` | No | `https://beema.io` | Your site URL |
| `OPENROUTER_APP_NAME` | No | `Beema Insurance Platform` | Your app name |
| `AI_ENABLED` | No | `true` | Enable/disable AI |
| `AI_AUTO_SELECT_MODEL` | No | `false` | Auto model selection |

## Testing

### 1. Run the test script

```bash
./test-openrouter.sh
```

### 2. Test with curl

```bash
curl -X POST http://localhost:8080/api/v1/claims/analysis/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "claimNumber": "CLM-001",
    "claimType": "motor_accident",
    "claimAmount": 5000,
    "marketContext": "RETAIL"
  }'
```

### 3. Run unit tests

```bash
mvn test -Dtest=ClaimAnalyzerServiceTest
```

## Monitoring

### Application Logs
Check logs for model usage:
```
INFO  c.b.k.a.s.ClaimAnalyzerService - Analyzing claim CLM-001 using OpenRouter with model: anthropic/claude-3-sonnet-20240229
```

### OpenRouter Dashboard
Monitor usage and costs:
- Activity: https://openrouter.ai/activity
- Stats: https://openrouter.ai/stats
- Credits: https://openrouter.ai/credits

## Troubleshooting

### API Key Issues
```bash
# Check if key is set
echo $OPENROUTER_API_KEY

# Verify key format (should start with sk-or-v1-)
# Get new key at: https://openrouter.ai/
```

### Model Not Found
```bash
# Check available models at: https://openrouter.ai/models
# Verify model name format: provider/model-name
export OPENROUTER_MODEL=anthropic/claude-3-sonnet-20240229
```

### Connection Issues
```bash
# Test OpenRouter API directly
curl -X POST https://openrouter.ai/api/v1/chat/completions \
  -H "Authorization: Bearer $OPENROUTER_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"model":"openai/gpt-4o-mini","messages":[{"role":"user","content":"test"}]}'
```

See [OPENROUTER_SETUP.md](OPENROUTER_SETUP.md) for more troubleshooting tips.

## Best Practices

### 1. Production Configuration
```yaml
beema:
  ai:
    model: anthropic/claude-3-sonnet-20240229  # Balanced performance
    auto-select-model: false                    # Disable for predictable costs
    max-retries: 3
    timeout-seconds: 30
    fallback-to-rules: true
```

### 2. Cost Optimization
- Use auto-select-model for dynamic routing
- Monitor usage regularly
- Set appropriate max-tokens limits
- Cache responses when possible

### 3. Security
- Never commit `.env` files
- Store API keys in environment variables
- Rotate keys regularly
- Monitor for unusual usage

### 4. Error Handling
- Always implement fallback logic
- Log all AI interactions
- Set appropriate timeouts
- Use retry with exponential backoff

## Migration from OpenAI

If you were using OpenAI directly:

1. Update environment variable:
   ```bash
   # Old
   export OPENAI_API_KEY=sk-...

   # New
   export OPENROUTER_API_KEY=sk-or-v1-...
   export OPENROUTER_MODEL=openai/gpt-4-turbo-preview
   ```

2. Restart application (no code changes needed)

3. Verify integration with test script

See [OPENROUTER_MIGRATION.md](OPENROUTER_MIGRATION.md) for complete migration guide.

## Support

### Documentation
- Setup: [OPENROUTER_SETUP.md](OPENROUTER_SETUP.md)
- Migration: [OPENROUTER_MIGRATION.md](OPENROUTER_MIGRATION.md)
- Quick Start: [AI_QUICK_START.md](AI_QUICK_START.md)
- Architecture: [AI_AGENT_GUIDE.md](AI_AGENT_GUIDE.md)

### OpenRouter Resources
- Website: https://openrouter.ai/
- Docs: https://openrouter.ai/docs
- Models: https://openrouter.ai/models
- Discord: https://discord.gg/openrouter
- Email: support@openrouter.ai

### Beema Platform
- Contact the Beema Platform team for issues
- File issues in the repository

## What's Next?

1. **Try Different Models**: Test various models to find the best fit
2. **Enable Auto-Selection**: Configure automatic model routing
3. **Monitor Costs**: Track usage at OpenRouter dashboard
4. **Fine-tune Selection**: Customize ModelSelectionService for your needs
5. **A/B Testing**: Compare model performance for your workload

## Summary

- ✅ Migration complete
- ✅ Multiple providers available
- ✅ Cost optimization enabled
- ✅ Auto-selection implemented
- ✅ Comprehensive documentation
- ✅ Testing tools provided
- ✅ No breaking changes

The OpenRouter integration is production-ready and provides significant benefits over direct OpenAI integration.

For detailed information, see [OPENROUTER_SETUP.md](OPENROUTER_SETUP.md).
