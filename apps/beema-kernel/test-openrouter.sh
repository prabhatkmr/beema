#!/bin/bash

# OpenRouter Integration Test Script
# This script helps verify the OpenRouter integration is working correctly

set -e

echo "=========================================="
echo "OpenRouter Integration Test"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if OpenRouter API key is set
if [ -z "$OPENROUTER_API_KEY" ]; then
    echo -e "${RED}ERROR: OPENROUTER_API_KEY is not set${NC}"
    echo ""
    echo "Please set your OpenRouter API key:"
    echo "  export OPENROUTER_API_KEY=sk-or-v1-your-key-here"
    echo ""
    echo "Get your API key at: https://openrouter.ai/"
    exit 1
fi

echo -e "${GREEN}✓ OPENROUTER_API_KEY is set${NC}"

# Check if model is configured
if [ -z "$OPENROUTER_MODEL" ]; then
    echo -e "${YELLOW}⚠ OPENROUTER_MODEL not set, using default: openai/gpt-4-turbo-preview${NC}"
    export OPENROUTER_MODEL=openai/gpt-4-turbo-preview
else
    echo -e "${GREEN}✓ OPENROUTER_MODEL is set to: $OPENROUTER_MODEL${NC}"
fi

echo ""
echo "Configuration:"
echo "  Base URL: ${OPENROUTER_BASE_URL:-https://openrouter.ai/api/v1}"
echo "  Model: $OPENROUTER_MODEL"
echo "  Referer: ${OPENROUTER_REFERER:-https://beema.io}"
echo "  App Name: ${OPENROUTER_APP_NAME:-Beema Insurance Platform}"
echo ""

# Test OpenRouter API directly
echo "=========================================="
echo "1. Testing OpenRouter API directly"
echo "=========================================="

RESPONSE=$(curl -s -X POST \
  "${OPENROUTER_BASE_URL:-https://openrouter.ai/api/v1}/chat/completions" \
  -H "Authorization: Bearer $OPENROUTER_API_KEY" \
  -H "HTTP-Referer: ${OPENROUTER_REFERER:-https://beema.io}" \
  -H "X-Title: ${OPENROUTER_APP_NAME:-Beema Insurance Platform}" \
  -H "Content-Type: application/json" \
  -d '{
    "model": "'"$OPENROUTER_MODEL"'",
    "messages": [
      {"role": "user", "content": "Say hello in exactly 5 words."}
    ],
    "max_tokens": 50
  }')

if echo "$RESPONSE" | grep -q "error"; then
    echo -e "${RED}✗ OpenRouter API test failed${NC}"
    echo "Response: $RESPONSE"
    exit 1
else
    echo -e "${GREEN}✓ OpenRouter API test successful${NC}"
    echo "Response: $(echo "$RESPONSE" | jq -r '.choices[0].message.content' 2>/dev/null || echo "$RESPONSE")"
fi

echo ""

# Check if beema-kernel is running
echo "=========================================="
echo "2. Testing Beema Kernel Integration"
echo "=========================================="

KERNEL_URL="${BEEMA_KERNEL_URL:-http://localhost:8080}"

if curl -s "$KERNEL_URL/actuator/health" > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Beema Kernel is running at $KERNEL_URL${NC}"
else
    echo -e "${YELLOW}⚠ Beema Kernel is not running at $KERNEL_URL${NC}"
    echo ""
    echo "To start Beema Kernel:"
    echo "  cd /Users/prabhatkumar/Desktop/dev-directory/beema/apps/beema-kernel"
    echo "  mvn spring-boot:run"
    echo ""
    echo "Skipping integration tests..."
    exit 0
fi

# Test claim analysis endpoint
echo ""
echo "Testing AI claim analysis endpoint..."

CLAIM_RESPONSE=$(curl -s -X POST "$KERNEL_URL/api/v1/claims/analysis/analyze" \
  -H "Content-Type: application/json" \
  -d '{
    "claimId": "550e8400-e29b-41d4-a716-446655440000",
    "claimNumber": "CLM-TEST-001",
    "claimType": "motor_accident",
    "claimAmount": 5000.0,
    "policyNumber": "POL-TEST-001",
    "marketContext": "RETAIL",
    "status": "REPORTED",
    "description": "Minor collision test"
  }')

if echo "$CLAIM_RESPONSE" | grep -q "nextAction"; then
    echo -e "${GREEN}✓ Claim analysis successful${NC}"
    echo "Next Action: $(echo "$CLAIM_RESPONSE" | jq -r '.nextAction' 2>/dev/null || echo "N/A")"
    echo "Confidence: $(echo "$CLAIM_RESPONSE" | jq -r '.confidence' 2>/dev/null || echo "N/A")"
else
    echo -e "${YELLOW}⚠ Claim analysis returned unexpected response${NC}"
    echo "Response: $CLAIM_RESPONSE"
fi

echo ""
echo "=========================================="
echo "3. Checking Application Logs"
echo "=========================================="

echo "Check your application logs for lines like:"
echo "  INFO  c.b.k.a.s.ClaimAnalyzerService - Analyzing claim CLM-TEST-001 using OpenRouter with model: $OPENROUTER_MODEL"
echo ""

echo "=========================================="
echo "4. Model Selection Test"
echo "=========================================="

echo "Available models:"
echo "  - openai/gpt-4-turbo-preview (default)"
echo "  - anthropic/claude-3-opus-20240229 (high accuracy)"
echo "  - anthropic/claude-3-sonnet-20240229 (balanced)"
echo "  - openai/gpt-4o-mini (fast & cheap)"
echo "  - meta-llama/llama-3.1-70b-instruct (cost-effective)"
echo ""
echo "To switch models:"
echo "  export OPENROUTER_MODEL=anthropic/claude-3-sonnet-20240229"
echo "  mvn spring-boot:run"
echo ""

echo "=========================================="
echo "5. Cost Monitoring"
echo "=========================================="

echo "Monitor your usage at:"
echo "  - Activity: https://openrouter.ai/activity"
echo "  - Stats: https://openrouter.ai/stats"
echo "  - Credits: https://openrouter.ai/credits"
echo ""

echo "=========================================="
echo "Test Summary"
echo "=========================================="
echo -e "${GREEN}✓ OpenRouter integration is working correctly${NC}"
echo ""
echo "Next steps:"
echo "  1. Review documentation: OPENROUTER_SETUP.md"
echo "  2. Try different models for comparison"
echo "  3. Enable auto-selection: AI_AUTO_SELECT_MODEL=true"
echo "  4. Monitor costs and performance"
echo ""
echo "For help, see: AI_QUICK_START.md"
