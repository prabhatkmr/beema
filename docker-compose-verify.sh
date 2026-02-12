#!/bin/bash

# Beema Docker Compose Verification Script
# This script checks the health and connectivity of all services

set -e

echo "=========================================="
echo "Beema Platform - Service Verification"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to check if service is running
check_service() {
    local service_name=$1
    local container_name=$2

    if docker ps --format '{{.Names}}' | grep -q "^${container_name}$"; then
        echo -e "${GREEN}✓${NC} $service_name is running"
        return 0
    else
        echo -e "${RED}✗${NC} $service_name is NOT running"
        return 1
    fi
}

# Function to check service health
check_health() {
    local service_name=$1
    local container_name=$2

    local health=$(docker inspect --format='{{.State.Health.Status}}' "$container_name" 2>/dev/null || echo "no-healthcheck")

    if [ "$health" = "healthy" ]; then
        echo -e "${GREEN}✓${NC} $service_name is healthy"
        return 0
    elif [ "$health" = "no-healthcheck" ]; then
        echo -e "${YELLOW}○${NC} $service_name (no healthcheck)"
        return 0
    else
        echo -e "${RED}✗${NC} $service_name health: $health"
        return 1
    fi
}

# Function to check HTTP endpoint
check_http() {
    local service_name=$1
    local url=$2

    if curl -sf "$url" > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC} $service_name HTTP endpoint is accessible"
        return 0
    else
        echo -e "${RED}✗${NC} $service_name HTTP endpoint is NOT accessible"
        return 1
    fi
}

echo "1. Checking if services are running..."
echo "--------------------------------------"
check_service "PostgreSQL" "beema-postgres"
check_service "Keycloak" "beema-keycloak"
check_service "Metadata Service" "beema-metadata"
check_service "Temporal Server" "beema-temporal"
check_service "Temporal UI" "beema-temporal-ui"
check_service "Beema Kernel" "beema-kernel"
check_service "Zookeeper" "beema-zookeeper"
check_service "Kafka" "beema-kafka"
check_service "Message Processor" "beema-message-processor" || true
check_service "Studio" "beema-studio"
echo ""

echo "2. Checking service health..."
echo "--------------------------------------"
check_health "PostgreSQL" "beema-postgres"
check_health "Keycloak" "beema-keycloak"
check_health "Metadata Service" "beema-metadata"
check_health "Temporal Server" "beema-temporal"
check_health "Temporal UI" "beema-temporal-ui"
check_health "Beema Kernel" "beema-kernel"
check_health "Kafka" "beema-kafka"
check_health "Studio" "beema-studio"
echo ""

echo "3. Checking HTTP endpoints..."
echo "--------------------------------------"
check_http "Beema Kernel Health" "http://localhost:8080/actuator/health"
check_http "Beema Kernel Swagger" "http://localhost:8080/swagger-ui.html"
check_http "Temporal UI" "http://localhost:8088"
check_http "Metadata Service" "http://localhost:8082/actuator/health"
check_http "Keycloak" "http://localhost:8180"
check_http "Studio" "http://localhost:3000"
echo ""

echo "4. Checking database connectivity..."
echo "--------------------------------------"
if docker exec beema-postgres pg_isready -U beema > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} PostgreSQL is accepting connections"
else
    echo -e "${RED}✗${NC} PostgreSQL is NOT accepting connections"
fi

# Check if beema_kernel database exists
if docker exec beema-postgres psql -U beema -lqt | cut -d \| -f 1 | grep -qw beema_kernel; then
    echo -e "${GREEN}✓${NC} Database 'beema_kernel' exists"
else
    echo -e "${RED}✗${NC} Database 'beema_kernel' does NOT exist"
fi
echo ""

echo "5. Checking Temporal connectivity..."
echo "--------------------------------------"
if docker exec beema-kernel curl -sf http://temporal:7233 > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} Temporal server is reachable from beema-kernel"
else
    echo -e "${YELLOW}○${NC} Temporal server check (gRPC, HTTP check may not work)"
fi

# Check if worker is registered (by checking logs)
if docker logs beema-kernel 2>&1 | grep -q "Started Temporal worker"; then
    echo -e "${GREEN}✓${NC} Temporal worker appears to be started"
else
    echo -e "${YELLOW}○${NC} Could not confirm Temporal worker status from logs"
fi
echo ""

echo "6. Checking Kafka connectivity..."
echo "--------------------------------------"
if docker exec beema-kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} Kafka broker is accepting connections"
else
    echo -e "${RED}✗${NC} Kafka broker is NOT accepting connections"
fi

# Check if topics exist
if docker exec beema-kafka kafka-topics --bootstrap-server localhost:9092 --list 2>/dev/null | grep -q "raw-messages"; then
    echo -e "${GREEN}✓${NC} Topic 'raw-messages' exists"
else
    echo -e "${RED}✗${NC} Topic 'raw-messages' does NOT exist"
fi

if docker exec beema-kafka kafka-topics --bootstrap-server localhost:9092 --list 2>/dev/null | grep -q "processed-messages"; then
    echo -e "${GREEN}✓${NC} Topic 'processed-messages' exists"
else
    echo -e "${RED}✗${NC} Topic 'processed-messages' does NOT exist"
fi
echo ""

echo "=========================================="
echo "Service Access URLs:"
echo "=========================================="
echo "Beema Kernel API:       http://localhost:8080"
echo "Swagger UI:             http://localhost:8080/swagger-ui.html"
echo "Actuator Health:        http://localhost:8080/actuator/health"
echo "Temporal UI:            http://localhost:8088"
echo "Studio (Frontend):      http://localhost:3000"
echo "Keycloak Admin:         http://localhost:8180 (admin/admin)"
echo "Metadata Service:       http://localhost:8082"
echo "PostgreSQL:             localhost:5433 (user: beema, password: beema)"
echo "Kafka (external):       localhost:9092"
echo ""

echo "=========================================="
echo "Verification complete!"
echo "=========================================="
