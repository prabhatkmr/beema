#!/bin/bash

# Test script for Dynamic OpenAPI Generator
# This script tests the dynamic OpenAPI endpoint and validates the generated spec

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
BEEMA_KERNEL_URL="${BEEMA_KERNEL_URL:-http://localhost:8080}"
OPENAPI_ENDPOINT="$BEEMA_KERNEL_URL/api/v1/docs/openapi.json"

echo -e "${BLUE}==================================================${NC}"
echo -e "${BLUE}  Beema Dynamic OpenAPI Generator - Test Suite  ${NC}"
echo -e "${BLUE}==================================================${NC}"
echo ""

# Step 1: Check if beema-kernel is running
echo -e "${YELLOW}[1/5]${NC} Checking if beema-kernel is running..."
if ! curl -sf "$BEEMA_KERNEL_URL/actuator/health" > /dev/null 2>&1; then
    echo -e "${RED}❌ beema-kernel is not running at $BEEMA_KERNEL_URL${NC}"
    echo ""
    echo "Start beema-kernel with:"
    echo "  cd apps/beema-kernel"
    echo "  mvn spring-boot:run"
    echo ""
    echo "Or use Docker:"
    echo "  docker-compose up -d beema-kernel"
    exit 1
fi
echo -e "${GREEN}✅ beema-kernel is running${NC}"
echo ""

# Step 2: Fetch dynamic OpenAPI spec
echo -e "${YELLOW}[2/5]${NC} Fetching dynamic OpenAPI specification..."
RESPONSE=$(curl -sf "$OPENAPI_ENDPOINT" -w "\n%{http_code}" 2>&1)
HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

if [ "$HTTP_CODE" != "200" ]; then
    echo -e "${RED}❌ Failed to fetch OpenAPI spec (HTTP $HTTP_CODE)${NC}"
    echo "Response: $BODY"
    exit 1
fi
echo -e "${GREEN}✅ OpenAPI spec retrieved successfully${NC}"
echo ""

# Save to file
OPENAPI_FILE="/tmp/beema-dynamic-openapi.json"
echo "$BODY" > "$OPENAPI_FILE"
echo "Saved to: $OPENAPI_FILE"
echo ""

# Step 3: Validate OpenAPI structure
echo -e "${YELLOW}[3/5]${NC} Validating OpenAPI structure..."

# Check for required fields
VALIDATION_ERRORS=0

# Check openapi version
OPENAPI_VERSION=$(echo "$BODY" | grep -o '"openapi":"[^"]*"' | head -1 | cut -d'"' -f4 || echo "")
if [ -z "$OPENAPI_VERSION" ]; then
    echo -e "${RED}❌ Missing 'openapi' version field${NC}"
    VALIDATION_ERRORS=$((VALIDATION_ERRORS + 1))
else
    echo -e "${GREEN}✅ OpenAPI version: $OPENAPI_VERSION${NC}"
fi

# Check info section
if echo "$BODY" | grep -q '"info"'; then
    echo -e "${GREEN}✅ Info section present${NC}"

    # Check title
    TITLE=$(echo "$BODY" | grep -o '"title":"[^"]*"' | head -1 | cut -d'"' -f4)
    echo "   Title: $TITLE"

    # Check version
    VERSION=$(echo "$BODY" | grep -o '"version":"[^"]*"' | head -1 | cut -d'"' -f4)
    echo "   Version: $VERSION"
else
    echo -e "${RED}❌ Missing 'info' section${NC}"
    VALIDATION_ERRORS=$((VALIDATION_ERRORS + 1))
fi

# Check paths section
if echo "$BODY" | grep -q '"paths"'; then
    echo -e "${GREEN}✅ Paths section present${NC}"

    # Count paths
    PATH_COUNT=$(echo "$BODY" | grep -o '"/api/v1/data/[^"]*"' | wc -l)
    echo "   Found $PATH_COUNT dynamic paths"
else
    echo -e "${RED}❌ Missing 'paths' section${NC}"
    VALIDATION_ERRORS=$((VALIDATION_ERRORS + 1))
fi

# Check servers section
if echo "$BODY" | grep -q '"servers"'; then
    echo -e "${GREEN}✅ Servers section present${NC}"
