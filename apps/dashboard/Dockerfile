# Stage 1: Build
FROM node:20-alpine AS builder
WORKDIR /app

# Install pnpm
RUN npm install -g pnpm

# Copy workspace config and package files (for dependency caching)
COPY package.json pnpm-lock.yaml* pnpm-workspace.yaml ./
COPY apps/dashboard/package.json ./apps/dashboard/
COPY packages/ui/package.json ./packages/ui/

# Install dependencies (no cross-stage COPY preserves pnpm symlinks)
RUN pnpm install --frozen-lockfile

# Copy application source code
COPY tsconfig.json ./
COPY apps/dashboard ./apps/dashboard
COPY packages ./packages
COPY turbo.json ./

# Build the application
WORKDIR /app/apps/dashboard
RUN pnpm run build

# Stage 3: Runtime
FROM node:20-alpine AS runtime
WORKDIR /app

# Create non-root user
RUN addgroup --system --gid 1001 nodejs && \
    adduser --system --uid 1001 nextjs

# Copy built application
COPY --from=builder /app/apps/dashboard/.next/standalone ./
COPY --from=builder /app/apps/dashboard/.next/static ./apps/dashboard/.next/static

# Set ownership
RUN chown -R nextjs:nodejs /app

USER nextjs

EXPOSE 3000

ENV PORT=3000
ENV NODE_ENV=production
ENV HOSTNAME="0.0.0.0"

CMD ["node", "apps/dashboard/server.js"]
