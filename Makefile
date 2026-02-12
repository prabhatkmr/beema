.PHONY: help dev dev-up dev-down dev-logs dev-studio dev-kernel dev-metadata dev-processor infra-up infra-down clean

help: ## Show this help message
	@echo 'Usage: make [target]'
	@echo ''
	@echo 'Available targets:'
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-20s\033[0m %s\n", $$1, $$2}'

# ==============================================================================
# Development Mode (with hot-reload)
# ==============================================================================

dev: ## Start all services in development mode with hot-reload
	docker-compose -f docker-compose.yml -f docker-compose.dev.yml up --build

dev-up: ## Start all services in development mode in background
	docker-compose -f docker-compose.yml -f docker-compose.dev.yml up --build -d

dev-down: ## Stop all development services
	docker-compose -f docker-compose.yml -f docker-compose.dev.yml down

dev-logs: ## Follow logs from all dev services
	docker-compose -f docker-compose.yml -f docker-compose.dev.yml logs -f

dev-studio: ## Start only Studio in dev mode (requires infra)
	docker-compose -f docker-compose.yml -f docker-compose.dev.yml up studio

dev-kernel: ## Start only Kernel in dev mode (requires infra)
	docker-compose -f docker-compose.yml -f docker-compose.dev.yml up beema-kernel

dev-metadata: ## Start only Metadata Service in dev mode (requires infra)
	docker-compose -f docker-compose.yml -f docker-compose.dev.yml up metadata-service

dev-processor: ## Start only Message Processor in dev mode (requires infra)
	docker-compose -f docker-compose.yml -f docker-compose.dev.yml up beema-message-processor

# ==============================================================================
# Infrastructure Only (for local dev without Docker)
# ==============================================================================

infra-up: ## Start only infrastructure services (postgres, kafka, temporal, etc.)
	docker-compose up -d postgres kafka zookeeper kafka-init temporal temporal-ui keycloak inngest jaeger prometheus grafana

infra-down: ## Stop infrastructure services
	docker-compose down

infra-logs: ## Follow infrastructure logs
	docker-compose logs -f postgres kafka temporal

# ==============================================================================
# Production Mode
# ==============================================================================

prod: ## Start all services in production mode
	docker-compose up --build

prod-up: ## Start all services in production mode in background
	docker-compose up --build -d

prod-down: ## Stop all production services
	docker-compose down

# ==============================================================================
# Local Development (run services outside Docker)
# ==============================================================================

local-studio: ## Run Studio locally (requires infra-up)
	cd apps/studio && pnpm dev

local-kernel: ## Run Kernel locally (requires infra-up)
	cd apps/beema-kernel && mvn spring-boot:run -Dspring-boot.run.profiles=dev

local-metadata: ## Run Metadata Service locally (requires infra-up)
	cd apps/metadata-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev

local-processor: ## Run Message Processor locally (requires infra-up)
	cd apps/beema-message-processor && mvn exec:java -Dexec.mainClass="com.beema.processor.MessageProcessorJob"

# ==============================================================================
# Utilities
# ==============================================================================

clean: ## Remove all containers, volumes, and build artifacts
	docker-compose -f docker-compose.yml -f docker-compose.dev.yml down -v
	find . -type d -name "target" -exec rm -rf {} + 2>/dev/null || true
	find . -type d -name "node_modules" -exec rm -rf {} + 2>/dev/null || true
	find . -type d -name ".next" -exec rm -rf {} + 2>/dev/null || true

rebuild: clean dev-up ## Clean and rebuild all services in dev mode

ps: ## Show status of all services
	docker-compose ps

logs-studio: ## Follow Studio logs
	docker-compose logs -f studio

logs-kernel: ## Follow Kernel logs
	docker-compose logs -f beema-kernel

logs-metadata: ## Follow Metadata Service logs
	docker-compose logs -f metadata-service

logs-processor: ## Follow Message Processor logs
	docker-compose logs -f beema-message-processor

logs-kafka: ## Follow Kafka logs
	docker-compose logs -f kafka

shell-studio: ## Open shell in Studio container
	docker-compose exec studio sh

shell-kernel: ## Open shell in Kernel container
	docker-compose exec beema-kernel bash

shell-metadata: ## Open shell in Metadata Service container
	docker-compose exec metadata-service bash

shell-postgres: ## Open PostgreSQL shell
	docker-compose exec postgres psql -U beema -d beema_kernel

# ==============================================================================
# Testing
# ==============================================================================

test: ## Run all tests
	cd apps/beema-kernel && mvn test
	cd apps/metadata-service && mvn test
	cd apps/beema-message-processor && mvn test
	cd apps/studio && pnpm test

test-kernel: ## Run Kernel tests
	cd apps/beema-kernel && mvn test

test-metadata: ## Run Metadata Service tests
	cd apps/metadata-service && mvn test

test-processor: ## Run Message Processor tests
	cd apps/beema-message-processor && mvn test

test-studio: ## Run Studio tests
	cd apps/studio && pnpm test
