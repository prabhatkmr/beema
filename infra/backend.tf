# OpenTofu Remote State Backend Configuration
# Uses S3 for state storage with DynamoDB for state locking

terraform {
  backend "s3" {
    bucket         = "beema-terraform-state"
    key            = "beema/infrastructure/terraform.tfstate"
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "beema-terraform-locks"

    # Enable versioning for state recovery
    versioning = true

    # Server-side encryption
    kms_key_id = "alias/terraform-state-key"
  }
}

# Note: Before running `tofu init`, create these resources manually:
#
# 1. S3 Bucket:
#    aws s3api create-bucket --bucket beema-terraform-state --region us-east-1
#    aws s3api put-bucket-versioning --bucket beema-terraform-state --versioning-configuration Status=Enabled
#    aws s3api put-bucket-encryption --bucket beema-terraform-state --server-side-encryption-configuration \
#      '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"}}]}'
#
# 2. DynamoDB Table:
#    aws dynamodb create-table \
#      --table-name beema-terraform-locks \
#      --attribute-definitions AttributeName=LockID,AttributeType=S \
#      --key-schema AttributeName=LockID,KeyType=HASH \
#      --billing-mode PAY_PER_REQUEST \
#      --region us-east-1
#
# 3. KMS Key (optional):
#    aws kms create-alias --alias-name alias/terraform-state-key --target-key-id <key-id>
