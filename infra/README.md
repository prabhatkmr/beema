# Beema Infrastructure as Code (OpenTofu)

Production-grade infrastructure for the Beema insurance platform using OpenTofu.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                         AWS VPC                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                  Public Subnets (3 AZs)                │ │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐             │ │
│  │  │   NAT    │  │   NAT    │  │   NAT    │             │ │
│  │  │ Gateway  │  │ Gateway  │  │ Gateway  │             │ │
│  │  └──────────┘  └──────────┘  └──────────┘             │ │
│  │       │              │              │                   │ │
│  │  ┌────▼────────┬────▼────────┬────▼────────┐          │ │
│  │  │   ALB / NLB (EKS Load Balancers)        │          │ │
│  │  └─────────────────────────────────────────┘          │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                 Private Subnets (3 AZs)                │ │
│  │  ┌──────────────────────────────────────────────────┐ │ │
│  │  │         EKS Cluster (beema-prod-eks)             │ │ │
│  │  │  ┌──────────┐  ┌──────────┐  ┌──────────┐       │ │ │
│  │  │  │  Worker  │  │  Worker  │  │  Worker  │       │ │ │
│  │  │  │   Node   │  │   Node   │  │   Node   │       │ │ │
│  │  │  └──────────┘  └──────────┘  └──────────┘       │ │ │
│  │  │                                                   │ │ │
│  │  │  - Spring Boot Apps                              │ │ │
│  │  │  - Temporal Workers                              │ │ │
│  │  │  - Background Jobs                               │ │ │
│  │  └──────────────────────────────────────────────────┘ │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                Database Subnets (3 AZs)                │ │
│  │  ┌──────────────────────────────────────────────────┐ │ │
│  │  │     RDS PostgreSQL (Multi-AZ)                    │ │ │
│  │  │  ┌─────────────┐       ┌─────────────┐          │ │ │
│  │  │  │   Primary   │ ───▶  │  Standby    │          │ │ │
│  │  │  │  (Active)   │       │ (Failover)  │          │ │ │
│  │  │  └─────────────┘       └─────────────┘          │ │ │
│  │  │                                                   │ │ │
│  │  │  - 500 GB Storage (auto-scales to 5 TB)         │ │ │
│  │  │  - 30-day automated backups                      │ │ │
│  │  │  - Encrypted with KMS                            │ │ │
│  │  │  - Performance Insights enabled                  │ │ │
│  │  └──────────────────────────────────────────────────┘ │ │
│  └────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## Modules

### VPC Module (`modules/vpc/`)
Creates secure multi-AZ network:
- Public subnets for load balancers
- Private subnets for application workloads (EKS)
- Database subnets (isolated, no internet)
- NAT Gateways (one per AZ)
- VPC Flow Logs for monitoring
- Proper routing tables

### Database Module (`modules/db/`)
Provisions production-grade RDS PostgreSQL:
- Multi-AZ deployment (high availability)
- Automated backups (30-day retention)
- Encryption at rest (KMS)
- Performance Insights enabled
- Enhanced monitoring
- CloudWatch alarms (CPU, storage, connections)
- Secrets Manager for credentials
- Auto-scaling storage

### Kubernetes Module (`modules/k8s/`)
Deploys managed EKS cluster:
- Managed control plane
- Managed node groups with autoscaling
- IAM Roles for Service Accounts (IRSA)
- VPC CNI, CoreDNS, kube-proxy add-ons
- CloudWatch logging
- IMDSv2 for nodes
- Encrypted EBS volumes

## Prerequisites

1. **OpenTofu Installed**
   ```bash
   brew install opentofu
   # or
   curl --proto '=https' --tlsv1.2 -fsSL https://get.opentofu.org/install-opentofu.sh | sh
   ```

2. **AWS CLI Configured**
   ```bash
   aws configure
   # Enter AWS Access Key ID, Secret Access Key, Region
   ```

3. **Create S3 Backend Resources**
   ```bash
   # S3 bucket for state
   aws s3api create-bucket \
     --bucket beema-terraform-state \
     --region us-east-1

   aws s3api put-bucket-versioning \
     --bucket beema-terraform-state \
     --versioning-configuration Status=Enabled

   aws s3api put-bucket-encryption \
     --bucket beema-terraform-state \
     --server-side-encryption-configuration \
     '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"}}]}'

   # DynamoDB table for state locking
   aws dynamodb create-table \
     --table-name beema-terraform-locks \
     --attribute-definitions AttributeName=LockID,AttributeType=S \
     --key-schema AttributeName=LockID,KeyType=HASH \
     --billing-mode PAY_PER_REQUEST \
     --region us-east-1
   ```

