# Root Module Outputs

# VPC Outputs
output "vpc_id" {
  description = "VPC ID"
  value       = module.vpc.vpc_id
}

output "public_subnet_ids" {
  description = "Public subnet IDs"
  value       = module.vpc.public_subnet_ids
}

output "private_subnet_ids" {
  description = "Private subnet IDs"
  value       = module.vpc.private_subnet_ids
}

output "database_subnet_ids" {
  description = "Database subnet IDs"
  value       = module.vpc.database_subnet_ids
}

output "nat_gateway_ips" {
  description = "NAT Gateway public IPs"
  value       = module.vpc.nat_gateway_ips
}

# Database Outputs
output "db_endpoint" {
  description = "Database endpoint"
  value       = module.db.db_endpoint
}

output "db_address" {
  description = "Database address"
  value       = module.db.db_address
}

output "db_port" {
  description = "Database port"
  value       = module.db.db_port
}

output "db_name" {
  description = "Database name"
  value       = module.db.db_name
}

output "db_secret_arn" {
  description = "ARN of Secrets Manager secret containing database credentials"
  value       = module.db.db_secret_arn
}

# EKS Outputs
output "eks_cluster_id" {
  description = "EKS cluster ID"
  value       = module.k8s.cluster_id
}

output "eks_cluster_name" {
  description = "EKS cluster name"
  value       = module.k8s.cluster_name
}

output "eks_cluster_endpoint" {
  description = "EKS cluster endpoint"
  value       = module.k8s.cluster_endpoint
}

output "eks_cluster_version" {
  description = "EKS cluster Kubernetes version"
  value       = module.k8s.cluster_version
}

output "eks_oidc_provider_arn" {
  description = "ARN of OIDC provider for IRSA"
  value       = module.k8s.oidc_provider_arn
}

output "kubeconfig_command" {
  description = "Command to configure kubectl"
  value       = module.k8s.kubeconfig_command
}

# Connection String for Application
output "db_connection_string_template" {
  description = "Database connection string template (password in Secrets Manager)"
  value       = "postgresql://${module.db.db_username}:PASSWORD_FROM_SECRETS_MANAGER@${module.db.db_address}:${module.db.db_port}/${module.db.db_name}"
  sensitive   = true
}