else
    echo -e "${YELLOW}⚠️  Missing 'servers' section (optional)${NC}"
fi

echo ""

# Step 4: Test filtering by market context
echo -e "${YELLOW}[4/5]${NC} Testing market context filtering..."

RETAIL_RESPONSE=$(curl -sf "$OPENAPI_ENDPOINT?marketContext=RETAIL" 2>&1)
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Filtered by RETAIL market context${NC}"
    RETAIL_PATHS=$(echo "$RETAIL_RESPONSE" | grep -o '"/api/v1/data/[^"]*"' | wc -l)
    echo "   Found $RETAIL_PATHS paths for RETAIL"
else
    echo -e "${YELLOW}⚠️  Market context filtering failed (may not have RETAIL objects)${NC}"
fi

COMMERCIAL_RESPONSE=$(curl -sf "$OPENAPI_ENDPOINT?marketContext=COMMERCIAL" 2>&1)
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✅ Filtered by COMMERCIAL market context${NC}"
    COMMERCIAL_PATHS=$(echo "$COMMERCIAL_RESPONSE" | grep -o '"/api/v1/data/[^"]*"' | wc -l)
    echo "   Found $COMMERCIAL_PATHS paths for COMMERCIAL"
else
    echo -e "${YELLOW}⚠️  Market context filtering failed (may not have COMMERCIAL objects)${NC}"
fi

echo ""

# Step 5: Validate sample path details
echo -e "${YELLOW}[5/5]${NC} Validating sample path details..."

# Extract first path
FIRST_PATH=$(echo "$BODY" | grep -o '"/api/v1/data/[^"]*"' | head -1 | tr -d '"')

if [ -n "$FIRST_PATH" ]; then
    echo "Checking path: $FIRST_PATH"

    # Check for POST operation
    if echo "$BODY" | grep -A 50 "$FIRST_PATH" | grep -q '"post"'; then
        echo -e "${GREEN}✅ POST operation defined${NC}"
    else
        echo -e "${RED}❌ Missing POST operation${NC}"
        VALIDATION_ERRORS=$((VALIDATION_ERRORS + 1))
    fi

    # Check for GET operation
    if echo "$BODY" | grep -A 50 "$FIRST_PATH" | grep -q '"get"'; then
        echo -e "${GREEN}✅ GET operation defined${NC}"
    else
        echo -e "${RED}❌ Missing GET operation${NC}"
        VALIDATION_ERRORS=$((VALIDATION_ERRORS + 1))
    fi

    # Check for schemas
    if echo "$BODY" | grep -q '"schema"'; then
        echo -e "${GREEN}✅ Schemas defined${NC}"
    else
        echo -e "${RED}❌ Missing schemas${NC}"
        VALIDATION_ERRORS=$((VALIDATION_ERRORS + 1))
    fi
else
    echo -e "${YELLOW}⚠️  No paths found (metadata may be empty)${NC}"
fi

echo ""

# Summary
echo -e "${BLUE}==================================================${NC}"
echo -e "${BLUE}                   Test Summary                   ${NC}"
echo -e "${BLUE}==================================================${NC}"
echo ""

if [ $VALIDATION_ERRORS -eq 0 ]; then
    echo -e "${GREEN}✅ All tests passed!${NC}"
    echo ""
    echo "📄 Generated OpenAPI spec saved to:"
    echo "   $OPENAPI_FILE"
    echo ""
    echo "🔗 View in browser:"
    echo "   $OPENAPI_ENDPOINT"
    echo ""
    echo "📊 Import to Swagger UI:"
    echo "   http://localhost:8080/swagger-ui/index.html?url=/api/v1/docs/openapi.json"
    echo ""
    echo "📮 Import to Postman:"
    echo "   1. Open Postman"
    echo "   2. Click 'Import'"
    echo "   3. Select 'Link'"
    echo "   4. Paste: $OPENAPI_ENDPOINT"
    echo ""
    echo "🧪 Test with curl:"
    echo "   curl $OPENAPI_ENDPOINT | jq ."
    echo ""
    exit 0
else
    echo -e "${RED}❌ $VALIDATION_ERRORS validation errors found${NC}"
    echo ""
    echo "Check the output above for details."
    echo ""
    exit 1
fi
