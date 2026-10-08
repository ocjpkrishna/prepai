#!/usr/bin/env bash
# Unattended build driver (see BUILD-RUNBOOK.md, "Unattended mode").
# Runs one fresh, small Claude session per runbook row, on the model the row names, until every row
# is done, a row is blocked, or a stop is requested. It spends only what the Claude plan allows:
# it reads the plan's real usage windows (5-hour and weekly) from Claude itself, moves to Haiku at
# SWITCH_FRACTION, waits for the window to reset near the limit, and never uses paid overage.
# All state lives in the repository, so a stopped run can simply be started again.
#
#   scripts/autobuild.sh                run it (best inside tmux)
#   DRY_RUN=1 scripts/autobuild.sh      show plan usage and the row plan, change nothing
#   touch .autobuild/STOP               stop cleanly after the current step
set -euo pipefail

cd "$(dirname "$0")/.."

RUNBOOK="BUILD-RUNBOOK.md"
STATE_DIR=".autobuild"
SETTINGS="scripts/autobuild-settings.json"

SWITCH_FRACTION="${SWITCH_FRACTION:-0.6}"   # plan usage at which every row moves to Haiku
PAUSE_FRACTION="${PAUSE_FRACTION:-0.9}"     # plan usage at which the run waits for the window to reset
ROW_BUDGET_USD="${ROW_BUDGET_USD:-6}"       # runaway guard for one session (list-price estimate, not money)
MAX_ATTEMPTS="${MAX_ATTEMPTS:-2}"
ROW_TIMEOUT="${ROW_TIMEOUT:-90m}"
DRY_RUN="${DRY_RUN:-0}"

FIVE=0; FIVE_RESET=0; WEEK=0; WEEK_RESET=0; STATUS=allowed; OVERAGE=false
LAST_OUT=""

mkdir -p "$STATE_DIR/logs"

log() {
	printf '%s %s\n' "$(date '+%F %T')" "$*" | tee -a "$STATE_DIR/driver.log"
}

# ---- runbook table -------------------------------------------------------------------------------

# Prints "id|name|model" for every open row, in table order.
open_rows() {
	awk -F'|' '
		/^\| *[0-9]+[a-z]? *\|/ {
			for (i = 2; i <= 6; i++) gsub(/^ +| +$/, "", $i)
			if ($6 == "[ ]") print $2 "|" $3 "|" $5
		}' "$RUNBOOK"
}

row_status() {
	awk -F'|' -v id="$1" '
		/^\| *[0-9]+[a-z]? *\|/ {
			gsub(/^ +| +$/, "", $2); gsub(/^ +| +$/, "", $6)
			if ($2 == id) print $6
		}' "$RUNBOOK"
}

mark_blocked() {
	sed -i -E "s/^(\| *$1 *\|.*)\[ \]( *\|)$/\1[!]\2/" "$RUNBOOK"
}

# ---- plan usage ----------------------------------------------------------------------------------

at_least() {
	awk -v a="$1" -v b="$2" 'BEGIN { exit !(a >= b) }'
}

percent() {
	awk -v v="$1" 'BEGIN { printf "%.0f%%", v * 100 }'
}

peak_usage() {
	awk -v a="$FIVE" -v b="$WEEK" 'BEGIN { print (a > b ? a : b) }'
}

window_field() {   # $1 event line, $2 window name, $3 field name
	printf '%s' "$1" | grep -o "\"$2\":{[^}]*}" | grep -o "\"$3\":[0-9.]*" | head -1 | cut -d: -f2 || true
}

