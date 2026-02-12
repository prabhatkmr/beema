#!/bin/bash
# Test script for beema-message-processor
# Tests the complete message transformation pipeline

set -e

echo "=== Beema Message Processor - Pipeline Test ==="
echo ""

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Check if services are running
echo -e "${YELLOW}1. Checking services...${NC}"
if ! docker ps | grep -q beema-kafka; then
    echo -e "${RED}Error: Kafka is not running. Start with: docker-compose up -d kafka${NC}"
    exit 1
fi

if ! docker ps | grep -q beema-postgres; then
    echo -e "${RED}Error: PostgreSQL is not running. Start with: docker-compose up -d postgres${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Services are running${NC}"
echo ""

# Test database connection
echo -e "${YELLOW}2. Testing database connection...${NC}"
if docker exec beema-postgres psql -U beema -d beema_kernel -c "SELECT COUNT(*) FROM sys_message_hooks;" > /dev/null 2>&1; then
    HOOK_COUNT=$(docker exec beema-postgres psql -U beema -d beema_kernel -t -c "SELECT COUNT(*) FROM sys_message_hooks;")
    echo -e "${GREEN}✓ Database connected. Found $HOOK_COUNT hooks.${NC}"
else
    echo -e "${RED}Error: Cannot connect to database. Run migration first: mvn flyway:migrate${NC}"
    exit 1
fi
echo ""

# Send test messages
echo -e "${YELLOW}3. Sending test messages to Kafka...${NC}"

# Test 1: Policy Created
echo -e "   ${YELLOW}→ Sending policy_created message...${NC}"
echo '{"messageId":"test-001","messageType":"policy_created","sourceSystem":"legacy_system","payload":{"policyRef":"pol-12345","customer":{"firstName":"John","lastName":"Doe"},"policy":{"premium":1000.00,"currency":"GBP"}},"timestamp":"2026-02-12T10:30:00Z"}' | \
    docker exec -i beema-kafka kafka-console-producer \
        --bootstrap-server localhost:9092 \
        --topic raw-messages > /dev/null 2>&1
echo -e "   ${GREEN}✓ Sent policy_created${NC}"

# Test 2: Claim Submitted
echo -e "   ${YELLOW}→ Sending claim_submitted message...${NC}"
echo '{"messageId":"test-002","messageType":"claim_submitted","sourceSystem":"partner_api","payload":{"claimId":"CLM-98765","policyNumber":"POL-12345","claim":{"amount":5000.00,"submittedDate":"2026-02-10","type":"property_damage"},"claimant":{"name":"John Doe"}},"timestamp":"2026-02-10T14:22:00Z"}' | \
    docker exec -i beema-kafka kafka-console-producer \
        --bootstrap-server localhost:9092 \
        --topic raw-messages > /dev/null 2>&1
echo -e "   ${GREEN}✓ Sent claim_submitted${NC}"

# Test 3: London Market Slip
echo -e "   ${YELLOW}→ Sending slip_created message...${NC}"
echo '{"messageId":"test-003","messageType":"slip_created","sourceSystem":"london_market","payload":{"slipNumber":"SLIP-2026-00123","umr":"B1234XYZ2026001","slip":{"leadUnderwriter":"Lloyds Syndicate 1234","totalPremium":500000.00,"totalLine":100,"currency":"GBP"},"broker":{"name":"ABC Brokers Ltd"}},"timestamp":"2026-02-11T09:15:00Z"}' | \
    docker exec -i beema-kafka kafka-console-producer \
        --bootstrap-server localhost:9092 \
        --topic raw-messages > /dev/null 2>&1
echo -e "   ${GREEN}✓ Sent slip_created${NC}"

echo ""

# Wait for processing
echo -e "${YELLOW}4. Waiting for messages to be processed (5 seconds)...${NC}"
sleep 5
echo -e "${GREEN}✓ Processing complete${NC}"
echo ""

# Consume and verify
echo -e "${YELLOW}5. Checking processed messages...${NC}"
PROCESSED_COUNT=$(docker exec beema-kafka kafka-console-consumer \
    --bootstrap-server localhost:9092 \
    --topic processed-messages \
    --from-beginning \
    --timeout-ms 3000 2>/dev/null | wc -l || echo "0")

if [ "$PROCESSED_COUNT" -ge 3 ]; then
    echo -e "${GREEN}✓ Found $PROCESSED_COUNT processed messages${NC}"
else
    echo -e "${RED}⚠ Warning: Expected at least 3 messages, found $PROCESSED_COUNT${NC}"
fi
echo ""

# Show sample output
echo -e "${YELLOW}6. Sample processed message:${NC}"
docker exec beema-kafka kafka-console-consumer \
    --bootstrap-server localhost:9092 \
    --topic processed-messages \
    --from-beginning \
    --max-messages 1 \
    --timeout-ms 3000 2>/dev/null | jq '.' || echo "No messages found or jq not installed"

echo ""
echo -e "${GREEN}=== Pipeline Test Complete ===${NC}"
echo ""
echo "To view all processed messages:"
echo "  docker exec -it beema-kafka kafka-console-consumer \\"
echo "    --bootstrap-server localhost:9092 \\"
echo "    --topic processed-messages \\"
echo "    --from-beginning"
echo ""
