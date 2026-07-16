#!/usr/bin/env bash
# Restaura un respaldo generado por backup.sh.
#
# Uso:  ./scripts/restore.sh backups/cineclub_20260716_020000.sql.gz
#       ./scripts/restore.sh --ultimo

set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DIR_BACKUPS="${RAIZ}/backups"
CONTENEDOR="${CONTENEDOR_DB:-cineclub-db}"

if [[ -f "${RAIZ}/.env" ]]; then
    set -a
    # shellcheck disable=SC1091
    source "${RAIZ}/.env"
    set +a
else
    echo "ERROR: no se encontro ${RAIZ}/.env" >&2
    exit 1
fi

: "${POSTGRES_DB:?falta POSTGRES_DB en .env}"
: "${POSTGRES_USER:?falta POSTGRES_USER en .env}"

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"; }

if [[ $# -lt 1 ]]; then
    echo "Uso: $0 <archivo.sql.gz> | --ultimo" >&2
    exit 1
fi

if [[ "$1" == "--ultimo" ]]; then
    ARCHIVO="$(find "${DIR_BACKUPS}" -name 'cineclub_*.sql.gz' -type f | sort | tail -n 1)"
    [[ -n "${ARCHIVO}" ]] || { log "ERROR: no hay respaldos en ${DIR_BACKUPS}"; exit 1; }
else
    ARCHIVO="$1"
fi

[[ -f "${ARCHIVO}" ]] || { log "ERROR: no existe el archivo '${ARCHIVO}'"; exit 1; }

if ! docker ps --format '{{.Names}}' | grep -q "^${CONTENEDOR}$"; then
    log "ERROR: el contenedor '${CONTENEDOR}' no esta en ejecucion."
    exit 1
fi

log "ATENCION: se sobrescribira el contenido actual de la base '${POSTGRES_DB}'."
read -r -p "Escribe 'restaurar' para confirmar: " CONFIRMACION
[[ "${CONFIRMACION}" == "restaurar" ]] || { log "Restauracion cancelada."; exit 0; }

log "Restaurando desde ${ARCHIVO}..."

gunzip -c "${ARCHIVO}" | docker exec -i "${CONTENEDOR}" psql \
    --username="${POSTGRES_USER}" \
    --dbname="${POSTGRES_DB}" \
    --quiet

log "Restauracion completada."
log "Reinicia el backend con el perfil 'prod' para que Hibernate no altere el esquema restaurado."