## Usage

### Deploy Development Environment

```bash
cd infra

# Initialize OpenTofu
tofu init

# Plan infrastructure changes
tofu plan -var-file=environments/dev/terraform.tfvars

# Apply changes
tofu apply -var-file=environments/dev/terraform.tfvars
```

### Deploy Production Environment

```bash
cd infra

# Initialize OpenTofu
tofu init

# Plan infrastructure changes
tofu plan -var-file=environments/prod/terraform.tfvars

# Apply changes (requires approval)
tofu apply -var-file=environments/prod/terraform.tfvars
```

### Connect to EKS Cluster

```bash
# Update kubeconfig
aws eks update-kubeconfig --region us-east-1 --name beema-prod-eks

# Verify connection
kubectl get nodes
kubectl get pods --all-namespaces
```

### Access Database Credentials

```bash
# Get database password from Secrets Manager
aws secretsmanager get-secret-value \
  --secret-id beema-prod-db-master-password \
  --query SecretString \
  --output text | jq .
```

### Destroy Infrastructure

```bash
# Development
tofu destroy -var-file=environments/dev/terraform.tfvars

# Production (requires deletion protection to be disabled)
tofu destroy -var-file=environments/prod/terraform.tfvars
```

## Cost Estimates

### Development Environment
- VPC: ~$90/month (3 NAT Gateways)
- RDS: ~$130/month (db.t3.medium, 100 GB)
- EKS: ~$73/month (cluster) + ~$150/month (2 t3.large nodes)
- **Total: ~$443/month**

### Production Environment
- VPC: ~$90/month (3 NAT Gateways)
- RDS: ~$450/month (db.r6g.xlarge Multi-AZ, 500 GB)
- EKS: ~$73/month (cluster) + ~$900/month (6 m5.xlarge nodes)
- **Total: ~$1,513/month**

## Security Features

✅ **Network Isolation**
- Private subnets for workloads
- Database subnets with no internet access
- Security groups with least privilege

✅ **Encryption**
- RDS encrypted at rest with KMS
- EKS secrets encrypted with KMS
- EBS volumes encrypted

✅ **Access Control**
- IAM roles for EKS nodes
- IRSA for Kubernetes service accounts
- Secrets Manager for database credentials

✅ **Monitoring**
- VPC Flow Logs
- EKS Control Plane logs
- RDS Enhanced Monitoring
- CloudWatch alarms

✅ **High Availability**
- Multi-AZ deployment for RDS
- NAT Gateways in each AZ
- EKS nodes across multiple AZs

## State Management

- **Backend:** S3 + DynamoDB
- **State Locking:** Enabled via DynamoDB
- **Encryption:** Server-side encryption (AES256)
- **Versioning:** Enabled for state recovery

## Maintenance

### Update OpenTofu Providers
```bash
tofu init -upgrade
```

### Format Configuration
```bash
tofu fmt -recursive
```

### Validate Configuration
```bash
tofu validate
```

### View Current State
```bash
tofu show
tofu state list
```

## Troubleshooting

### State Lock Issues
```bash
# Force unlock (use carefully!)
tofu force-unlock <LOCK_ID>
```

### EKS Connection Issues
```bash
# Verify AWS credentials
aws sts get-caller-identity

# Check EKS cluster status
aws eks describe-cluster --name beema-prod-eks --region us-east-1
```

### RDS Connection Issues
```bash
# Verify security group allows traffic from EKS
aws ec2 describe-security-groups --group-ids <DB_SG_ID>

# Test connection from EKS pod
kubectl run -it --rm debug --image=postgres:15 --restart=Never -- \
  psql -h <DB_ENDPOINT> -U beema_admin -d beema_prod
```

## Next Steps

After infrastructure is deployed:

1. **Deploy Helm Charts**
   ```bash
   cd ../platform
   helm install beema-kernel ./charts/beema-kernel
   ```

2. **Run Database Migrations**
   ```bash
   kubectl exec -it deployment/beema-kernel -- \
     mvn flyway:migrate
   ```

3. **Configure DNS**
   - Point domain to EKS Load Balancer
   - Configure SSL/TLS certificates

4. **Set Up Monitoring**
   - Deploy Prometheus
   - Deploy Grafana
   - Configure alerts

---

**Managed By:** OpenTofu
**Last Updated:** 2026-02-12
