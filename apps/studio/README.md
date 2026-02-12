# Beema Studio

Next.js application for visual form layout building and blueprint management.

## Tech Stack

- Next.js 16 with App Router
- TypeScript
- Tailwind CSS v4
- @dnd-kit for drag and drop
- React 19

## Development

```bash
pnpm dev       # Start dev server on :3000
pnpm build     # Build for production
pnpm start     # Start production server
pnpm lint      # Run ESLint
```

## Features

- **Canvas Builder**: Drag-and-drop form layout designer
- **Blueprint Editor**: Message transformation blueprints
- Integration with beema-kernel metadata schema

## Project Structure

```
apps/studio/
├── app/
│   ├── api/           # API routes
│   ├── canvas/        # Canvas builder pages
│   ├── blueprints/    # Blueprint management pages
│   ├── layout.tsx     # Root layout
│   ├── page.tsx       # Home page
│   └── globals.css    # Global styles
├── components/
│   ├── canvas/        # Canvas-specific components
│   └── ui/            # Reusable UI components
├── lib/               # Utility functions
├── types/             # TypeScript type definitions
└── public/            # Static assets
```

## Integration with Beema Platform

This app integrates with:
- beema-kernel for metadata schemas
- beema-message-processor for blueprint execution
- metadata-service for field definitions

## Getting Started

1. Install dependencies:
```bash
pnpm install
```

2. Start the development server:
```bash
pnpm dev
```

3. Open [http://localhost:3000](http://localhost:3000) in your browser

## Configuration

The app is configured for Turborepo integration with:
- Transpiled packages: `@beema/ui`
- TypeScript path aliases for clean imports
- Tailwind CSS with Beema brand colors
