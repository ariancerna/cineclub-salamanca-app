#!/usr/bin/env bash
# ==========================================================================
#  CineClub Salamanca — Respaldo de la base de datos PostgreSQL
# --------------------------------------------------------------------------
#  Genera un volcado comprimido en backups/ y aplica la politica de retencion
#  eliminando los respaldos mas antiguos que RETENCION_DIAS.
#
#  Uso:      ./scripts/backup.sh
#  Cron:     0 2 * * *  /ruta/al/proyecto/scripts/backup.sh >> /var/log/cineclub-backup.log 2>&1
# ==========================================================================

set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DIR_BACKUPS="${RAIZ}/backups"
RETENCION_DIAS="${RETENCION_DIAS:-30}"
CONTENEDOR="${CONTENEDOR_DB:-cineclub-db}"

# Carga las credenciales desde .env sin exponerlas en la linea de comandos
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

mkdir -p "${DIR_BACKUPS}"

MARCA="$(date +%Y%m%d_%H%M%S)"
DESTINO="${DIR_BACKUPS}/cineclub_${MARCA}.sql.gz"

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"; }

log "Iniciando respaldo de la base '${POSTGRES_DB}'..."

if ! docker ps --format '{{.Names}}' | grep -q "^${CONTENEDOR}$"; then
    log "ERROR: el contenedor '${CONTENEDOR}' no esta en ejecucion."
    exit 1
fi

# --clean permite restaurar sobre una base existente sin recrearla a mano
docker exec "${CONTENEDOR}" pg_dump \
    --username="${POSTGRES_USER}" \
    --dbname="${POSTGRES_DB}" \
    --clean --if-exists \
    | gzip > "${DESTINO}"

# Un volcado valido nunca queda vacio: si lo esta, el respaldo fallo silenciosamente
if [[ ! -s "${DESTINO}" ]]; then
    log "ERROR: el respaldo quedo vacio. Se elimina el archivo."
    rm -f "${DESTINO}"
    exit 1
fi

TAMANO="$(du -h "${DESTINO}" | cut -f1)"
log "Respaldo completado: ${DESTINO} (${TAMANO})"

# Politica de retencion
ELIMINADOS="$(find "${DIR_BACKUPS}" -name 'cineclub_*.sql.gz' -type f -mtime "+${RETENCION_DIAS}" -print -delete | wc -l)"
log "Retencion (${RETENCION_DIAS} dias): ${ELIMINADOS} respaldo(s) antiguo(s) eliminado(s)."
log "Respaldos vigentes: $(find "${DIR_BACKUPS}" -name 'cineclub_*.sql.gz' -type f | wc -l)"
