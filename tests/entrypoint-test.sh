#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
fixture=$(mktemp -d)
trap 'rm -rf "$fixture"' EXIT
printf '#!/bin/sh\nexit 0\n' > "$fixture/java"
chmod +x "$fixture/java"
export PATH="$fixture:$PATH"
unset DB_URL DB_USER DB_PASSWORD SPRING_PROFILES_ACTIVE PORT
reject() { if sh docker-entrypoint.sh > "$fixture/output" 2>&1; then echo 'Expected configuration rejection' >&2; exit 1; fi; }
reject
grep -q 'Falta DB_URL' "$fixture/output"
export DB_URL=jdbc:postgresql://localhost:5432/test
reject
grep -q 'contenedor local' "$fixture/output"
export DB_URL=postgresql://db:5432/test
reject
grep -q 'URL JDBC' "$fixture/output"
export DB_URL=jdbc:postgresql://db:5432/test DB_USER=test DB_PASSWORD=synthetic-test-only
sh docker-entrypoint.sh
export SPRING_PROFILES_ACTIVE=demo
reject
export SPRING_PROFILES_ACTIVE=prod PORT=invalid
reject
export PORT=65536
reject
export PORT=9090
sh docker-entrypoint.sh
unset DB_PASSWORD
reject
grep -q 'Falta DB_PASSWORD' "$fixture/output"
echo 'Entrypoint configuration checks passed'
