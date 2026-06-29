#!/usr/bin/env bash
# =============================================================================
# Billing OS — Client Onboarding Deploy Script
#
# Usage:
#   ./scripts/deploy.sh              # fresh install (interactive)
#   ./scripts/deploy.sh --no-prompt  # non-interactive (requires .env to exist)
#
# Requirements: docker >= 24, docker compose v2, openssl
# Run as a user with docker permissions (or root).
# =============================================================================

set -euo pipefail

COMPOSE_FILE="docker-compose.prod.yml"
ENV_FILE=".env"
BOLD="\033[1m"
GREEN="\033[0;32m"
YELLOW="\033[1;33m"
RED="\033[0;31m"
RESET="\033[0m"

info()    { echo -e "${GREEN}[✓]${RESET} $*"; }
warn()    { echo -e "${YELLOW}[!]${RESET} $*"; }
error()   { echo -e "${RED}[✗]${RESET} $*" >&2; exit 1; }
heading() { echo -e "\n${BOLD}$*${RESET}"; }

NO_PROMPT=false
[[ "${1:-}" == "--no-prompt" ]] && NO_PROMPT=true

# ── Prereq checks ────────────────────────────────────────────────────────────

heading "Checking prerequisites..."

command -v docker   >/dev/null 2>&1 || error "docker is not installed."
docker compose version >/dev/null 2>&1 || error "docker compose v2 is not installed."
command -v openssl  >/dev/null 2>&1 || error "openssl is not installed."

info "docker $(docker --version | awk '{print $3}' | tr -d ',')"
info "docker compose $(docker compose version --short)"

# ── .env setup ───────────────────────────────────────────────────────────────

heading "Environment configuration..."

if [[ -f "$ENV_FILE" && "$NO_PROMPT" == "false" ]]; then
    warn ".env already exists."
    read -r -p "  Overwrite it? [y/N] " overwrite
    [[ "$overwrite" =~ ^[Yy]$ ]] || { info "Keeping existing .env."; }
fi

if [[ ! -f "$ENV_FILE" ]]; then
    if [[ "$NO_PROMPT" == "true" ]]; then
        error ".env file not found. Run without --no-prompt to create it interactively."
    fi

    heading "Creating .env from .env.example..."
    cp .env.example "$ENV_FILE"

    # Generate strong random secrets
    JWT_SECRET=$(openssl rand -hex 32)
    DB_PASS=$(openssl rand -hex 16)
    MINIO_SECRET=$(openssl rand -hex 16)

    # Substitute generated values
    sed -i "s|JWT_SECRET=.*|JWT_SECRET=${JWT_SECRET}|" "$ENV_FILE"
    sed -i "s|DB_PASS=.*|DB_PASS=${DB_PASS}|" "$ENV_FILE"
    sed -i "s|MINIO_SECRET_KEY=.*|MINIO_SECRET_KEY=${MINIO_SECRET}|" "$ENV_FILE"

    echo ""
    echo -e "${BOLD}Customize the remaining values in .env:${RESET}"
    echo ""
    echo "  ADMIN_USERNAME   — first-login admin account name"
    echo "  ADMIN_PASSWORD   — set a strong password for the admin account"
    echo "  MH_API_URL       — https://apidte.mh.gob.sv  (production)"
    echo "                     https://apitest.dtes.mh.gob.sv  (testing)"
    echo "  DTE_CERTIFICATE_PASSWORD — password for the PKCS12 DTE certificate"
    echo ""
    read -r -p "  Press ENTER when you have finished editing .env..." _

    # Re-read to pick up any manual edits
    # shellcheck disable=SC1090
    source "$ENV_FILE"
fi

# Source .env for validation
set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

# ── Validate required vars ────────────────────────────────────────────────────

heading "Validating configuration..."

REQUIRED_VARS=(DB_NAME DB_USER DB_PASS MINIO_ACCESS_KEY MINIO_SECRET_KEY MINIO_BUCKET JWT_SECRET ADMIN_USERNAME ADMIN_PASSWORD MH_API_URL)
for var in "${REQUIRED_VARS[@]}"; do
    val="${!var:-}"
    if [[ -z "$val" ]]; then
        error "Required variable $var is not set in $ENV_FILE"
    fi
    # Warn on placeholder values
    if [[ "$val" == *"change-me"* ]]; then
        error "$var still contains a placeholder value. Set it in $ENV_FILE before deploying."
    fi
