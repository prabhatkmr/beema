# Stage 1: Build
FROM node:20-alpine AS builder
WORKDIR /app

# Install pnpm
RUN npm install -g pnpm

# Copy workspace config and package files (for dependency caching)
COPY package.json pnpm-lock.yaml* pnpm-workspace.yaml ./
COPY apps/portal/package.json ./apps/portal/

# Install dependencies (single stage preserves pnpm symlinks)
RUN pnpm install --frozen-lockfile

# Copy application source code
COPY apps/portal ./apps/portal

# Build the application
WORKDIR /app/apps/portal
RUN pnpm run build

# Stage 2: Runtime
FROM node:20-alpine AS runtime
WORKDIR /app

# Create non-root user
RUN addgroup --system --gid 1001 nodejs && \
    adduser --system --uid 1001 nextjs

# Copy built application
COPY --from=builder /app/apps/portal/.next/standalone ./
COPY --from=builder /app/apps/portal/.next/static ./apps/portal/.next/static

# Set ownership
RUN chown -R nextjs:nodejs /app

USER nextjs

EXPOSE 3000

ENV PORT=3000
ENV NODE_ENV=production
ENV HOSTNAME="0.0.0.0"

CMD ["node", "apps/portal/server.js"]
