# Beema Infrastructure - Root Module
#
# This root module orchestrates all infrastructure components:
# - VPC with multi-AZ networking
# - RDS PostgreSQL database
# - EKS Kubernetes cluster

# VPC Module
module "vpc" {
  source = "./modules/vpc"

  project_name       = var.project_name
  environment        = var.environment
  vpc_cidr           = var.vpc_cidr
  availability_zones = var.availability_zones
  enable_nat_gateway = true
  enable_flow_logs   = true
}

# Database Module
module "db" {
  source = "./modules/db"

  project_name         = var.project_name
  environment          = var.environment
  vpc_id               = module.vpc.vpc_id
  database_subnet_ids  = module.vpc.database_subnet_ids
  allowed_cidr_blocks  = module.vpc.private_subnet_ids  # Allow from private subnets
  availability_zones   = var.availability_zones

  # Database configuration
  db_name               = var.db_name
  db_username           = var.db_username
  postgres_version      = "15.4"
  db_instance_class     = var.db_instance_class
  allocated_storage     = var.db_allocated_storage
  max_allocated_storage = var.db_allocated_storage * 10  # Auto-scale up to 10x
  max_connections       = 200

  # High availability (disabled for dev, enabled for prod)
  multi_az = var.environment == "prod" ? true : false

  # Backup configuration
  backup_retention_period = var.environment == "prod" ? 30 : 7

  # Protection (enabled for prod)
  deletion_protection = var.environment == "prod" ? true : false
  skip_final_snapshot = var.environment == "prod" ? false : true

  # Monitoring
  enable_performance_insights = true

  depends_on = [module.vpc]
}

# EKS Cluster Module
module "k8s" {
  source = "./modules/k8s"

  project_name       = var.project_name
  environment        = var.environment
  vpc_id             = module.vpc.vpc_id
  private_subnet_ids = module.vpc.private_subnet_ids
  public_subnet_ids  = module.vpc.public_subnet_ids

  # Cluster configuration
  cluster_version                = var.eks_cluster_version
  cluster_endpoint_public_access = var.environment == "prod" ? false : true
  cluster_endpoint_access_cidrs  = ["0.0.0.0/0"]  # TODO: Restrict in production

  # Node group configuration
  node_instance_types = var.eks_node_instance_types
  node_desired_size   = var.eks_node_desired_size
  node_min_size       = var.eks_node_min_size
  node_max_size       = var.eks_node_max_size
  node_disk_size      = 100
  node_capacity_type  = var.environment == "prod" ? "ON_DEMAND" : "ON_DEMAND"

  depends_on = [module.vpc]
}
