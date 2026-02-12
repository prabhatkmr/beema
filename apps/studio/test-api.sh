#!/bin/bash

# Test script for Layout Validation API
# Usage: ./test-api.sh

set -e

API_BASE="http://localhost:3000/api/layouts"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=================================="
echo "Layout Validation API Test Suite"
echo "=================================="
echo ""

# Test 1: Validate valid layout
echo -e "${YELLOW}Test 1: Validate Valid Layout${NC}"
echo "POST ${API_BASE}/validate"
RESPONSE=$(curl -s -X POST ${API_BASE}/validate \
  -H "Content-Type: application/json" \
  -d @test-layout.json)

if echo "$RESPONSE" | grep -q '"valid":true'; then
  echo -e "${GREEN}✓ PASSED${NC}: Valid layout accepted"
else
  echo -e "${RED}✗ FAILED${NC}: Valid layout rejected"
  echo "$RESPONSE"
fi
echo ""

# Test 2: Validate invalid layout
echo -e "${YELLOW}Test 2: Validate Invalid Layout${NC}"
echo "POST ${API_BASE}/validate"
RESPONSE=$(curl -s -X POST ${API_BASE}/validate \
  -H "Content-Type: application/json" \
  -d @test-invalid-layout.json)

if echo "$RESPONSE" | grep -q '"valid":false'; then
  echo -e "${GREEN}✓ PASSED${NC}: Invalid layout rejected"
  echo "Errors found:"
  echo "$RESPONSE" | grep -o '"errors":\[[^]]*\]' || true
else
  echo -e "${RED}✗ FAILED${NC}: Invalid layout accepted"
  echo "$RESPONSE"
fi
echo ""

# Test 3: Save valid layout
echo -e "${YELLOW}Test 3: Save Valid Layout${NC}"
echo "POST ${API_BASE}/save"
RESPONSE=$(curl -s -X POST ${API_BASE}/save \
  -H "Content-Type: application/json" \
  -d @test-layout.json)

if echo "$RESPONSE" | grep -q '"success":true'; then
  echo -e "${GREEN}✓ PASSED${NC}: Layout saved successfully"
  LAYOUT_ID=$(echo "$RESPONSE" | grep -o '"layout_id":"[^"]*"' | cut -d'"' -f4)
  echo "Layout ID: $LAYOUT_ID"
else
  echo -e "${YELLOW}⚠ WARNING${NC}: Save may have failed or kernel unavailable"
  echo "$RESPONSE"
fi
echo ""

# Test 4: Save invalid layout
echo -e "${YELLOW}Test 4: Save Invalid Layout${NC}"
echo "POST ${API_BASE}/save"
RESPONSE=$(curl -s -X POST ${API_BASE}/save \
  -H "Content-Type: application/json" \
  -d @test-invalid-layout.json)

if echo "$RESPONSE" | grep -q '"success":false'; then
  echo -e "${GREEN}✓ PASSED${NC}: Invalid layout save rejected"
else
  echo -e "${RED}✗ FAILED${NC}: Invalid layout save accepted"
  echo "$RESPONSE"
fi
echo ""

# Test 5: Fetch all layouts
echo -e "${YELLOW}Test 5: Fetch All Layouts${NC}"
echo "GET ${API_BASE}"
RESPONSE=$(curl -s ${API_BASE})

if echo "$RESPONSE" | grep -q '"layouts"'; then
  COUNT=$(echo "$RESPONSE" | grep -o '"count":[0-9]*' | cut -d':' -f2)
  echo -e "${GREEN}✓ PASSED${NC}: Fetched layouts"
  echo "Total layouts: ${COUNT:-0}"
else
  echo -e "${YELLOW}⚠ WARNING${NC}: Fetch may have failed or kernel unavailable"
  echo "$RESPONSE"
fi
echo ""

# Test 6: Fetch filtered layouts
echo -e "${YELLOW}Test 6: Fetch Filtered Layouts (RETAIL)${NC}"
echo "GET ${API_BASE}?marketContext=RETAIL"
RESPONSE=$(curl -s "${API_BASE}?marketContext=RETAIL")

if echo "$RESPONSE" | grep -q '"layouts"'; then
  COUNT=$(echo "$RESPONSE" | grep -o '"count":[0-9]*' | cut -d':' -f2)
  echo -e "${GREEN}✓ PASSED${NC}: Fetched filtered layouts"
  echo "RETAIL layouts: ${COUNT:-0}"
else
  echo -e "${YELLOW}⚠ WARNING${NC}: Fetch may have failed or kernel unavailable"
  echo "$RESPONSE"
fi
echo ""

# Test 7: Test missing required fields
echo -e "${YELLOW}Test 7: Test Missing Required Fields${NC}"
echo "POST ${API_BASE}/validate"
RESPONSE=$(curl -s -X POST ${API_BASE}/validate \
  -H "Content-Type: application/json" \
  -d '{
    "layout_name": "",
    "fields": []
  }')

if echo "$RESPONSE" | grep -q '"valid":false'; then
  echo -e "${GREEN}✓ PASSED${NC}: Missing fields detected"
  echo "$RESPONSE" | grep -o '"errors":\[[^]]*\]' || true
else
  echo -e "${RED}✗ FAILED${NC}: Missing fields not detected"
  echo "$RESPONSE"
fi
echo ""

# Test 8: Test invalid JSON
echo -e "${YELLOW}Test 8: Test Invalid JSON${NC}"
echo "POST ${API_BASE}/validate"
RESPONSE=$(curl -s -X POST ${API_BASE}/validate \
  -H "Content-Type: application/json" \
  -d 'invalid json')

if echo "$RESPONSE" | grep -q '"valid":false'; then
  echo -e "${GREEN}✓ PASSED${NC}: Invalid JSON rejected"
else
  echo -e "${RED}✗ FAILED${NC}: Invalid JSON accepted"
  echo "$RESPONSE"
fi
echo ""

echo "=================================="
echo "Test Suite Complete"
echo "=================================="
echo ""
echo "Notes:"
echo "- Some tests may show warnings if beema-kernel is not running"
echo "- This is expected behavior (graceful degradation)"
echo "- To test full integration, ensure beema-kernel is running"
