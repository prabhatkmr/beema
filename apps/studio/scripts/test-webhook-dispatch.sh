#!/bin/bash

echo "Testing Webhook Dispatcher"
echo "=========================="

# 1. Test event publishing from beema-kernel
echo "\n1. Publishing test event from beema-kernel..."
curl -X POST http://localhost:8080/api/v1/events/test/policy-bound

sleep 2

# 2. Check Inngest Dev Server
echo "\n2. Check Inngest Dev Server: http://localhost:8288"
echo "   You should see the event in the Inngest dashboard"

# 3. Verify webhook delivery
echo "\n3. Check webhook delivery logs in Studio API"
curl -X GET http://localhost:3000/api/webhooks/deliveries

echo "\n\nDone! Check Inngest Dev Server for function runs."