done

if [[ ${#JWT_SECRET} -lt 32 ]]; then
    error "JWT_SECRET must be at least 32 characters."
fi

info "All required variables present."

# ── Build images ──────────────────────────────────────────────────────────────

heading "Building Docker images..."
docker compose -f "$COMPOSE_FILE" build --parallel
info "Images built."

# ── Start infrastructure (postgres + minio) ───────────────────────────────────

heading "Starting database and storage..."
docker compose -f "$COMPOSE_FILE" up -d postgres minio

echo -n "  Waiting for postgres..."
until docker compose -f "$COMPOSE_FILE" exec -T postgres pg_isready -U "$DB_USER" -q 2>/dev/null; do
    echo -n "."
    sleep 2
done
echo ""
info "PostgreSQL is ready."

echo -n "  Waiting for MinIO..."
until docker compose -f "$COMPOSE_FILE" exec -T minio mc ready local >/dev/null 2>&1; do
    echo -n "."
    sleep 2
done
echo ""
info "MinIO is ready."

# ── Start backend (runs Flyway migrations on startup) ─────────────────────────

heading "Starting backend (running database migrations)..."
docker compose -f "$COMPOSE_FILE" up -d backend

echo -n "  Waiting for backend health check..."
BACKEND_READY=false
for _ in $(seq 1 30); do
    if docker compose -f "$COMPOSE_FILE" exec -T backend \
        wget -qO- http://localhost:8080/api/actuator/health 2>/dev/null | grep -q '"UP"'; then
        BACKEND_READY=true
        break
    fi
    echo -n "."
    sleep 3
done
echo ""

if [[ "$BACKEND_READY" == "false" ]]; then
    warn "Backend did not become healthy within 90s. Check logs:"
    docker compose -f "$COMPOSE_FILE" logs --tail=50 backend
    error "Deploy aborted."
fi

info "Backend is healthy."

# ── Start frontend + nginx ────────────────────────────────────────────────────

heading "Starting frontend and nginx..."
docker compose -f "$COMPOSE_FILE" up -d frontend nginx
info "All services started."

# ── Final health check ────────────────────────────────────────────────────────

heading "Final health check..."
sleep 5

HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost/ 2>/dev/null || echo "000")
if [[ "$HTTP_STATUS" == "200" ]]; then
    info "nginx → frontend: HTTP $HTTP_STATUS"
else
    warn "nginx returned HTTP $HTTP_STATUS (may need a moment to stabilise)"
fi

API_STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost/api/actuator/health 2>/dev/null || echo "000")
if [[ "$API_STATUS" == "200" ]]; then
    info "nginx → backend API: HTTP $API_STATUS"
else
    warn "API returned HTTP $API_STATUS"
fi

# ── Summary ───────────────────────────────────────────────────────────────────

echo ""
echo -e "${BOLD}═══════════════════════════════════════════${RESET}"
echo -e "${GREEN}${BOLD}  Billing OS deployed successfully!${RESET}"
echo -e "${BOLD}═══════════════════════════════════════════${RESET}"
echo ""
echo "  URL          : http://$(hostname -I | awk '{print $1}' 2>/dev/null || echo 'localhost')"
echo "  Admin user   : ${ADMIN_USERNAME}"
echo ""
echo "  Next steps:"
echo "   1. Open the URL above and log in"
echo "   2. Go to Configuración → Empresa and fill in company details"
echo "   3. Go to Configuración → Certificados and upload your DTE certificate (.p12)"
echo "   4. Go to Configuración → Sucursales and create a branch + point of sale"
echo "   5. Issue your first invoice"
echo ""
echo "  Logs:  docker compose -f $COMPOSE_FILE logs -f"
echo "  Stop:  docker compose -f $COMPOSE_FILE down"
echo ""
