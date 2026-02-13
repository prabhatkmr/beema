#!/usr/bin/env bash
# Initialize MinIO with default buckets for local development.
# Usage: ./scripts/init-minio.sh
#
# Prerequisites: MinIO must be running (docker-compose up -d minio)

set -euo pipefail

MINIO_ENDPOINT="http://localhost:9000"
MINIO_ROOT_USER="admin"
MINIO_ROOT_PASSWORD="password123"
ALIAS_NAME="beema-local"

# Default buckets to create
BUCKETS=("beema-exports" "beema-documents" "beema-attachments" "beema-datalake" "beema-checkpoints")

echo "Configuring MinIO client alias..."

# Use Docker-based mc if mc is not installed locally
if command -v mc &> /dev/null; then
  MC="mc"
else
  echo "MinIO client (mc) not found locally, using Docker..."
  MC="docker run --rm --net=host minio/mc"
fi

$MC alias set "$ALIAS_NAME" "$MINIO_ENDPOINT" "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD"

for BUCKET in "${BUCKETS[@]}"; do
  if $MC ls "$ALIAS_NAME/$BUCKET" &> /dev/null; then
    echo "Bucket '$BUCKET' already exists, skipping."
  else
    $MC mb "$ALIAS_NAME/$BUCKET"
    echo "Created bucket: $BUCKET"
  fi
done

echo ""
echo "MinIO initialized successfully!"
echo "  API endpoint:  $MINIO_ENDPOINT"
echo "  Console:       http://localhost:9001"
echo "  Credentials:   $MINIO_ROOT_USER / $MINIO_ROOT_PASSWORD"
echo "  Buckets:       ${BUCKETS[*]}"
