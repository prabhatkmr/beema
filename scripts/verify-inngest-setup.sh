#!/bin/bash

echo "Verifying Inngest Setup"
echo "======================="

ERRORS=0

# Check if Inngest container is running
if docker-compose ps inngest | grep -q "Up"; then
    echo "✅ Inngest container is running"
else
    echo "❌ Inngest container is not running"
    ERRORS=$((ERRORS + 1))
fi

# Check Inngest health endpoint
if curl -f http://localhost:8288/health > /dev/null 2>&1; then
    echo "✅ Inngest health check passed"
else
    echo "❌ Inngest health check failed"
    ERRORS=$((ERRORS + 1))
fi

# Check if beema-kernel can reach Inngest
if docker-compose exec -T beema-kernel curl -f http://inngest:8288/health > /dev/null 2>&1; then
    echo "✅ beema-kernel can reach Inngest"
else
    echo "❌ beema-kernel cannot reach Inngest"
    ERRORS=$((ERRORS + 1))
fi

# Check Studio Inngest serve route
if curl -f http://localhost:3000/api/inngest > /dev/null 2>&1; then
    echo "✅ Studio Inngest serve route is accessible"
else
    echo "⚠️  Studio Inngest serve route not accessible (Studio may not be running)"
fi

# Check sys_webhooks table exists
if docker-compose exec -T postgres psql -U beema -d beema_kernel -c "\dt sys_webhooks" | grep -q "sys_webhooks"; then
    echo "✅ sys_webhooks table exists"
else
    echo "❌ sys_webhooks table not found"
    ERRORS=$((ERRORS + 1))
fi

echo ""
if [ $ERRORS -eq 0 ]; then
    echo "✅ All checks passed!"
    echo ""
    echo "Access Points:"
    echo "  - Inngest UI: http://localhost:8288"
    echo "  - Studio: http://localhost:3000"
    echo "  - beema-kernel: http://localhost:8080"
else
    echo "❌ $ERRORS check(s) failed"
    exit 1
fi
