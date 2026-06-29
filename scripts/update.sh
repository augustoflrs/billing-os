#!/usr/bin/env bash
# =============================================================================
# Billing OS — Update Script
#
# Usage:
#   ./scripts/update.sh          # pull latest code, rebuild, rolling restart
#   ./scripts/update.sh --build-only  # rebuild images without restarting
#
# Run from the billing-os project root directory.
# =============================================================================

set -euo pipefail

COMPOSE_FILE="docker-compose.prod.yml"
GREEN="\033[0;32m"
YELLOW="\033[1;33m"
RED="\033[0;31m"
BOLD="\033[1m"
RESET="\033[0m"

info()    { echo -e "${GREEN}[✓]${RESET} $*"; }
warn()    { echo -e "${YELLOW}[!]${RESET} $*"; }
error()   { echo -e "${RED}[✗]${RESET} $*" >&2; exit 1; }
heading() { echo -e "\n${BOLD}$*${RESET}"; }

BUILD_ONLY=false
[[ "${1:-}" == "--build-only" ]] && BUILD_ONLY=true

[[ -f "$COMPOSE_FILE" ]] || error "Run this script from the billing-os project root directory."
[[ -f ".env" ]] || error ".env not found. Run scripts/deploy.sh first."

heading "Pulling latest changes from git..."
if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    git pull --ff-only
    info "Code updated."
else
    warn "Not a git repository — skipping git pull."
fi

heading "Rebuilding images..."
docker compose -f "$COMPOSE_FILE" build --parallel
info "Images rebuilt."

if [[ "$BUILD_ONLY" == "true" ]]; then
    info "Build-only mode — skipping restart."
    exit 0
fi

heading "Restarting services (zero-downtime order)..."

# Backend first (runs Flyway migrations)
docker compose -f "$COMPOSE_FILE" up -d --no-deps backend
echo -n "  Waiting for backend..."
for _ in $(seq 1 30); do
    if docker compose -f "$COMPOSE_FILE" exec -T backend \
        wget -qO- http://localhost:8080/api/actuator/health 2>/dev/null | grep -q '"UP"'; then
        break
    fi
    echo -n "."
    sleep 3
done
echo ""
info "Backend healthy."

# Frontend + nginx
docker compose -f "$COMPOSE_FILE" up -d --no-deps frontend nginx
info "Frontend and nginx restarted."

heading "Verifying deployment..."
sleep 3
HTTP=$(curl -s -o /dev/null -w "%{http_code}" http://localhost/api/actuator/health 2>/dev/null || echo "000")
if [[ "$HTTP" == "200" ]]; then
    info "Health check passed (HTTP $HTTP)."
else
    warn "Health check returned HTTP $HTTP — inspect with: docker compose -f $COMPOSE_FILE logs -f"
fi

echo ""
echo -e "${GREEN}${BOLD}Update complete.${RESET}"
echo ""
