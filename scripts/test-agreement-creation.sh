#!/bin/bash

# Test agreement creation with curl
# Usage: ./scripts/test-agreement-creation.sh

BEEMA_KERNEL_URL="${BEEMA_KERNEL_URL:-http://localhost:8080}"
TIMESTAMP=$(date +%s)
AGREEMENT_NUMBER="AGR-TEST-$TIMESTAMP"

echo "Creating test agreement..."
echo "Agreement Number: $AGREEMENT_NUMBER"
echo ""

curl -v -X POST "$BEEMA_KERNEL_URL/api/v1/agreements" \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: default" \
  -H "X-User-ID: test-user" \
  -H "X-User-Email: test@beema.io" \
  -d "{
    \"agreementNumber\": \"$AGREEMENT_NUMBER\",
    \"agreementType\": \"motor_comprehensive\",
    \"marketContext\": \"RETAIL\",
    \"productType\": \"MOTOR_COMPREHENSIVE\",
    \"effectiveDate\": \"2026-02-12T00:00:00Z\",
    \"expiryDate\": \"2027-02-12T00:00:00Z\",
    \"policyNumber\": \"POL-TEST-$TIMESTAMP\",
    \"premium\": 1250.00,
    \"attributes\": {
      \"vehicleMake\": \"Toyota\",
      \"vehicleModel\": \"Camry\",
      \"vehicleYear\": 2024,
      \"sumInsured\": 50000.00,
      \"driverAge\": 35,
      \"driverExperience\": 15
    }
  }" | jq .

echo ""
echo "Agreement creation request sent!"
echo ""
echo "Next steps:"
echo "1. Check Inngest UI: http://localhost:8288"
echo "2. Look for 'agreement/created' event"
echo "3. Check webhook-dispatcher function execution"
