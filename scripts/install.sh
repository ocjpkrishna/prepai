#!/usr/bin/env bash
# VPS bootstrap for PrepAI (spec 10.1, 9.2 Agent 9). Run as root on Debian 13: sudo ./scripts/install.sh
#
# Idempotent: every step checks first and skips what is already present. It only installs what is
# missing and never upgrades, restarts or reconfigures a shared service. It never touches MongoDB,
# QuestDB, Grafana or the trading services.
#
# Environment (all optional):
#   PREPAI_SERVER_NAME  public host for nginx (default prepai.in)
#   PREPAI_ENV_FILE     secrets file read by systemd (default /etc/prepai/prepai.env)
set -euo pipefail

SERVER_NAME="${PREPAI_SERVER_NAME:-prepai.in}"
ENV_FILE="${PREPAI_ENV_FILE:-/etc/prepai/prepai.env}"
REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
DB_NAME="prepai_prod"
DB_ROLE="prepai_prod"
AUDIO_DIR="/var/lib/prepai/audio"
MODEL_DIR="/var/lib/prepai/models"
LOG_DIR="/var/log/prepai"
WEB_ROOT="/var/www/prepai"

log() { printf '[install] %s\n' "$*"; }
die() { printf '[install] ERROR: %s\n' "$*" >&2; exit 1; }
package_installed() { dpkg -s "$1" >/dev/null 2>&1; }

require_root() {
	[[ "$(id -u)" -eq 0 ]] || die "run as root (sudo ./scripts/install.sh)"
}

install_missing_packages() {
	local missing=()
	local pkg
	for pkg in "$@"; do
		package_installed "$pkg" || missing+=("$pkg")
	done
	if [[ ${#missing[@]} -gt 0 ]]; then
		log "installing: ${missing[*]}"
		apt-get update -qq
		DEBIAN_FRONTEND=noninteractive apt-get install -y -qq "${missing[@]}"
	else
		log "already installed: $*"
	fi
}

check_shared_runtimes() {
	# Java 21, Node 18+ and Redis 8 are shared with other services on this box: only verify them.
	local java_version
	java_version="$(java -version 2>&1 || true)"
	[[ "$java_version" == *'version "21'* ]] || die "Java 21 is required (not installed by this script)"
	node -e 'process.exit(Number(process.versions.node.split(".")[0]) >= 18 ? 0 : 1)' \
		|| die "Node.js 18+ is required (not installed by this script)"
	systemctl is-active --quiet redis-server || log "warning: redis-server is not active"
}

install_postgres() {
	if ! package_installed postgresql-17; then
		install_missing_packages postgresql-17 postgresql-17-pgvector
	fi
	systemctl enable --now postgresql
}

ensure_env_file() {
	if [[ -f "$ENV_FILE" ]]; then
		log "env file exists, keeping it: $ENV_FILE"
		return
	fi
	log "writing $ENV_FILE with generated secrets (mode 600)"
	install -d -m 755 "$(dirname "$ENV_FILE")"
	(umask 077 && cat >"$ENV_FILE" <<EOF
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://localhost:5432/${DB_NAME}
DB_USERNAME=${DB_ROLE}
DB_PASSWORD=$(openssl rand -hex 24)
JWT_SECRET=$(openssl rand -base64 48 | tr -d '\n')
AUDIO_DIR=${AUDIO_DIR}
EMBEDDING_CACHE_DIR=${MODEL_DIR}
LOG_DIR=${LOG_DIR}
APP_BASE_URL=https://${SERVER_NAME}
CORS_ALLOWED_ORIGINS=https://${SERVER_NAME}
# Still to be supplied by the operator (see BUILD-DECISIONS.md, "Needs the user"):
GOOGLE_CLIENT_ID=
CLAUDE_API_KEY=
RAZORPAY_KEY_ID=
RAZORPAY_KEY_SECRET=
RAZORPAY_WEBHOOK_SECRET=
SMTP_HOST=
SMTP_USERNAME=
SMTP_PASSWORD=
EOF
	)
}

env_value() {
	grep -E "^$1=" "$ENV_FILE" | head -n 1 | cut -d= -f2-
}

ensure_database_role() {
	local password role_exists
	password="$(env_value DB_PASSWORD)"
	role_exists="$(runuser -u postgres -- psql -tAc "SELECT 1 FROM pg_roles WHERE rolname='${DB_ROLE}'")"
	if [[ "$role_exists" == "1" ]]; then
		log "database role ${DB_ROLE} exists"
		return
	fi
	log "creating database role ${DB_ROLE}"
	printf "CREATE ROLE %s LOGIN PASSWORD :'pw';\n" "$DB_ROLE" \
		| runuser -u postgres -- psql -v ON_ERROR_STOP=1 -v pw="$password" >/dev/null
}

ensure_database() {
	local db_exists
	db_exists="$(runuser -u postgres -- psql -tAc "SELECT 1 FROM pg_database WHERE datname='${DB_NAME}'")"
	if [[ "$db_exists" != "1" ]]; then
		log "creating database ${DB_NAME}"
		runuser -u postgres -- createdb -O "$DB_ROLE" "$DB_NAME"
	fi
	# The application role cannot create extensions, so the superuser installs pgvector.
	runuser -u postgres -- psql -v ON_ERROR_STOP=1 -d "$DB_NAME" -qc "CREATE EXTENSION IF NOT EXISTS vector"
}

ensure_directories() {
	id -u prepai >/dev/null 2>&1 || adduser --system --group --home /var/lib/prepai prepai
	install -d -o prepai -g prepai -m 750 /var/lib/prepai "$AUDIO_DIR" "$MODEL_DIR" "$LOG_DIR"
	install -d -m 755 "$WEB_ROOT"
}

install_nginx() {
	install_missing_packages nginx certbot python3-certbot-nginx gettext-base
	install -d /etc/nginx/snippets
	install -m 644 "$REPO_DIR/nginx/snippets/prepai-proxy.conf" /etc/nginx/snippets/prepai-proxy.conf
	if [[ ! -d "/etc/letsencrypt/live/${SERVER_NAME}" ]]; then
		log "no certificate for ${SERVER_NAME} yet: run 'certbot certonly --webroot -w ${WEB_ROOT} -d ${SERVER_NAME}', then rerun"
		return
	fi
	sed -e "s|__SERVER_NAME__|${SERVER_NAME}|g" \
		-e "s|__WEB_ROOT__|${WEB_ROOT}|g" \
		-e "s|__AUDIO_DIR__|${AUDIO_DIR}|g" \
		"$REPO_DIR/nginx/prepai.conf.template" >/etc/nginx/sites-available/prepai.conf
	ln -sf /etc/nginx/sites-available/prepai.conf /etc/nginx/sites-enabled/prepai.conf
	nginx -t
	systemctl reload nginx
}

check_voicestudio() {
	if systemctl cat voicestudio.service >/dev/null 2>&1; then
		log "VoiceStudio service is present"
	else
		log "VoiceStudio is not installed: its source is not in this repository (see BUILD-DECISIONS.md)"
	fi
}

main() {
	require_root
	check_shared_runtimes
	install_postgres
	ensure_env_file
	ensure_database_role
	ensure_database
	ensure_directories
	install_nginx
	check_voicestudio
	log "done. Next: fill the empty secrets in $ENV_FILE and run ./gradlew -p prepai bootRun or the systemd unit"
}

main "$@"
