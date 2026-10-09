#!/usr/bin/env bash
# Pushes PrepAI's dashboards, alert rules and Prometheus data source into the shared Grafana (spec 8.4).
#
# Idempotent. It only creates or overwrites items inside the "PrepAI" folder and its own data source
# "prepai-prometheus". It never edits other folders, data sources, the notification policy or Grafana itself.
#
# Usage:
#   GRAFANA_URL=https://algorithmyc.com/gfn GRAFANA_API_TOKEN=... ./scripts/grafana-sync.sh
#   ./scripts/grafana-sync.sh --dry-run      print the payloads, contact nothing
#
# The token is a service account limited to the PrepAI folder (TODO-5). It is read from the environment
# only and passed to curl through stdin, so it never shows in the process list.
set -euo pipefail

REPO_DIR="$(cd "$(dirname "$0")/.." && pwd)"
DASHBOARD_DIR="$REPO_DIR/ops/grafana/dashboards"
RULES_FILE="$REPO_DIR/ops/grafana/alert-rules.json"
FOLDER_TITLE="PrepAI"
DATASOURCE_UID="prepai-prometheus"
PROMETHEUS_URL="http://localhost:9095"
RULE_GROUP="prepai"
FOLDER_UID=""
REPLY_CODE=""
REPLY_BODY=""

log() { printf '[grafana-sync] %s\n' "$*"; }
die() { printf '[grafana-sync] ERROR: %s\n' "$*" >&2; exit 1; }

require_tools() {
	command -v curl >/dev/null || die "curl is required"
	command -v jq >/dev/null || die "jq is required"
}

require_env() {
	[[ -n "${GRAFANA_URL:-}" ]] || die "GRAFANA_URL is not set"
	[[ -n "${GRAFANA_API_TOKEN:-}" ]] || die "GRAFANA_API_TOKEN is not set"
}

# request METHOD PATH [JSON]: sets REPLY_CODE and REPLY_BODY.
request() {
	local method="$1" path="$2" data="${3-}" raw
	local args=(-sS -X "$method" -H "Accept: application/json" -H "Content-Type: application/json"
		-w '\n%{http_code}' "${GRAFANA_URL%/}$path")
	[[ -z "$data" ]] || args+=(--data-binary "$data")
	raw="$(printf 'header = "Authorization: Bearer %s"\n' "$GRAFANA_API_TOKEN" | curl --config - "${args[@]}")"
	REPLY_CODE="${raw##*$'\n'}"
	REPLY_BODY="${raw%$'\n'*}"
}

expect_success() {
	[[ "$REPLY_CODE" =~ ^2 ]] || die "$1 failed with HTTP $REPLY_CODE: ${REPLY_BODY:0:200}"
}

ensure_folder() {
	request GET "/api/search?type=dash-folder&query=$FOLDER_TITLE"
	expect_success "folder search"
	FOLDER_UID="$(jq -r --arg t "$FOLDER_TITLE" '[.[] | select(.title == $t)][0].uid // empty' <<<"$REPLY_BODY")"
	if [[ -z "$FOLDER_UID" ]]; then
		log "creating folder $FOLDER_TITLE"
		request POST /api/folders "$(jq -nc --arg t "$FOLDER_TITLE" '{title: $t}')"
		expect_success "folder create"
		FOLDER_UID="$(jq -r '.uid' <<<"$REPLY_BODY")"
	fi
	log "folder $FOLDER_TITLE is $FOLDER_UID"
}

datasource_body() {
	jq -nc --arg uid "$DATASOURCE_UID" --arg url "$PROMETHEUS_URL" \
		'{name: $uid, uid: $uid, type: "prometheus", access: "proxy", url: $url, isDefault: false}'
}

ensure_datasource() {
	request GET "/api/datasources/uid/$DATASOURCE_UID"
	if [[ "$REPLY_CODE" == "200" ]]; then
		log "data source $DATASOURCE_UID exists, keeping it"
		return
	fi
	[[ "$REPLY_CODE" == "404" ]] || die "data source lookup failed with HTTP $REPLY_CODE"
	log "creating data source $DATASOURCE_UID"
	request POST /api/datasources "$(datasource_body)"
	expect_success "data source create"
}

