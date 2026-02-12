#!/bin/bash
set -e

# Beema Development Mode Launcher
# This script helps you quickly start services in development mode

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}╔════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║   Beema Platform - Development Mode   ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════╝${NC}"
echo ""

# Function to check if a port is in use
check_port() {
    local port=$1
    if lsof -Pi :$port -sTCP:LISTEN -t >/dev/null 2>&1; then
        return 0  # Port is in use
    else
        return 1  # Port is free
    fi
}

# Function to kill process on port
kill_port() {
    local port=$1
    echo -e "${YELLOW}Killing process on port $port...${NC}"
    lsof -ti:$port | xargs kill -9 2>/dev/null || true
}

# Check for conflicting ports
echo -e "${BLUE}Checking for port conflicts...${NC}"
CONFLICTS=0

if check_port 3000; then
    echo -e "${YELLOW}⚠️  Port 3000 (Studio) is in use${NC}"
    CONFLICTS=1
fi

if check_port 8080; then
    echo -e "${YELLOW}⚠️  Port 8080 (Kernel) is in use${NC}"
    CONFLICTS=1
fi

if check_port 8082; then
    echo -e "${YELLOW}⚠️  Port 8082 (Metadata Service) is in use${NC}"
    CONFLICTS=1
fi

if [ $CONFLICTS -eq 1 ]; then
    echo ""
    read -p "Kill processes on conflicting ports? (y/N) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        check_port 3000 && kill_port 3000
        check_port 8080 && kill_port 8080
        check_port 8082 && kill_port 8082
        echo -e "${GREEN}✓ Ports cleared${NC}"
    fi
fi

echo ""
echo -e "${BLUE}Select development mode:${NC}"
echo "  1) Full Docker dev mode (all services with hot-reload)"
echo "  2) Local dev mode (infra in Docker, services running locally)"
echo "  3) Infrastructure only (for manual service startup)"
echo "  4) Single service (choose which one)"
echo "  5) Production mode (no dev tools)"
echo ""
read -p "Enter choice [1-5]: " choice

case $choice in
    1)
        echo -e "${GREEN}Starting all services in Docker dev mode...${NC}"
        docker-compose -f docker-compose.yml -f docker-compose.dev.yml up --build
        ;;
    2)
        echo -e "${GREEN}Starting infrastructure services...${NC}"
        docker-compose up -d postgres kafka zookeeper kafka-init temporal temporal-ui keycloak inngest jaeger prometheus grafana

        echo ""
        echo -e "${YELLOW}Infrastructure started. Now run services locally:${NC}"
        echo ""
        echo -e "  ${BLUE}Terminal 1:${NC} cd apps/studio && pnpm dev"
        echo -e "  ${BLUE}Terminal 2:${NC} cd apps/beema-kernel && mvn spring-boot:run -Dspring-boot.run.profiles=dev"
        echo -e "  ${BLUE}Terminal 3:${NC} cd apps/metadata-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev"
        echo -e "  ${BLUE}Terminal 4:${NC} cd apps/beema-message-processor && mvn exec:java"
        echo ""
        echo -e "${GREEN}Press Ctrl+C to stop infrastructure${NC}"
        docker-compose logs -f postgres kafka temporal
        ;;
    3)
        echo -e "${GREEN}Starting infrastructure only...${NC}"
        docker-compose up postgres kafka zookeeper kafka-init temporal temporal-ui keycloak inngest jaeger prometheus grafana
        ;;
    4)
        echo ""
        echo -e "${BLUE}Select service:${NC}"
        echo "  1) Studio (Next.js)"
        echo "  2) Kernel (Spring Boot)"
        echo "  3) Metadata Service (Spring Boot)"
        echo "  4) Message Processor (Flink)"
        echo ""
        read -p "Enter choice [1-4]: " service_choice

        # Start infrastructure first
        echo -e "${GREEN}Starting infrastructure...${NC}"
        docker-compose up -d postgres kafka zookeeper kafka-init temporal temporal-ui keycloak inngest

        case $service_choice in
            1)
                echo -e "${GREEN}Starting Studio in dev mode...${NC}"
                docker-compose -f docker-compose.yml -f docker-compose.dev.yml up studio
                ;;
            2)
                echo -e "${GREEN}Starting Kernel in dev mode...${NC}"
                docker-compose -f docker-compose.yml -f docker-compose.dev.yml up beema-kernel
                ;;
            3)
                echo -e "${GREEN}Starting Metadata Service in dev mode...${NC}"
                docker-compose -f docker-compose.yml -f docker-compose.dev.yml up metadata-service
                ;;
            4)
                echo -e "${GREEN}Starting Message Processor in dev mode...${NC}"
                docker-compose -f docker-compose.yml -f docker-compose.dev.yml up beema-message-processor
                ;;
            *)
                echo -e "${RED}Invalid choice${NC}"
                exit 1
                ;;
        esac
        ;;
    5)
        echo -e "${GREEN}Starting all services in production mode...${NC}"
        docker-compose up --build
        ;;
    *)
        echo -e "${RED}Invalid choice${NC}"
        exit 1
        ;;
esac
