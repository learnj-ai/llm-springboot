#!/bin/bash
set -euo pipefail

# Create per-module databases so each workshop module is isolated.
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE workshop_module03;
    CREATE DATABASE workshop_module04;
EOSQL

# Module 03 schema + seed.
if [ -f /sql-init/module-03-schema.sql ]; then
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname workshop_module03 \
        -f /sql-init/module-03-schema.sql
fi
if [ -f /sql-init/module-03-data.sql ]; then
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname workshop_module03 \
        -f /sql-init/module-03-data.sql
fi

# Module 04 seed.
if [ -f /sql-init/module-04-init.sql ]; then
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname workshop_module04 \
        -f /sql-init/module-04-init.sql
fi
