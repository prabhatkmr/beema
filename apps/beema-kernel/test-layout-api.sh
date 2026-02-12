#!/bin/bash
# Layout API Test Script

BASE_URL="${BASE_URL:-http://localhost:8080}"

echo "================================"
echo "Layout Resolution API Test Script"
echo "================================"
echo ""

# Test 1: Get motor policy layout (default role)
echo "Test 1: Get motor policy layout (default role)"
echo "GET $BASE_URL/api/v1/layouts/policy/motor_comprehensive?marketContext=RETAIL"
curl -s -X GET "$BASE_URL/api/v1/layouts/policy/motor_comprehensive?marketContext=RETAIL" \
  -H "X-Tenant-ID: default" \
  -H "X-User-Role: user" \
  -H "Content-Type: application/json" | jq '.'
echo ""
echo "---"
echo ""

# Test 2: Get motor policy layout (underwriter role)
echo "Test 2: Get motor policy layout (underwriter role)"
echo "GET $BASE_URL/api/v1/layouts/policy/motor_comprehensive?marketContext=RETAIL"
curl -s -X GET "$BASE_URL/api/v1/layouts/policy/motor_comprehensive?marketContext=RETAIL" \
  -H "X-Tenant-ID: default" \
  -H "X-User-Role: underwriter" \
  -H "Content-Type: application/json" | jq '.'
echo ""
echo "---"
echo ""

# Test 3: Get all layouts
echo "Test 3: Get all layouts"
echo "GET $BASE_URL/api/v1/layouts/all"
curl -s -X GET "$BASE_URL/api/v1/layouts/all" \
  -H "Content-Type: application/json" | jq '.'
echo ""
echo "---"
echo ""

# Test 4: Get layouts filtered by context
echo "Test 4: Get layouts filtered by context (policy)"
echo "GET $BASE_URL/api/v1/layouts/all?context=policy"
curl -s -X GET "$BASE_URL/api/v1/layouts/all?context=policy" \
  -H "Content-Type: application/json" | jq '.'
echo ""
echo "---"
echo ""

# Test 5: Health check
echo "Test 5: Health check"
echo "GET $BASE_URL/api/v1/layouts/health"
curl -s -X GET "$BASE_URL/api/v1/layouts/health" \
  -H "Content-Type: application/json" | jq '.'
echo ""
echo "---"
echo ""

# Test 6: Get non-existent layout (should return default)
echo "Test 6: Get non-existent layout (should return default)"
echo "GET $BASE_URL/api/v1/layouts/unknown/unknown_type"
curl -s -X GET "$BASE_URL/api/v1/layouts/unknown/unknown_type" \
  -H "X-Tenant-ID: default" \
  -H "X-User-Role: user" \
  -H "Content-Type: application/json" | jq '.'
echo ""
echo "---"
echo ""

echo "================================"
echo "Test completed!"
echo "================================"
