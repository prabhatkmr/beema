#!/bin/bash

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔═══════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   Beema Webhooks End-to-End Verification            ║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════════════════════╝${NC}"
echo ""

# Configuration
BEEMA_KERNEL_URL="${BEEMA_KERNEL_URL:-http://localhost:8080}"
STUDIO_URL="${STUDIO_URL:-http://localhost:3000}"
INNGEST_URL="${INNGEST_URL:-http://localhost:8288}"
POSTGRES_HOST="${POSTGRES_HOST:-localhost}"
POSTGRES_PORT="${POSTGRES_PORT:-5433}"
POSTGRES_DB="${POSTGRES_DB:-beema_kernel}"
POSTGRES_USER="${POSTGRES_USER:-beema}"

# Step 0: Prerequisites Check
echo -e "${YELLOW}Step 0: Checking Prerequisites${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Check if services are running
echo -n "Checking beema-kernel... "
if curl -sf "$BEEMA_KERNEL_URL/actuator/health" > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC}"
else
    echo -e "${RED}✗ beema-kernel is not running${NC}"
    echo "Start it with: cd apps/beema-kernel && mvn spring-boot:run"
    exit 1
fi

echo -n "Checking Studio... "
if curl -sf "$STUDIO_URL" > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC}"
else
    echo -e "${YELLOW}⚠ Studio is not running (optional)${NC}"
fi

echo -n "Checking Inngest Dev Server... "
if curl -sf "$INNGEST_URL/health" > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC}"
else
    echo -e "${RED}✗ Inngest Dev Server is not running${NC}"
    echo "Start it with: docker-compose up -d inngest"
    echo "Or: npx inngest-cli@latest dev"
    exit 1
fi

echo -n "Checking PostgreSQL... "
if docker exec beema-postgres pg_isready -U "$POSTGRES_USER" > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC}"
else
    echo -e "${RED}✗ PostgreSQL is not running${NC}"
    exit 1
fi

echo ""

# Step 1: Setup - Insert Test Webhook
echo -e "${YELLOW}Step 1: Setup - Inserting Test Webhook${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Generate unique webhook name with timestamp
WEBHOOK_NAME="test-webhook-$(date +%s)"
WEBHOOK_SECRET="whsec_test_$(openssl rand -hex 16)"

echo "Creating webhook:"
echo "  Name: $WEBHOOK_NAME"
echo "  Event: agreement/created"
echo "  URL: https://httpbin.org/post"
echo ""

# Insert webhook into database
SQL_INSERT="
INSERT INTO sys_webhooks (
    webhook_name,
    tenant_id,
    event_type,
    url,
    secret,
    enabled,
    headers,
    retry_config,
    created_by
) VALUES (
    '$WEBHOOK_NAME',
    'default',
    'agreement/created',
    'https://httpbin.org/post',
    '$WEBHOOK_SECRET',
    true,
    '{\"X-Custom-Header\": \"Beema-Test\"}'::jsonb,
    '{\"maxAttempts\": 3, \"backoffMs\": 1000}'::jsonb,
    'test-script'
)
ON CONFLICT (tenant_id, webhook_name) DO UPDATE
SET url = EXCLUDED.url,
    enabled = EXCLUDED.enabled
RETURNING webhook_id;
"

WEBHOOK_ID=$(docker exec beema-postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -t -c "$SQL_INSERT" | xargs)

if [ -z "$WEBHOOK_ID" ]; then
    echo -e "${RED}✗ Failed to insert webhook${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Webhook created with ID: $WEBHOOK_ID${NC}"
echo ""

# Verify webhook was created
echo "Verifying webhook in database..."
WEBHOOK_CHECK=$(docker exec beema-postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -t -c \
    "SELECT COUNT(*) FROM sys_webhooks WHERE webhook_id = $WEBHOOK_ID;" | xargs)

if [ "$WEBHOOK_CHECK" = "1" ]; then
    echo -e "${GREEN}✓ Webhook found in database${NC}"
else
    echo -e "${RED}✗ Webhook not found in database${NC}"
    exit 1
fi

echo ""

# Step 2: Trigger - Create Agreement
echo -e "${YELLOW}Step 2: Trigger - Creating Agreement via API${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Generate unique agreement data
AGREEMENT_NUMBER="AGR-TEST-$(date +%s)"
POLICY_NUMBER="POL-TEST-$(date +%s)"

echo "Creating agreement:"
echo "  Agreement Number: $AGREEMENT_NUMBER"
echo "  Policy Number: $POLICY_NUMBER"
echo ""

# Create agreement via API
AGREEMENT_PAYLOAD='{
  "agreementNumber": "'"$AGREEMENT_NUMBER"'",
  "agreementType": "motor_comprehensive",
  "marketContext": "RETAIL",
  "productType": "MOTOR_COMPREHENSIVE",
  "effectiveDate": "2026-02-12T00:00:00Z",
  "expiryDate": "2027-02-12T00:00:00Z",
  "policyNumber": "'"$POLICY_NUMBER"'",
  "premium": 1250.00,
  "attributes": {
    "vehicleMake": "Toyota",
    "vehicleModel": "Camry",
    "vehicleYear": 2024,
    "sumInsured": 50000.00
  }
}'

