# EKS Module Variables

variable "project_name" {
  description = "Project name for resource naming"
  type        = string
}

variable "environment" {
  description = "Environment name"
  type        = string
}

variable "vpc_id" {
  description = "VPC ID"
  type        = string
}

variable "private_subnet_ids" {
  description = "List of private subnet IDs for node groups"
  type        = list(string)
}

variable "public_subnet_ids" {
  description = "List of public subnet IDs for load balancers"
  type        = list(string)
}

# Cluster Configuration
variable "cluster_version" {
  description = "Kubernetes version"
  type        = string
}

variable "cluster_endpoint_public_access" {
  description = "Enable public access to cluster endpoint"
  type        = bool
  default     = true
}

variable "cluster_endpoint_access_cidrs" {
  description = "CIDR blocks allowed to access cluster endpoint"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

# Node Group Configuration
variable "node_instance_types" {
  description = "Instance types for node group"
  type        = list(string)
}

variable "node_desired_size" {
  description = "Desired number of nodes"
  type        = number
}

variable "node_min_size" {
  description = "Minimum number of nodes"
  type        = number
}

variable "node_max_size" {
  description = "Maximum number of nodes"
  type        = number
}

variable "node_disk_size" {
  description = "Disk size for nodes in GB"
  type        = number
  default     = 100
}

variable "node_capacity_type" {
  description = "Node capacity type (ON_DEMAND or SPOT)"
  type        = string
  default     = "ON_DEMAND"
}

# Add-on Versions
variable "vpc_cni_version" {
  description = "VPC CNI add-on version"
  type        = string
  default     = null  # Use latest
}

variable "coredns_version" {
  description = "CoreDNS add-on version"
  type        = string
  default     = null  # Use latest
}

variable "kube_proxy_version" {
  description = "kube-proxy add-on version"
  type        = string
  default     = null  # Use latest
}
