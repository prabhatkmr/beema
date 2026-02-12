#!/bin/bash

echo "🚀 Starting Inngest Dev Server"
echo "================================"

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker and try again."
    exit 1
fi

# Load environment variables
if [ -f .env.inngest ]; then
    export $(cat .env.inngest | xargs)
fi

# Start Inngest service
echo "Starting Inngest Dev Server..."
docker-compose up -d inngest

# Wait for health check
echo "Waiting for Inngest to be ready..."
timeout 30 sh -c 'until docker-compose exec -T inngest curl -f http://localhost:8288/health > /dev/null 2>&1; do sleep 1; done'

if [ $? -eq 0 ]; then
    echo "✅ Inngest Dev Server is running"
    echo ""
    echo "📊 Inngest Dev UI: http://localhost:8288"
    echo "📡 Event API: http://localhost:8288/e/local"
    echo ""
    echo "Test event publishing:"
    echo "  curl -X POST http://localhost:8080/api/v1/events/test/policy-bound"
else
    echo "❌ Inngest failed to start"
    docker-compose logs inngest
    exit 1
fi
