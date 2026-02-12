# Beema Studio - Quick Start Guide

Get up and running with Beema Studio in 5 minutes.

## Prerequisites

- Node.js 18+
- pnpm 8+
- Git

## Quick Start

### 1. Install Dependencies

```bash
cd apps/studio
pnpm install
```

### 2. Setup Environment

```bash
# Copy environment template
cp .env.example .env

# Edit .env if needed (default values work for local development)
```

### 3. Start Development Server

```bash
pnpm dev
```

Open http://localhost:3000 in your browser.

## Development Workflow

### Creating a Blueprint

1. Click "New Blueprint" in the sidebar
2. Fill in the form:
   - Name: "My First Blueprint"
   - Source System: "external-api"
   - Target Schema: "beema-policy"
3. Click "Create"

### Mapping Fields

1. Drag a field from the left panel (source)
2. Drop it on a field in the right panel (target)
3. The mapping is created automatically

### Adding Transformations

1. Click on a mapped field in the target panel
2. The expression editor opens at the bottom
3. Write your JEXL expression:
   ```javascript
   source.premium * 1.05
   ```
4. Click "Test" to validate

### Saving Your Work

- Click "Save" in the header
- Changes are persisted to the backend
- The blueprint is now available for processing

## Common Commands

```bash
# Development
pnpm dev              # Start dev server with hot reload
pnpm build            # Build for production
pnpm preview          # Preview production build

# Code Quality
pnpm lint             # Run linter
pnpm format           # Format code
pnpm test             # Run tests
pnpm test:ui          # Run tests with UI

# Docker
docker build -t beema/studio .
docker run -p 3000:80 beema/studio
```

## Keyboard Shortcuts

- `Ctrl/Cmd + S` - Save blueprint
- `Ctrl/Cmd + K` - Search blueprints
- `Esc` - Close modals
- `Delete` - Remove selected mapping

## Troubleshooting

**Server won't start?**
- Check port 3000 is available
- Try `pnpm clean && pnpm install`

**API calls failing?**
- Verify beema-kernel is running on port 8080
- Check VITE_API_BASE_URL in .env

**Build errors?**
- Clear node_modules: `rm -rf node_modules && pnpm install`
- Check Node.js version: `node --version` (should be 18+)

## Next Steps

- Read the full [README.md](./README.md) for detailed documentation
- Check out [JEXL Expression Guide](./README.md#jexl-expression-syntax)
- Deploy to production with [Docker](./README.md#docker-deployment)

## Need Help?

- Documentation: https://docs.beema.io/studio
- Issues: https://github.com/prabhatkmr/beema/issues
- Slack: #beema-studio
