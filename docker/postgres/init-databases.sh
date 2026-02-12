#!/bin/bash
set -e

# Create keycloak database if it doesn't exist
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    SELECT 'CREATE DATABASE keycloak'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'keycloak')\gexec
EOSQL

# Create metadata database if it doesn't exist
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    SELECT 'CREATE DATABASE beema_metadata'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'beema_metadata')\gexec
EOSQL

echo "Databases created successfully: keycloak, beema_metadata"
