#!/bin/bash

# Beema Observability Stack Verification Script

set -e

echo "🔍 Verifying Beema Observability Stack..."
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to check service health
check_service() {
    local service_name=$1
    local url=$2
    local expected_code=${3:-200}

    echo -n "Checking $service_name... "

    if response=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null); then
        if [ "$response" -eq "$expected_code" ]; then
            echo -e "${GREEN}✓ UP${NC} (HTTP $response)"
            return 0
        else
            echo -e "${YELLOW}⚠ Unexpected response${NC} (HTTP $response)"
            return 1
        fi
    else
        echo -e "${RED}✗ DOWN${NC}"
        return 1
    fi
}

# Function to check Docker container
check_container() {
    local container_name=$1

    echo -n "Checking container $container_name... "

    if docker ps --filter "name=$container_name" --filter "status=running" | grep -q "$container_name"; then
        health=$(docker inspect --format='{{.State.Health.Status}}' "$container_name" 2>/dev/null || echo "no-healthcheck")
        if [ "$health" = "healthy" ] || [ "$health" = "no-healthcheck" ]; then
            echo -e "${GREEN}✓ Running${NC} (Health: $health)"
            return 0
        else
            echo -e "${YELLOW}⚠ Unhealthy${NC} (Health: $health)"
            return 1
        fi
    else
        echo -e "${RED}✗ Not running${NC}"
        return 1
    fi
}

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "1. Docker Containers"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

check_container "beema-jaeger"
check_container "beema-prometheus"
check_container "beema-grafana"
check_container "beema-kernel"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "2. Service Health Checks"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

check_service "Jaeger UI" "http://localhost:16686"
check_service "Prometheus" "http://localhost:9090/-/healthy"
check_service "Grafana" "http://localhost:3001/api/health"
check_service "Beema Kernel Health" "http://localhost:8080/actuator/health"
check_service "Beema Kernel Metrics" "http://localhost:8080/actuator/prometheus"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "3. OpenTelemetry Configuration"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

echo -n "Checking OTLP endpoint (Jaeger)... "
if curl -s -o /dev/null -w "%{http_code}" "http://localhost:4318" | grep -q "405"; then
    echo -e "${GREEN}✓ OTLP HTTP endpoint accessible${NC}"
else
    echo -e "${YELLOW}⚠ OTLP endpoint may not be ready${NC}"
fi

echo -n "Checking Jaeger OTLP receiver... "
if nc -z localhost 4317 2>/dev/null; then
    echo -e "${GREEN}✓ OTLP gRPC endpoint accessible${NC}"
else
    echo -e "${YELLOW}⚠ OTLP gRPC endpoint not accessible${NC}"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "4. Prometheus Targets"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

if targets=$(curl -s "http://localhost:9090/api/v1/targets" 2>/dev/null); then
    active_targets=$(echo "$targets" | grep -o '"health":"up"' | wc -l | tr -d ' ')
    total_targets=$(echo "$targets" | grep -o '"health":' | wc -l | tr -d ' ')
    echo "Active targets: $active_targets / $total_targets"

    if [ "$active_targets" -gt 0 ]; then
        echo -e "${GREEN}✓ Prometheus scraping targets${NC}"
    else
        echo -e "${YELLOW}⚠ No active Prometheus targets${NC}"
    fi
else
    echo -e "${RED}✗ Could not fetch Prometheus targets${NC}"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "5. Generate Test Traces"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

echo "Generating sample traces..."
for i in {1..3}; do
    echo -n "  Request $i... "
    if curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080/actuator/health" | grep -q "200"; then
        echo -e "${GREEN}✓${NC}"
    else
        echo -e "${RED}✗${NC}"
    fi
    sleep 0.5
done

echo ""
echo "Waiting for traces to propagate..."
sleep 2

echo -n "Checking for traces in Jaeger... "
if traces=$(curl -s "http://localhost:16686/api/traces?service=beema-kernel&limit=5" 2>/dev/null); then
    trace_count=$(echo "$traces" | grep -o '"traceID"' | wc -l | tr -d ' ')
    if [ "$trace_count" -gt 0 ]; then
        echo -e "${GREEN}✓ Found $trace_count traces${NC}"
    else
        echo -e "${YELLOW}⚠ No traces found yet (may need more time)${NC}"
    fi
else
    echo -e "${RED}✗ Could not fetch traces${NC}"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "6. Access URLs"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

echo ""
echo "📊 Grafana:     http://localhost:3001 (admin/admin)"
echo "🔍 Jaeger UI:   http://localhost:16686"
echo "📈 Prometheus:  http://localhost:9090"
echo "🚀 Kernel API:  http://localhost:8080/swagger-ui"
echo ""

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Next Steps:"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "1. Open Grafana and import dashboard ID: 19004 (Spring Boot 3.x)"
echo "2. View traces in Jaeger for service: beema-kernel"
echo "3. Explore metrics in Prometheus: http_server_requests_seconds_count"
echo "4. Read the full guide: platform/observability/README.md"
echo ""
echo "✅ Verification complete!"
