#!/usr/bin/env bash
# Set up Flink S3 plugin for MinIO integration.
#
# NOTE: In the docker-compose setup, the S3 plugin is automatically configured
# via the container command (copies flink-s3-fs-hadoop from /opt/flink/opt/).
# This script is for manual/bare-metal Flink installations.
#
# Usage: FLINK_HOME=/path/to/flink ./scripts/setup-flink-s3.sh

set -euo pipefail

FLINK_HOME="${FLINK_HOME:-/opt/flink}"
PLUGIN_DIR="$FLINK_HOME/plugins/s3-fs-hadoop"

echo "Setting up Flink S3 plugin..."
echo "  FLINK_HOME: $FLINK_HOME"

# Create the plugin directory
mkdir -p "$PLUGIN_DIR"

# Copy the bundled S3 filesystem plugin to the plugins directory
# Flink ships with this JAR in /opt/flink/opt/ but it must be in plugins/ to be loaded
S3_JAR=$(ls "$FLINK_HOME/opt"/flink-s3-fs-hadoop-*.jar 2>/dev/null | head -1)

if [ -z "$S3_JAR" ]; then
  echo "ERROR: flink-s3-fs-hadoop JAR not found in $FLINK_HOME/opt/"
  echo "  Ensure you are using an official Flink distribution (1.17+)."
  exit 1
fi

cp "$S3_JAR" "$PLUGIN_DIR/"
echo "  Installed: $(basename "$S3_JAR") -> $PLUGIN_DIR/"

# Append S3 configuration to flink-conf.yaml if not already present
FLINK_CONF="$FLINK_HOME/conf/flink-conf.yaml"
if [ -f "$FLINK_CONF" ] && ! grep -q "s3.endpoint" "$FLINK_CONF"; then
  cat >> "$FLINK_CONF" <<'CONF'

# MinIO S3 Configuration (added by setup-flink-s3.sh)
s3.endpoint: http://localhost:9000
s3.path.style.access: true
s3.access-key: admin
s3.secret-key: password123
CONF
  echo "  Appended S3 configuration to $FLINK_CONF"
else
  echo "  S3 configuration already present in $FLINK_CONF (or file not found), skipping."
fi

echo ""
echo "Flink S3 plugin setup complete!"
echo ""
echo "Verify with:"
echo "  $FLINK_HOME/bin/flink run --help  # Should start without ClassNotFoundException"
echo ""
echo "Checkpoint/savepoint paths can now use:"
echo "  s3://beema-checkpoints/checkpoints"
echo "  s3://beema-checkpoints/savepoints"
echo "  s3://beema-datalake/..."
