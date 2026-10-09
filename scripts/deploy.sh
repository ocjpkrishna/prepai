#!/usr/bin/env bash
# Deploys the latest PrepAI build (spec 9.2 Agent 9). Run as root on the VPS: sudo ./scripts/deploy.sh
#
# Steps: pull the branch, build the boot jar, keep it as a new release, point app.jar at it, restart
# prepai.service, and wait for the health check on 127.0.0.1:9091. If the health check fails, the previous
# release is restored and the script exits with an error.
#
# Environment (all optional):
#   PREPAI_DEPLOY_BRANCH   branch to deploy (default main)
#   PREPAI_HEALTH_TIMEOUT  seconds to wait for health (default 60)
set -euo pipefail

REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
BRANCH="${PREPAI_DEPLOY_BRANCH:-main}"
HEALTH_TIMEOUT="${PREPAI_HEALTH_TIMEOUT:-60}"
HEALTH_URL="http://127.0.0.1:9091/actuator/health"
DEPLOY_ROOT="/opt/prepai"
RELEASE_DIR="$DEPLOY_ROOT/releases"
CURRENT_JAR="$DEPLOY_ROOT/app.jar"
UNIT="prepai.service"
KEEP_RELEASES=5

log() { printf '[deploy] %s\n' "$*"; }
die() { printf '[deploy] ERROR: %s\n' "$*" >&2; exit 1; }

require_root() {
	[[ "$(id -u)" -eq 0 ]] || die "run as root (sudo ./scripts/deploy.sh)"
}

pull_latest() {
	log "pulling origin/$BRANCH"
	git -C "$REPO_DIR" pull --ff-only origin "$BRANCH"
}

build_jar() {
	nice -n 15 "$REPO_DIR/prepai/gradlew" -p "$REPO_DIR/prepai" -q bootJar
	find "$REPO_DIR/prepai/build/libs" -maxdepth 1 -name '*.jar' ! -name '*-plain.jar' | head -n 1
}

install_release() {
	local jar="$1" release
	release="$RELEASE_DIR/$(date -u +%Y%m%dT%H%M%SZ)-$(git -C "$REPO_DIR" rev-parse --short HEAD).jar"
	install -d -m 755 "$RELEASE_DIR"
	install -m 644 "$jar" "$release"
	printf '%s\n' "$release"
}

current_release() {
	[[ -L "$CURRENT_JAR" ]] && readlink -f "$CURRENT_JAR" || true
}

switch_to() {
	ln -sfn "$1" "$CURRENT_JAR.next"
	mv -Tf "$CURRENT_JAR.next" "$CURRENT_JAR"
}

health_ok() {
	curl -fsS "$HEALTH_URL" 2>/dev/null | grep -q '"UP"'
}

restart_and_wait() {
	local waited=0
	systemctl enable "$UNIT" >/dev/null
	systemctl restart "$UNIT"
	until health_ok; do
		(( waited >= HEALTH_TIMEOUT )) && return 1
		sleep 1
		waited=$((waited + 1))
	done
	log "health is UP after ${waited}s"
}

rollback() {
	local previous="$1"
	[[ -n "$previous" ]] || die "health check failed and there is no previous release to restore"
	log "health check failed, restoring $previous"
	switch_to "$previous"
	systemctl restart "$UNIT"
	die "deploy failed, the previous release is running again"
}

prune_releases() {
	find "$RELEASE_DIR" -maxdepth 1 -name '*.jar' -printf '%T@ %p\n' \
		| sort -rn | tail -n "+$((KEEP_RELEASES + 1))" | cut -d' ' -f2- | xargs -r rm -f --
}

main() {
	require_root
	local previous jar release
	previous="$(current_release)"
	pull_latest
	jar="$(build_jar)"
	[[ -n "$jar" ]] || die "bootJar produced no jar"
	release="$(install_release "$jar")"
	switch_to "$release"
	restart_and_wait || rollback "$previous"
	prune_releases
	log "deployed $(basename "$release")"
}

main "$@"