# Reads the last rate-limit event of a stream-json log; keeps the old value for a window it omits.
read_usage() {
	local line value
	line="$(grep '"type":"rate_limit_event"' "$1" 2>/dev/null | tail -1 || true)"
	[[ -z "$line" ]] && return 0
	value="$(window_field "$line" five_hour utilization)";  [[ -n "$value" ]] && FIVE="$value"
	value="$(window_field "$line" five_hour resetsAt)";     [[ -n "$value" ]] && FIVE_RESET="$value"
	value="$(window_field "$line" seven_day utilization)";  [[ -n "$value" ]] && WEEK="$value"
	value="$(window_field "$line" seven_day resetsAt)";     [[ -n "$value" ]] && WEEK_RESET="$value"
	STATUS="$(printf '%s' "$line" | grep -o '"status":"[a-z_]*"' | head -1 | cut -d'"' -f4)"
	OVERAGE="$(printf '%s' "$line" | grep -o '"isUsingOverage":[a-z]*' | head -1 | cut -d: -f2)"
}

# A one-word request to Haiku: costs almost nothing and reports the plan's current usage.
probe_usage() {
	local out="$STATE_DIR/logs/probe-$(date +%H%M%S).jsonl"
	timeout 120 claude -p "Reply with the single word OK." --model haiku --settings "$SETTINGS" \
		--permission-mode dontAsk --output-format stream-json --verbose --no-session-persistence \
		< /dev/null > "$out" 2> /dev/null || true
	read_usage "$out"
	log "plan usage: five-hour $(percent "$FIVE"), weekly $(percent "$WEEK") (status ${STATUS})"
}

sleep_until() {   # $1 epoch (0 = unknown, wait 30 minutes), $2 reason; returns 1 if a stop was requested
	local target="$1"
	(( target > 0 )) || target=$(( $(date +%s) + 1800 ))
	target=$(( target + 90 ))
	log "waiting until $(date -d "@${target}" '+%F %T') ($2)"
	while (( $(date +%s) < target )); do
		[[ -e "$STATE_DIR/STOP" ]] && return 1
		sleep 60
	done
}

# Which window to wait for: the fuller one.
next_reset() {
	if awk -v a="$FIVE" -v b="$WEEK" 'BEGIN { exit !(a > b) }'; then echo "$FIVE_RESET"; else echo "$WEEK_RESET"; fi
}

wait_for_capacity() {
	if at_least "$WEEK" "$PAUSE_FRACTION"; then
		sleep_until "$WEEK_RESET" "weekly plan usage is $(percent "$WEEK")" || return 1
		probe_usage
	elif at_least "$FIVE" "$PAUSE_FRACTION"; then
		sleep_until "$FIVE_RESET" "5-hour plan usage is $(percent "$FIVE")" || return 1
		probe_usage
	fi
}

# Haiku by default, Sonnet for starred rows and for a second attempt, Haiku for everything once the
# plan usage reaches SWITCH_FRACTION (the "60% rule").
choose_model() {
	local tier="$1" attempt="$2"
	if at_least "$(peak_usage)" "$SWITCH_FRACTION"; then
		echo haiku
	elif [[ "$tier" == *Sonnet* || "$attempt" -gt 1 ]]; then
		echo sonnet
	else
		echo haiku
	fi
}

# ---- one session ---------------------------------------------------------------------------------

build_prompt() {
	local id="$1" name="$2"
	cat <<EOF
Resume the PrepAI build and complete row ${id} (${name}) of BUILD-RUNBOOK.md, then stop.

Read CLAUDE.md and BUILD-RUNBOOK.md first, then follow the runbook: prepai/ARCHITECTURE.md, the module's MODULE.md, and only the spec sections it links to (load them with scripts/spec-section.sh).

You are running unattended and nobody will answer questions. Do not ask any. Decide using BUILD-DECISIONS.md. If a choice is not covered there, take the simplest option that matches the spec and add one line to "Decisions made during the build". If something needs a person (a real key, a legal or business decision), use a fake or a feature flag, keep going, and add it to "Needs the user".

Run Gradle from the repository root as: nice -n 15 ./prepai/gradlew -p prepai -q <tasks>. Do not leave any server or background process running.

Finish with a checkpoint: ./gradlew check green (frontend: lint, tests and build), the module's MODULE.md true, row ${id} ticked [x] in BUILD-RUNBOOK.md, one handoff line added, and a commit (git add with explicit paths, a feat/fix/test/docs/chore message, never git push). If you cannot reach a green checkpoint, leave the row unticked, change nothing outside the row's scope, and explain why in the handoff line.

Report in 10 lines or fewer.
EOF
}

