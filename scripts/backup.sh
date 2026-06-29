#!/usr/bin/env bash
# =============================================================================
# Billing OS — Backup Script
#
# Usage:
#   ./scripts/backup.sh               # backup to ./backups/
#   ./scripts/backup.sh /path/to/dir  # backup to a specific directory
#
# Creates a timestamped PostgreSQL dump. Run on a cron schedule for automated
# backups, e.g.:  0 2 * * * /opt/billing-os/scripts/backup.sh /data/backups
# =============================================================================

set -euo pipefail

COMPOSE_FILE="docker-compose.prod.yml"
BACKUP_DIR="${1:-./backups}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
GREEN="\033[0;32m"
RED="\033[0;31m"
BOLD="\033[1m"
RESET="\033[0m"

info()  { echo -e "${GREEN}[✓]${RESET} $*"; }
error() { echo -e "${RED}[✗]${RESET} $*" >&2; exit 1; }

[[ -f "$COMPOSE_FILE" ]] || error "Run from the billing-os project root directory."
[[ -f ".env" ]]          || error ".env not found."

# shellcheck disable=SC1091
set -a; source .env; set +a

mkdir -p "$BACKUP_DIR"

DUMP_FILE="${BACKUP_DIR}/billingos_${TIMESTAMP}.sql.gz"

echo -e "${BOLD}Backing up database to ${DUMP_FILE}...${RESET}"

docker compose -f "$COMPOSE_FILE" exec -T postgres \
    pg_dump -U "$DB_USER" "$DB_NAME" | gzip > "$DUMP_FILE"

SIZE=$(du -sh "$DUMP_FILE" | cut -f1)
info "Database backup complete: $DUMP_FILE ($SIZE)"

# Keep last 30 backups, remove older ones
KEPT=30
REMOVED=$(find "$BACKUP_DIR" -name "billingos_*.sql.gz" -printf '%T@ %p\n' \
    | sort -rn | tail -n +$((KEPT + 1)) | awk '{print $2}')

if [[ -n "$REMOVED" ]]; then
    echo "$REMOVED" | xargs rm -f
    info "Removed $(echo "$REMOVED" | wc -l) old backup(s)."
fi

echo ""
echo -e "${GREEN}${BOLD}Backup complete.${RESET}  Restore with:"
echo "  gunzip -c $DUMP_FILE | docker compose -f $COMPOSE_FILE exec -T postgres psql -U \$DB_USER \$DB_NAME"
echo ""
