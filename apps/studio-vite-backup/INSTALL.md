# Beema Studio - Installation & Setup Guide

Complete installation guide for Beema Studio in various environments.

## Table of Contents

1. [Local Development Setup](#local-development-setup)
2. [Docker Setup](#docker-setup)
3. [Kubernetes Setup](#kubernetes-setup)
4. [Troubleshooting](#troubleshooting)

---

## Local Development Setup

### Prerequisites

- **Node.js**: 18.0.0 or higher
- **pnpm**: 8.0.0 or higher
- **Git**: Latest version
- **Code Editor**: VS Code recommended

### Step 1: Verify Prerequisites

```bash
# Check Node.js version
node --version
# Should output: v18.x.x or higher

# Check pnpm version
pnpm --version
# Should output: 8.x.x or higher

# If pnpm is not installed:
npm install -g pnpm@8.15.0
```

### Step 2: Clone Repository

```bash
# Clone the repository
git clone https://github.com/prabhatkmr/beema.git
cd beema/apps/studio
```

### Step 3: Install Dependencies

```bash
# Install all dependencies
pnpm install

# This will install:
# - React, React-DOM
# - TypeScript
# - Vite
# - Tailwind CSS
# - @dnd-kit packages
# - Monaco Editor
# - React Query
# - Zustand
# - And all dev dependencies
```

### Step 4: Configure Environment

```bash
# Copy environment template
cp .env.example .env

# Edit .env file
# Default values work for local development:
# VITE_API_BASE_URL=http://localhost:8080/api/v1
```

### Step 5: Start Development Server

```bash
# Start the dev server
pnpm dev

# Server will start on http://localhost:3000
# Hot Module Replacement (HMR) is enabled
```

### Step 6: Verify Installation

1. Open browser to http://localhost:3000
2. You should see the Beema Studio interface
3. Check browser console for any errors

### Optional: Install VS Code Extensions

```bash
# VS Code will prompt to install recommended extensions
# Or install manually:
code --install-extension dbaeumer.vscode-eslint
code --install-extension esbenp.prettier-vscode
code --install-extension bradlc.vscode-tailwindcss
```

---

## Docker Setup

### Prerequisites

- **Docker**: 20.10 or higher
- **Docker Compose**: 2.0 or higher

### Step 1: Build Docker Image

```bash
# Navigate to studio directory
cd apps/studio

# Build the image
docker build -t beema/studio:latest .

# This creates a multi-stage build:
# - Stage 1: Builds the application
# - Stage 2: Creates production nginx container
```

### Step 2: Run Container

```bash
# Run the container
docker run -d \
  --name beema-studio \
  -p 3000:80 \
  -e VITE_API_BASE_URL=http://localhost:8080/api/v1 \
  beema/studio:latest

# Check logs
docker logs -f beema-studio

# Stop container
docker stop beema-studio

# Remove container
docker rm beema-studio
```

### Step 3: Using Docker Compose

```bash
# From project root directory
cd /path/to/beema

# Start all services (including studio)
docker-compose up -d

# Check status
docker-compose ps

# View logs
docker-compose logs -f studio

# Stop all services
docker-compose down
```

### Docker Compose Configuration

The studio service is configured in `docker-compose.yml`:

```yaml
studio:
  build:
    context: ./apps/studio
    dockerfile: Dockerfile
  container_name: beema-studio
  ports:
    - "3000:80"
  environment:
    - VITE_API_BASE_URL=http://beema-kernel:8080/api/v1
  depends_on:
    - beema-kernel
  networks:
    - beema-network
```

---

## Kubernetes Setup

### Prerequisites

- **Kubernetes Cluster**: 1.24 or higher
- **Helm**: 3.10 or higher
- **kubectl**: Configured and connected to cluster

### Step 1: Verify Cluster

```bash
# Check cluster connection
kubectl cluster-info

# Check nodes
kubectl get nodes

# Create namespace (if needed)
kubectl create namespace beema
```

### Step 2: Build and Push Image

```bash
# Build image
docker build -t your-registry/beema-studio:v1.0.0 .

# Tag for registry
docker tag beema/studio:latest your-registry/beema-studio:v1.0.0

# Push to registry
docker push your-registry/beema-studio:v1.0.0
```

### Step 3: Configure Helm Values

```bash
# Create custom values file
cat > custom-values.yaml <<EOF
image:
  repository: your-registry/beema-studio
  tag: v1.0.0

replicaCount: 2

ingress:
  enabled: true
  hosts:
    - host: studio.yourdomain.com
      paths:
        - path: /
          pathType: Prefix
  tls:
    - secretName: studio-tls
      hosts:
        - studio.yourdomain.com

config:
  apiBaseUrl: http://beema-kernel:8080/api/v1

resources:
  requests:
    cpu: 100m
    memory: 128Mi
  limits:
    cpu: 500m
    memory: 512Mi
EOF
```

### Step 4: Install with Helm

```bash
# Install the chart
helm install beema-studio ./helm/studio \
  -f custom-values.yaml \
  --namespace beema

# Check deployment
kubectl get pods -n beema

# Check services
kubectl get svc -n beema

# Check ingress
kubectl get ingress -n beema
```

### Step 5: Verify Deployment

```bash
# Get pod status
kubectl get pods -n beema -l app.kubernetes.io/name=beema-studio

# Check logs
kubectl logs -n beema -l app.kubernetes.io/name=beema-studio

# Port forward (for testing)
kubectl port-forward -n beema svc/beema-studio 3000:80

# Access at http://localhost:3000
```

### Step 6: Configure DNS

```bash
# Get ingress IP
kubectl get ingress -n beema beema-studio

# Add DNS record:
# studio.yourdomain.com -> <INGRESS_IP>
```

---

## Advanced Configuration

### SSL/TLS Setup

#### Using cert-manager

```bash
# Install cert-manager
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml

# Create ClusterIssuer
cat <<EOF | kubectl apply -f -
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: devops@yourdomain.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: nginx
EOF

# Update Helm values
helm upgrade beema-studio ./helm/studio \
  --set ingress.annotations."cert-manager\.io/cluster-issuer"=letsencrypt-prod
```

### Horizontal Pod Autoscaling

```bash
# Enable metrics-server if not already installed
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# Update HPA configuration in values.yaml
autoscaling:
  enabled: true
  minReplicas: 2
  maxReplicas: 10
  targetCPUUtilizationPercentage: 80
  targetMemoryUtilizationPercentage: 80

# Upgrade deployment
helm upgrade beema-studio ./helm/studio -f values.yaml

# Check HPA status
kubectl get hpa -n beema
```

### Custom Domain Configuration

```bash
# Update values.yaml
ingress:
  enabled: true
  className: "nginx"
  hosts:
    - host: studio.yourdomain.com
      paths:
        - path: /
          pathType: Prefix
  tls:
    - secretName: studio-tls
      hosts:
        - studio.yourdomain.com

# Upgrade deployment
helm upgrade beema-studio ./helm/studio -f values.yaml
```

---

## Troubleshooting

### Common Issues

#### Issue 1: Port Already in Use

```bash
# Error: Port 3000 is already in use

# Solution 1: Find and kill process
lsof -ti:3000 | xargs kill -9

# Solution 2: Use different port
pnpm dev -- --port 3001
```

#### Issue 2: Dependencies Installation Failed

```bash
# Error: Installation failed

# Solution 1: Clear cache
pnpm store prune
rm -rf node_modules
pnpm install

# Solution 2: Use specific registry
pnpm install --registry https://registry.npmjs.org/
```

#### Issue 3: Build Fails

```bash
# Error: Build failed

# Solution 1: Check Node version
node --version  # Should be 18+

# Solution 2: Clear build cache
rm -rf dist .turbo
pnpm build

# Solution 3: Check TypeScript errors
pnpm tsc --noEmit
```

#### Issue 4: API Connection Failed

```bash
# Error: Cannot connect to API

# Solution 1: Check beema-kernel is running
curl http://localhost:8080/actuator/health

# Solution 2: Check environment variable
echo $VITE_API_BASE_URL

# Solution 3: Update .env file
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

#### Issue 5: Docker Build Fails

```bash
# Error: Docker build failed

# Solution 1: Increase Docker memory
# Docker Desktop -> Settings -> Resources -> Memory: 4GB+

# Solution 2: Use BuildKit
DOCKER_BUILDKIT=1 docker build -t beema/studio .

# Solution 3: Clear build cache
docker builder prune
```

#### Issue 6: Kubernetes Pod Not Starting

```bash
# Check pod status
kubectl describe pod -n beema <pod-name>

# Common causes:
# 1. Image pull failed
kubectl get pods -n beema  # Check ImagePullBackOff

# 2. Resource limits too low
kubectl top pods -n beema

# 3. Health check failing
kubectl logs -n beema <pod-name>
```

### Debug Mode

#### Enable Verbose Logging

```bash
# Development
VITE_LOG_LEVEL=debug pnpm dev

# Production
kubectl set env deployment/beema-studio LOG_LEVEL=debug -n beema
```

#### Access Container Shell

```bash
# Docker
docker exec -it beema-studio sh

# Kubernetes
kubectl exec -it -n beema <pod-name> -- sh
```

### Performance Issues

#### Slow Build Times

```bash
# Solution 1: Use Vite cache
rm -rf node_modules/.vite

# Solution 2: Disable source maps
# vite.config.ts
build: {
  sourcemap: false
}

# Solution 3: Upgrade Node.js
nvm install 20
nvm use 20
```

#### High Memory Usage

```bash
# Solution 1: Increase Node memory
export NODE_OPTIONS="--max-old-space-size=4096"

# Solution 2: Reduce concurrent builds
pnpm build --max-workers=2
```

---

## Verification Checklist

### Local Development

- [ ] Node.js 18+ installed
- [ ] pnpm 8+ installed
- [ ] Dependencies installed successfully
- [ ] Dev server starts without errors
- [ ] Application loads in browser
- [ ] Hot reload works
- [ ] No console errors

### Docker

- [ ] Docker image builds successfully
- [ ] Container starts without errors
- [ ] Health check passes
- [ ] Application accessible on port 3000
- [ ] API proxy works
- [ ] Logs show no errors

### Kubernetes

- [ ] Helm chart validates
- [ ] Pods are running (2+ replicas)
- [ ] Service is created
- [ ] Ingress is configured
- [ ] TLS certificate issued
- [ ] Application accessible via domain
- [ ] HPA is working
- [ ] Health checks passing

---

## Next Steps

After successful installation:

1. **Read Documentation**: Review [README.md](./README.md)
2. **Quick Start**: Follow [QUICKSTART.md](./QUICKSTART.md)
3. **Architecture**: Understand [ARCHITECTURE.md](./ARCHITECTURE.md)
4. **Create Blueprint**: Start building your first blueprint
5. **Configure Backend**: Ensure beema-kernel API is accessible

---

## Support

- **Documentation**: https://docs.beema.io/studio
- **Issues**: https://github.com/prabhatkmr/beema/issues
- **Slack**: #beema-studio
- **Email**: devops@beema.io

---

**Last Updated**: February 2024
**Version**: 0.1.0
