# Development Environment Configuration

# General
environment  = "dev"
aws_region   = "us-east-1"
cost_center  = "beema-platform"
project_name = "beema"

# VPC
vpc_cidr = "10.1.0.0/16"
availability_zones = [
  "us-east-1a",
  "us-east-1b"
]

# Database - Development
db_name             = "beema_dev"
db_username         = "beema_admin"
db_instance_class   = "db.t3.medium"  # 2 vCPU, 4 GB RAM
db_allocated_storage = 100  # 100 GB

# EKS - Development
eks_cluster_version      = "1.28"
eks_node_instance_types  = ["t3.large"]  # 2 vCPU, 8 GB RAM
eks_node_desired_size    = 2
eks_node_min_size        = 1
eks_node_max_size        = 5
