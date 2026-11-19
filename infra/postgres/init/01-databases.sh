#!/bin/bash
# One database + one role per service; no cross-connect. Runs once, on first init of the data volume.
set -euo pipefail

# name:password-env-var
SERVICES="identity catalog review notification loan catalog_query"

for db in $SERVICES; do
  var="$(echo "${db}" | tr '[:lower:]' '[:upper:]')_DB_PASSWORD"
  pass="${!var:-$db}"
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres \
       -v role="$db" -v pass="$pass" <<'SQL'
SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'role', :'pass') \gexec
SELECT format('CREATE DATABASE %I OWNER %I', :'role', :'role') \gexec
SELECT format('REVOKE CONNECT ON DATABASE %I FROM PUBLIC', :'role') \gexec
SQL
done
