# OpenRouter Setup Guide

## Quick Start

### 1. Get API Key

1. Visit [https://openrouter.ai/](https://openrouter.ai/)
2. Sign up or log in
3. Navigate to API Keys
4. Create a new key
5. Copy your key (starts with `sk-or-v1-`)

### 2. Configure Beema

```bash
cd /Users/prabhatkumar/Desktop/dev-directory/beema/apps/beema-kernel

# Create .env file
cat > .env << 'EOF'
OPENROUTER_API_KEY=sk-or-v1-your-key-here
OPENROUTER_MODEL=openai/gpt-4-turbo-preview
OPENROUTER_BASE_URL=https://openrouter.ai/api/v1
OPENROUTER_REFERER=https://beema.io
OPENROUTER_APP_NAME=Beema Insurance Platform
AI_ENABLED=true
EOF

# Load environment
source .env
```

### 3. Test Connection

```bash
# Start beema-kernel
mvn spring-boot:run

# Test AI analysis (in another terminal)
curl -X POST http://localhost:8080/api/v1/claims/analysis/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "claimNumber": "CLM-001",
    "claimType": "motor_accident",
    "claimAmount": 5000,
    "marketContext": "RETAIL"
  }'
```

## Model Selection

### By Use Case

**High Accuracy (Complex Claims)**
```bash
OPENROUTER_MODEL=anthropic/claude-3-opus-20240229
```

**Balanced (Most Claims)**
```bash
OPENROUTER_MODEL=anthropic/claude-3-sonnet-20240229
```

**Speed & Cost (Simple Claims)**
```bash
OPENROUTER_MODEL=openai/gpt-4o-mini
```

### Auto-Selection

Enable automatic model selection based on claim characteristics:

```yaml
beema:
  ai:
    auto-select-model: true
```

This will:
- Use Claude Opus for claims > $100k
- Use Claude Sonnet for complex claim types
- Use GPT-4o Mini for standard claims

## Pricing Comparison

| Model | Input (per 1M tokens) | Output (per 1M tokens) | Best For |
|-------|----------------------|------------------------|----------|
| GPT-4 Turbo | $10 | $30 | Complex reasoning |
| Claude 3 Opus | $15 | $75 | Highest accuracy |
| Claude 3 Sonnet | $3 | $15 | Balanced performance |
| GPT-4o Mini | $0.15 | $0.60 | High volume, speed |
| Llama 3.1 70B | $0.35 | $0.40 | Cost-effective |

*Prices via OpenRouter as of 2024*

## Available Models

### OpenAI
- `openai/gpt-4-turbo-preview` - Best for complex reasoning
- `openai/gpt-4o` - Multimodal, fast
- `openai/gpt-4o-mini` - Cost-effective
- `openai/gpt-3.5-turbo` - Legacy, cheap

### Anthropic
- `anthropic/claude-3-opus-20240229` - Highest accuracy
- `anthropic/claude-3-sonnet-20240229` - Balanced
- `anthropic/claude-3-haiku-20240307` - Fast, cheap

### Google
- `google/gemini-pro-1.5` - Large context window
- `google/gemini-flash-1.5` - Fast, affordable

### Meta
- `meta-llama/llama-3.1-70b-instruct` - Open source
- `meta-llama/llama-3.1-405b-instruct` - Large, powerful

### Perplexity (with internet access)
- `perplexity/llama-3.1-sonar-large-128k-online` - Real-time web search

## Configuration Reference

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `OPENROUTER_API_KEY` | Your OpenRouter API key | Required |
| `OPENROUTER_MODEL` | Model to use | `openai/gpt-4-turbo-preview` |
| `OPENROUTER_BASE_URL` | OpenRouter endpoint | `https://openrouter.ai/api/v1` |
| `OPENROUTER_REFERER` | Your site URL | `https://beema.io` |
| `OPENROUTER_APP_NAME` | Your app name | `Beema Insurance Platform` |
| `AI_ENABLED` | Enable/disable AI | `true` |
| `AI_AUTO_SELECT_MODEL` | Auto model selection | `false` |

### application.yml

```yaml
beema:
  ai:
    model: ${OPENROUTER_MODEL:openai/gpt-4-turbo-preview}
    auto-select-model: false
    openrouter:
      site-url: https://beema.io
      app-name: Beema Insurance Platform

spring:
  ai:
    openai:
      base-url: https://openrouter.ai/api/v1
      api-key: ${OPENROUTER_API_KEY}
      chat:
        options:
          model: ${OPENROUTER_MODEL:openai/gpt-4-turbo-preview}
          temperature: 0.3
          max-tokens: 2000
```

## Monitoring Usage

Check your usage and costs at:
- Dashboard: [https://openrouter.ai/activity](https://openrouter.ai/activity)
- API Stats: [https://openrouter.ai/stats](https://openrouter.ai/stats)

## Troubleshooting

### Invalid API Key
```
Error: 401 Unauthorized
```
**Solution:**
- Check your API key is correct
- Ensure it starts with `sk-or-v1-`
- Verify key has sufficient credits at [https://openrouter.ai/credits](https://openrouter.ai/credits)

### Model Not Found
```
Error: Model not found
```
**Solution:**
- Check model name matches OpenRouter format: `provider/model-name`
- See available models at [https://openrouter.ai/models](https://openrouter.ai/models)
- Ensure model is not deprecated

### Rate Limiting
```
Error: 429 Too Many Requests
```
**Solution:**
- OpenRouter has per-model rate limits
- Switch to a different model
- Add retry logic (already configured in beema-kernel)
- Check rate limits at [https://openrouter.ai/docs#rate-limits](https://openrouter.ai/docs#rate-limits)

### Connection Timeout
```
Error: Read timed out
```
**Solution:**
- Increase timeout in application.yml:
  ```yaml
  beema:
    ai:
      timeout-seconds: 60
  ```
- Check network connectivity
- Try a faster model

### Invalid Response Format
```
Error: Failed to parse AI response
```
**Solution:**
- Check prompt format
- Ensure model supports function calling
- Try a different model (GPT-4, Claude recommended)

## Best Practices

### 1. Model Selection
- Use **Claude 3 Opus** for high-stakes, complex claims
- Use **Claude 3 Sonnet** for most production workloads
- Use **GPT-4o Mini** for high-volume, simple claims
- Use **Llama 3.1 70B** for cost-sensitive deployments

### 2. Cost Optimization
- Enable auto-select-model for dynamic routing
- Cache responses when possible
- Use streaming for long responses
- Monitor usage regularly

### 3. Error Handling
- Always implement fallback logic
- Log all AI interactions for debugging
- Set appropriate timeouts
- Use retry with exponential backoff

### 4. Security
- Store API keys in environment variables
- Never commit `.env` files
- Rotate keys regularly
- Monitor for unusual usage

## Advanced Configuration

### Custom Model Selection Logic

Edit `ModelSelectionService.java`:

```java
public String selectModelForClaim(Claim claim) {
    // Custom logic for your business rules
    if (claim.getClaimAmount() > 500000) {
        return OpenRouterModels.CLAUDE_3_OPUS;
    }
    if (claim.getMarketContext().equals("LONDON_MARKET")) {
        return OpenRouterModels.CLAUDE_3_SONNET;
    }
    return OpenRouterModels.GPT_4O_MINI;
}
```

### Runtime Model Override

Override model for specific requests:

```java
chatClient.prompt()
    .options(ChatOptions.builder()
        .withModel("anthropic/claude-3-opus-20240229")
        .build())
    .user(prompt)
    .call();
```

## Support

- OpenRouter Docs: [https://openrouter.ai/docs](https://openrouter.ai/docs)
- Discord: [https://discord.gg/openrouter](https://discord.gg/openrouter)
- Status: [https://status.openrouter.ai/](https://status.openrouter.ai/)
- Email: support@openrouter.ai

## Migration from OpenAI

If migrating from OpenAI directly:

1. Update environment variable:
   ```bash
   # Old
   OPENAI_API_KEY=sk-...

   # New
   OPENROUTER_API_KEY=sk-or-v1-...
   OPENROUTER_MODEL=openai/gpt-4-turbo-preview
   ```

2. No code changes needed - OpenRouter is API-compatible

3. Test thoroughly with your workload

4. Monitor costs and performance

## Next Steps

1. Review `AI_AGENT_GUIDE.md` for architecture details
2. Check `AI_QUICK_START.md` for quick examples
3. Explore available models at [https://openrouter.ai/models](https://openrouter.ai/models)
4. Set up monitoring and alerting
5. Configure auto-selection for production
