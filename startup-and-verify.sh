#!/bin/bash

# Beema Platform Startup and Verification Script
# This script starts all services and verifies they are running correctly

set -e  # Exit on error

echo "🚀 Starting Beema Platform..."
echo ""

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Step 1: Check prerequisites
echo "📋 Checking prerequisites..."
command -v docker >/dev/null 2>&1 || { echo -e "${RED}❌ Docker is not installed${NC}"; exit 1; }
command -v docker-compose >/dev/null 2>&1 || { echo -e "${RED}❌ Docker Compose is not installed${NC}"; exit 1; }
echo -e "${GREEN}✅ Prerequisites met${NC}"
echo ""

# Step 2: Start infrastructure services first
echo "🔧 Starting infrastructure services (Postgres, Temporal, Kafka, Inngest)..."
docker-compose up -d postgres keycloak temporal temporal-ui zookeeper kafka inngest
echo ""

# Wait for infrastructure to be healthy
echo "⏳ Waiting for infrastructure services to be healthy (this may take 30-60 seconds)..."
timeout=120
elapsed=0
while [ $elapsed -lt $timeout ]; do
    if docker-compose ps | grep -q "unhealthy"; then
        echo -n "."
        sleep 5
        elapsed=$((elapsed + 5))
    else
        break
    fi
done
echo ""
echo -e "${GREEN}✅ Infrastructure services are ready${NC}"
echo ""

# Step 3: Start application services
echo "🚀 Starting application services (beema-kernel, studio, metadata-service, message-processor)..."
docker-compose up -d beema-kernel metadata-service beema-message-processor studio
echo ""

# Wait for application services
echo "⏳ Waiting for application services to start (this may take 30-60 seconds)..."
sleep 30
echo ""

# Step 4: Verify services
echo "🔍 Verifying services..."
echo ""

services=(
    "postgres:5433:PostgreSQL:nc -z localhost 5433"
    "keycloak:8180:Keycloak:curl -sf http://localhost:8180/health/ready > /dev/null"
    "temporal:8088:Temporal UI:curl -sf http://localhost:8088/ > /dev/null"
    "inngest:8288:Inngest:curl -sf http://localhost:8288/health > /dev/null"
    "beema-kernel:8080:Beema Kernel API:curl -sf http://localhost:8080/actuator/health > /dev/null"
    "metadata-service:8081:Metadata Service:curl -sf http://localhost:8081/actuator/health > /dev/null"
    "studio:3000:Studio UI:curl -sf http://localhost:3000/ > /dev/null"
)

all_healthy=true
for service_info in "${services[@]}"; do
    IFS=':' read -r name port description check_cmd <<< "$service_info"

    if eval "$check_cmd" 2>/dev/null; then
        echo -e "${GREEN}✅ $description${NC} - http://localhost:$port"
    else
        echo -e "${RED}❌ $description${NC} - http://localhost:$port (not responding)"
        all_healthy=false
    fi
done

echo ""

if [ "$all_healthy" = true ]; then
    echo -e "${GREEN}🎉 All services are running successfully!${NC}"
    echo ""
    echo "📊 Service URLs:"
    echo "   • Studio UI:           http://localhost:3000"
    echo "   • Beema Kernel API:    http://localhost:8080/swagger-ui/index.html"
    echo "   • Temporal UI:         http://localhost:8088"
    echo "   • Inngest Dashboard:   http://localhost:8288"
    echo "   • Keycloak Admin:      http://localhost:8180 (admin/admin)"
    echo "   • Metadata Service:    http://localhost:8081/swagger-ui/index.html"
    echo ""
    echo "🧪 Quick Tests:"
    echo "   • Layout API:          cd apps/beema-kernel && ./test-layout-api.sh"
    echo "   • OpenRouter:          cd apps/beema-kernel && ./test-openrouter.sh"
    echo "   • Webhook E2E:         ./scripts/verify-webhooks-e2e.sh"
    echo ""
    echo "📝 View Logs:"
    echo "   • All services:        docker-compose logs -f"
    echo "   • Specific service:    docker-compose logs -f beema-kernel"
    echo ""
    echo "🛑 Stop Services:"
    echo "   • All:                 docker-compose down"
    echo "   • With data cleanup:   docker-compose down -v"
    echo ""
else
    echo -e "${YELLOW}⚠️  Some services are not responding yet.${NC}"
    echo ""
    echo "🔍 Check service status:"
    echo "   docker-compose ps"
    echo ""
    echo "📋 View logs for failing services:"
    echo "   docker-compose logs [service-name]"
    echo ""
    exit 1
fi

# Step 5: Optional - Run quick health check on beema-kernel
echo "🏥 Running health check on beema-kernel..."
if curl -sf http://localhost:8080/actuator/health | grep -q '"status":"UP"'; then
    echo -e "${GREEN}✅ Beema Kernel is healthy${NC}"

    # Check components
    echo ""
    echo "🔧 Component Status:"
    components=$(curl -sf http://localhost:8080/actuator/health | grep -oP '"[^"]+":"UP"' | head -5)
    echo "$components" | while read -r line; do
        echo "   • $line"
    done
else
    echo -e "${YELLOW}⚠️  Health check inconclusive, but service is running${NC}"
fi

echo ""
echo -e "${GREEN}✨ Beema Platform is ready for development!${NC}"