dashboard_payload() {
	jq -c --arg folder "$FOLDER_UID" '{dashboard: (. + {id: null}), folderUid: $folder, overwrite: true}' "$1"
}

sync_dashboard() {
	local file="$1" uid folder
	uid="$(jq -r '.uid' "$file")"
	request GET "/api/dashboards/uid/$uid"
	if [[ "$REPLY_CODE" == "200" ]]; then
		folder="$(jq -r '.meta.folderUid // ""' <<<"$REPLY_BODY")"
		[[ "$folder" == "$FOLDER_UID" ]] || die "dashboard $uid is outside the $FOLDER_TITLE folder, refusing to overwrite it"
	fi
	request POST /api/dashboards/db "$(dashboard_payload "$file")"
	expect_success "dashboard $uid"
	log "dashboard $uid synced"
}

# rule_payload JSON: one entry of alert-rules.json as a Grafana provisioning body (Prometheus query + threshold).
rule_payload() {
	jq -c --arg folder "$FOLDER_UID" --arg ds "$DATASOURCE_UID" --arg group "$RULE_GROUP" '{
		uid: .uid,
		title: .title,
		folderUid: $folder,
		ruleGroup: $group,
		condition: "B",
		noDataState: "NoData",
		execErrState: "Error",
		for: .for,
		labels: {app: "prepai", severity: .severity},
		annotations: {summary: .summary},
		data: [
			{refId: "A", relativeTimeRange: {from: 3600, to: 0}, datasourceUid: $ds,
			 model: {refId: "A", datasource: {type: "prometheus", uid: $ds}, expr: .expr,
			         instant: true, intervalMs: 1000, maxDataPoints: 43200}},
			{refId: "B", relativeTimeRange: {from: 0, to: 0}, datasourceUid: "__expr__",
			 model: {refId: "B", type: "threshold", datasource: {type: "__expr__", uid: "__expr__"},
			         expression: "A", conditions: [{evaluator: {params: [.threshold], type: .op}}]}}
		]
	}' <<<"$1"
}

rule_folder_uid() {
	jq -r --arg uid "$2" '.[] | select(.uid == $uid) | .folderUid' <<<"$1"
}

sync_rule() {
	local rule="$1" existing="$2" payload uid current_folder
	payload="$(rule_payload "$rule")"
	uid="$(jq -r '.uid' <<<"$rule")"
	current_folder="$(rule_folder_uid "$existing" "$uid")"
	if [[ -z "$current_folder" ]]; then
		request POST /api/v1/provisioning/alert-rules "$payload"
		expect_success "alert rule $uid create"
	else
		[[ "$current_folder" == "$FOLDER_UID" ]] || die "alert rule $uid is outside the $FOLDER_TITLE folder, refusing to overwrite it"
		request PUT "/api/v1/provisioning/alert-rules/$uid" "$payload"
		expect_success "alert rule $uid update"
	fi
	log "alert rule $uid synced"
}

sync_alert_rules() {
	local existing rule
	local rules=()
	request GET /api/v1/provisioning/alert-rules
	expect_success "alert rule list"
	existing="$REPLY_BODY"
	mapfile -t rules < <(jq -c '.[]' "$RULES_FILE")
	for rule in "${rules[@]}"; do
		sync_rule "$rule" "$existing"
	done
}

dry_run() {
	FOLDER_UID="<folder-uid>"
	local file rule
	local rules=()
	for file in "$DASHBOARD_DIR"/*.json; do
		dashboard_payload "$file"
	done
	mapfile -t rules < <(jq -c '.[]' "$RULES_FILE")
	for rule in "${rules[@]}"; do
		rule_payload "$rule"
	done
}

sync_dashboards() {
	local file
	for file in "$DASHBOARD_DIR"/*.json; do
		sync_dashboard "$file"
	done
}

main() {
	require_tools
	if [[ "${1-}" == "--dry-run" ]]; then
		dry_run
		return
	fi
	require_env
	ensure_folder
	ensure_datasource
	sync_dashboards
	sync_alert_rules
	log "done"
}

main "$@"
