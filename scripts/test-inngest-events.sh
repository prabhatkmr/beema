#!/bin/bash

echo "Testing Inngest Event Flow"
echo "=========================="

BASE_URL="http://localhost:8080"
INNGEST_URL="http://localhost:8288"

# 1. Publish Policy Bound Event
echo "\n1️⃣  Publishing Policy Bound Event..."
curl -X POST $BASE_URL/api/v1/events/test/policy-bound
sleep 2

# 2. Publish Claim Opened Event
echo "\n2️⃣  Publishing Claim Opened Event..."
curl -X POST $BASE_URL/api/v1/events/test/claim-opened
sleep 2

# 3. Check Inngest Dev Server
echo "\n3️⃣  Checking Inngest Dev Server..."
curl -s $INNGEST_URL/health | jq .

# 4. View recent events (if API available)
echo "\n4️⃣  Recent Events:"
echo "   Visit: http://localhost:8288"
echo "   You should see events in the Inngest dashboard"

# 5. Check webhook deliveries
echo "\n5️⃣  Checking webhook deliveries..."
curl -s http://localhost:3000/api/webhooks/deliveries | jq .

echo "\n✅ Test complete!"
echo "   - View events: http://localhost:8288"
echo "   - View function runs: http://localhost:8288/functions"