run_session() {
	local id="$1" model="$2" prompt="$3"
	LAST_OUT="$STATE_DIR/logs/row-${id}-$(date +%H%M%S).jsonl"
	if ! timeout "$ROW_TIMEOUT" nice -n 15 claude -p "$prompt" --model "$model" \
		--settings "$SETTINGS" --permission-mode dontAsk --max-budget-usd "$ROW_BUDGET_USD" \
		--output-format stream-json --verbose --no-session-persistence \
		< /dev/null > "$LAST_OUT" 2> "${LAST_OUT}.err"; then
		log "row ${id}: the session ended with an error, see ${LAST_OUT}"
	fi
	read_usage "$LAST_OUT"
	log "row ${id}: plan usage now five-hour $(percent "$FIVE"), weekly $(percent "$WEEK")"
}

session_hit_limit() {
	local result
	result="$(grep '"type":"result"' "$LAST_OUT" 2>/dev/null | tail -1 || true)"
	printf '%s' "$result" | grep -q '"is_error":true' && printf '%s' "$result" | grep -qi 'limit'
}

tree_is_clean() {
	[[ -z "$(git status --porcelain)" ]]
}

stop_run() {
	log "STOPPED: $1"
	printf '%s\n' "$1" > "$STATE_DIR/STOPPED"
	exit 1
}

# ---- dry run -------------------------------------------------------------------------------------

show_plan() {
	local id name tier
	probe_usage
	log "dry run: switch to Haiku at $(percent "$SWITCH_FRACTION") plan usage, wait for a reset at $(percent "$PAUSE_FRACTION")"
	log "five-hour window resets $(date -d "@${FIVE_RESET}" '+%F %T'), weekly window resets $(date -d "@${WEEK_RESET}" '+%F %T')"
	while IFS='|' read -r id name tier; do
		log "  row ${id}: ${name}  [$(choose_model "$tier" 1)]"
	done < <(open_rows)
}

# ---- main ----------------------------------------------------------------------------------------

if [[ "$DRY_RUN" == "1" ]]; then
	show_plan
	exit 0
fi

exec 9> "$STATE_DIR/lock"
flock -n 9 || { echo "another autobuild run is already active"; exit 1; }
rm -f "$STATE_DIR/STOPPED"

probe_usage

while true; do
	[[ -e "$STATE_DIR/STOP" ]] && { log "stop requested, finishing"; break; }
	[[ "$OVERAGE" == "true" ]] && stop_run "the plan has started using paid overage; refusing to spend extra money"
	wait_for_capacity || { log "stop requested while waiting"; break; }

	next="$(open_rows | head -1)"
	[[ -z "$next" ]] && { log "all rows are done"; break; }
	IFS='|' read -r id name tier <<< "$next"

	tree_is_clean || stop_run "the working tree has uncommitted changes before row ${id}"

	attempt=1
	while (( attempt <= MAX_ATTEMPTS )); do
		model="$(choose_model "$tier" "$attempt")"
		log "row ${id} (${name}): attempt ${attempt} on ${model}"
		run_session "$id" "$model" "$(build_prompt "$id" "$name")"

		if session_hit_limit; then
			log "row ${id}: the plan usage limit was reached, waiting for the reset"
			sleep_until "$(next_reset)" "usage limit reached" || break 2
			probe_usage
			continue
		fi
		if [[ "$(row_status "$id")" == "[x]" ]] && tree_is_clean; then
			log "row ${id}: done"
			break
		fi
		log "row ${id}: checkpoint not reached"
		attempt=$((attempt + 1))
	done

	if (( attempt > MAX_ATTEMPTS )); then
		mark_blocked "$id"
		stop_run "row ${id} (${name}) did not reach a green checkpoint after ${MAX_ATTEMPTS} attempts, marked [!]"
	fi
done

log "finished: plan usage five-hour $(percent "$FIVE"), weekly $(percent "$WEEK")"
