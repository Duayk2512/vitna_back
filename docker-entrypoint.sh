#!/bin/sh
set -eu

fail() { printf '%s\n' "VITNA: $1" >&2; exit 1; }
[ "${SPRING_PROFILES_ACTIVE:-prod}" = "prod" ] || fail 'El contenedor requiere SPRING_PROFILES_ACTIVE=prod.'
export SPRING_PROFILES_ACTIVE=prod
[ -n "${DB_URL:-}" ] || fail 'Falta DB_URL: configurar jdbc:postgresql://HOST:PUERTO/BASE con referencias a PostgreSQL en Railway.'
case "$DB_URL" in
  jdbc:postgresql://localhost*|jdbc:postgresql://127.*|jdbc:postgresql://\[::1\]*) fail 'DB_URL apunta al contenedor local. Usar el host del servicio PostgreSQL.' ;;
  jdbc:postgresql://*) ;;
  *) fail 'DB_URL debe comenzar con jdbc:postgresql://; DATABASE_URL de Railway no es una URL JDBC.' ;;
esac
[ -n "${DB_USER:-}" ] || fail 'Falta DB_USER: referenciar PGUSER del servicio PostgreSQL.'
[ -n "${DB_PASSWORD:-}" ] || fail 'Falta DB_PASSWORD: referenciar PGPASSWORD del servicio PostgreSQL.'
if [ -n "${PORT:-}" ]; then
  case "$PORT" in *[!0-9]*) fail 'PORT debe ser numérico.' ;; esac
  [ "$PORT" -gt 0 ] && [ "$PORT" -le 65535 ] || fail 'PORT debe estar entre 1 y 65535.'
fi
exec java -jar /app/vitna.jar "$@"
