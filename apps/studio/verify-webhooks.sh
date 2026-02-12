#!/bin/bash

echo "Verifying Webhooks UI Implementation..."
echo "========================================"
echo ""

STUDIO_DIR="/Users/prabhatkumar/Desktop/dev-directory/beema/apps/studio"

# Check page
echo "Checking page..."
if [ -f "$STUDIO_DIR/app/webhooks/page.tsx" ]; then
  echo "✓ app/webhooks/page.tsx"
else
  echo "✗ app/webhooks/page.tsx MISSING"
fi

# Check components
echo ""
echo "Checking components..."
COMPONENTS=(
  "WebhookForm.tsx"
  "WebhookList.tsx"
  "WebhookDeliveries.tsx"
  "WebhookTester.tsx"
  "index.ts"
)

for component in "${COMPONENTS[@]}"; do
  if [ -f "$STUDIO_DIR/components/webhooks/$component" ]; then
    echo "✓ components/webhooks/$component"
  else
    echo "✗ components/webhooks/$component MISSING"
  fi
done

# Check API routes
echo ""
echo "Checking API routes..."
API_ROUTES=(
  "route.ts"
  "[id]/route.ts"
  "[id]/test/route.ts"
  "deliveries/route.ts"
)

for route in "${API_ROUTES[@]}"; do
  if [ -f "$STUDIO_DIR/app/api/webhooks/$route" ]; then
    echo "✓ app/api/webhooks/$route"
  else
    echo "✗ app/api/webhooks/$route MISSING"
  fi
done

# Check documentation
echo ""
echo "Checking documentation..."
DOCS=(
  "WEBHOOKS_UI_GUIDE.md"
  "WEBHOOKS_IMPLEMENTATION.md"
  "WEBHOOKS_SUMMARY.md"
)

for doc in "${DOCS[@]}"; do
  if [ -f "$STUDIO_DIR/$doc" ]; then
    echo "✓ $doc"
  else
    echo "✗ $doc MISSING"
  fi
done

# Check layout update
echo ""
echo "Checking layout update..."
if grep -q "href=\"/webhooks\"" "$STUDIO_DIR/app/layout.tsx"; then
  echo "✓ Navigation link added to layout.tsx"
else
  echo "✗ Navigation link NOT found in layout.tsx"
fi

echo ""
echo "========================================"
echo "Verification complete!"
echo ""
echo "To start the dev server:"
echo "  cd $STUDIO_DIR"
echo "  npm run dev"
echo ""
echo "Then navigate to: http://localhost:3000/webhooks"