echo "Sending POST request to $BEEMA_KERNEL_URL/api/v1/agreements..."
AGREEMENT_RESPONSE=$(curl -s -X POST "$BEEMA_KERNEL_URL/api/v1/agreements" \
    -H "Content-Type: application/json" \
    -H "X-Tenant-ID: default" \
    -d "$AGREEMENT_PAYLOAD")

# Check if agreement was created
AGREEMENT_ID=$(echo "$AGREEMENT_RESPONSE" | grep -o '"id":"[^"]*"' | cut -d'"' -f4)

if [ -n "$AGREEMENT_ID" ]; then
    echo -e "${GREEN}✓ Agreement created with ID: $AGREEMENT_ID${NC}"
else
    echo -e "${RED}✗ Failed to create agreement${NC}"
    echo "Response: $AGREEMENT_RESPONSE"

    # Try alternative endpoint for testing
    echo ""
    echo "Trying test endpoint instead..."
    TEST_RESPONSE=$(curl -s -X POST "$BEEMA_KERNEL_URL/api/v1/events/test/policy-bound")
    echo "Test event published: $TEST_RESPONSE"
fi

echo ""

# Wait for event to propagate
echo "Waiting for event to propagate through Inngest (5 seconds)..."
sleep 5
echo ""

# Step 3: Assert - Check Inngest Dashboard
echo -e "${YELLOW}Step 3: Assert - Verifying Webhook Delivery${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Check for recent function runs
echo "Querying Inngest for recent function runs..."

# Try to get events from Inngest (API may vary based on version)
INNGEST_CHECK=$(curl -s "$INNGEST_URL/v1/events?limit=10" || echo "{}")

echo "Recent events in Inngest:"
echo "$INNGEST_CHECK" | jq -r '.[] | "\(.name) - \(.ts)"' 2>/dev/null || echo "Unable to query events API"

echo ""

# Check database for webhook deliveries
echo "Checking sys_webhook_deliveries table..."
DELIVERY_CHECK=$(docker exec beema-postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -t -c \
    "SELECT
        delivery_id,
        event_type,
        status,
        status_code,
        delivered_at
     FROM sys_webhook_deliveries
     WHERE webhook_id = $WEBHOOK_ID
     ORDER BY delivered_at DESC
     LIMIT 5;" 2>/dev/null || echo "")

if [ -n "$DELIVERY_CHECK" ]; then
    echo -e "${GREEN}✓ Found webhook deliveries:${NC}"
    echo "$DELIVERY_CHECK"

    # Check for successful delivery
    SUCCESS_COUNT=$(echo "$DELIVERY_CHECK" | grep -c "success" || true)
    if [ "$SUCCESS_COUNT" -gt 0 ]; then
        echo -e "${GREEN}✓ Found $SUCCESS_COUNT successful delivery/deliveries${NC}"
    else
        echo -e "${YELLOW}⚠ No successful deliveries found yet${NC}"
    fi
else
    echo -e "${YELLOW}⚠ No deliveries found yet (table may not exist or webhook hasn't fired)${NC}"
fi

echo ""

# Additional verification via Studio API (if running)
if curl -sf "$STUDIO_URL" > /dev/null 2>&1; then
    echo "Checking Studio API for deliveries..."
    STUDIO_DELIVERIES=$(curl -s "$STUDIO_URL/api/webhooks/deliveries?limit=5" || echo "{}")
    echo "$STUDIO_DELIVERIES" | jq '.' 2>/dev/null || echo "Unable to query Studio API"
    echo ""
fi

# Summary
echo -e "${BLUE}╔═══════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   Verification Summary                               ║${NC}"
echo -e "${BLUE}╚═══════════════════════════════════════════════════════╝${NC}"
echo ""

echo "1. Webhook Created:"
echo "   ID: $WEBHOOK_ID"
echo "   Name: $WEBHOOK_NAME"
echo "   Event: agreement/created"
echo "   URL: https://httpbin.org/post"
echo ""

if [ -n "$AGREEMENT_ID" ]; then
    echo "2. Agreement Created:"
    echo "   ID: $AGREEMENT_ID"
    echo "   Number: $AGREEMENT_NUMBER"
    echo ""
fi

echo "3. Manual Verification Steps:"
echo ""
echo -e "   ${YELLOW}a) Open Inngest Dev UI:${NC}"
echo "      http://localhost:8288"
echo "      → Navigate to 'Events' tab"
echo "      → Look for 'agreement/created' event"
echo ""
echo -e "   ${YELLOW}b) Check Function Runs:${NC}"
echo "      http://localhost:8288/functions"
echo "      → Look for 'webhook-dispatcher' function"
echo "      → Check execution logs"
echo ""
echo -e "   ${YELLOW}c) Verify httpbin.org received webhook:${NC}"
echo "      → Check function logs for HTTP 200 status"
echo "      → Look for response from httpbin.org"
echo ""

# Cleanup option
echo ""
read -p "Do you want to delete the test webhook? (y/n) " -n 1 -r
echo ""
if [[ $REPLY =~ ^[Yy]$ ]]; then
    docker exec beema-postgres psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c \
        "DELETE FROM sys_webhooks WHERE webhook_id = $WEBHOOK_ID;"
    echo -e "${GREEN}✓ Test webhook deleted${NC}"
fi

echo ""
echo -e "${GREEN}Verification script complete!${NC}"
