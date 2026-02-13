#!/bin/bash
# Beema Platform - Developer Shell Aliases
#
# This file contains helpful aliases for local development.
#
# Installation:
#   For zsh users (macOS default):
#     echo "source $(pwd)/infra/dev-aliases.sh" >> ~/.zshrc
#     source ~/.zshrc
#
#   For bash users:
#     echo "source $(pwd)/infra/dev-aliases.sh" >> ~/.bashrc
#     source ~/.bashrc
#
# Or run once per session:
#     source ./infra/dev-aliases.sh

# =============================================================================
# Docker Infrastructure Aliases
# =============================================================================

# Start all infrastructure services (PostgreSQL, Kafka, Temporal, etc.)
alias beema-infra='docker compose up -d postgres keycloak kafka zookeeper temporal inngest minio minio-init kafka-init jaeger prometheus grafana temporal-ui'

# Stop all Docker containers
alias beema-infra-down='docker compose down'

# View logs from all infrastructure services (follow mode)
alias beema-infra-logs='docker compose logs -f'

# Check status of all Docker containers
alias beema-infra-ps='docker compose ps'

# Restart all infrastructure services
alias beema-infra-restart='docker compose restart'

# Stop and remove all containers, networks, and volumes (clean slate)
alias beema-infra-clean='docker compose down -v'

# =============================================================================
# Docker Full Stack Aliases (All Services including Apps)
# =============================================================================

# Start EVERYTHING (infrastructure + application services)
alias beema-up='docker compose up -d'

# Stop everything
alias beema-down='docker compose down'

# Rebuild all Docker images
alias beema-rebuild='docker compose build'

# View logs from all services
alias beema-logs='docker compose logs -f'

# =============================================================================
# Development Workflow Aliases
# =============================================================================

# Start infrastructure, then run frontend apps (Dashboard, Studio, Portal)
alias beema-dev='docker compose up -d postgres keycloak kafka zookeeper temporal inngest minio minio-init kafka-init jaeger prometheus grafana temporal-ui && echo "⏳ Waiting for services to be healthy..." && sleep 10 && pnpm run dev:frontend'

# Start infrastructure + frontend in background (logs to beema-dev.log)
alias beema-dev-bg='docker compose up -d postgres keycloak kafka zookeeper temporal inngest minio minio-init kafka-init jaeger prometheus grafana temporal-ui && echo "⏳ Waiting for services to be healthy..." && sleep 10 && nohup pnpm run dev:frontend > beema-dev.log 2>&1 & echo "✅ Started in background. View logs: tail -f beema-dev.log"'

# Start infrastructure + backend services, then run frontend apps
alias beema-dev-full='docker compose up -d postgres keycloak kafka zookeeper temporal inngest minio minio-init kafka-init jaeger prometheus grafana temporal-ui beema-kernel metadata-service beema-message-processor beema-streaming && echo "⏳ Waiting for services to be healthy..." && sleep 15 && pnpm run dev:frontend'

# Quick health check - shows status of all services
alias beema-health='docker compose ps && echo "\n📊 Service URLs:\n- Dashboard: http://localhost:3000\n- Studio: http://localhost:3010\n- Portal: http://localhost:3011\n- Kernel API: http://localhost:8080\n- Metadata API: http://localhost:8082\n- Keycloak: http://localhost:8180\n- Grafana: http://localhost:3002\n- Jaeger: http://localhost:16686\n- Temporal UI: http://localhost:8088\n- Inngest: http://localhost:8288\n- MinIO: http://localhost:9001"'

# View logs from background dev servers
alias beema-dev-logs='tail -f beema-dev.log'

# Stop background dev servers
alias beema-dev-stop='pkill -f "next dev" && echo "✅ Stopped background dev servers"'

# =============================================================================
# Utility Aliases
# =============================================================================

# Open Beema project in VS Code (from any directory)
alias beema-code='code /Users/prabhatkumar/Desktop/dev-directory/beema'

# Navigate to Beema project directory
alias beema-cd='cd /Users/prabhatkumar/Desktop/dev-directory/beema'

echo "✅ Beema development aliases loaded!"
echo "Run 'beema-health' to see all available service URLs"
