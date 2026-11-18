#!/bin/bash
set -euo pipefail
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" \
    --set=usuarios_password="$ACIS_USUARIOS_DB_PASSWORD" \
    --set=operaciones_password="$ACIS_OPERACIONES_DB_PASSWORD" <<'SQL'
CREATE USER acis_usuarios WITH PASSWORD :'usuarios_password';
CREATE DATABASE acis_usuarios OWNER acis_usuarios;
REVOKE CONNECT ON DATABASE acis_usuarios FROM PUBLIC;
GRANT CONNECT ON DATABASE acis_usuarios TO acis_usuarios;
CREATE USER acis_operaciones WITH PASSWORD :'operaciones_password';
CREATE DATABASE acis_operaciones OWNER acis_operaciones;
REVOKE CONNECT ON DATABASE acis_operaciones FROM PUBLIC;
GRANT CONNECT ON DATABASE acis_operaciones TO acis_operaciones;
SQL
