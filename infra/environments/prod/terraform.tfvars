# Production Environment Configuration

# General
environment  = "prod"
aws_region   = "us-east-1"
cost_center  = "beema-platform"
project_name = "beema"

# VPC
vpc_cidr = "10.0.0.0/16"
availability_zones = [
  "us-east-1a",
  "us-east-1b",
  "us-east-1c"
]

# Database - Production Grade
db_name             = "beema_prod"
db_username         = "beema_admin"
db_instance_class   = "db.r6g.xlarge"  # 4 vCPU, 32 GB RAM
db_allocated_storage = 500  # 500 GB, auto-scales up to 5 TB

# EKS - Production Grade
eks_cluster_version      = "1.28"
eks_node_instance_types  = ["m5.xlarge"]  # 4 vCPU, 16 GB RAM
eks_node_desired_size    = 6
eks_node_min_size        = 4
eks_node_max_size        = 20
