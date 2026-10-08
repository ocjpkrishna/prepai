#!/usr/bin/env bash
# Unattended build driver (see BUILD-RUNBOOK.md, "Unattended mode").
# Runs one fresh, small Claude session per runbook row, on the model the row names, until every row
# is done, a row is blocked, the budget is used, or a stop is requested. All state lives in the
# repository, nothing lives in a conversation, so a stopped run can simply be started again.
#
#   BUDGET_USD=40 scripts/autobuild.sh              run it
#   DRY_RUN=1 BUDGET_USD=40 scripts/autobuild.sh    show the plan, call nothing
#   touch .autobuild/STOP                           stop cleanly after the current row
set -euo pipefail

cd "$(dirname "$0")/.."

RUNBOOK="BUILD-RUNBOOK.md"
STATE_DIR=".autobuild"
SETTINGS="scripts/autobuild-settings.json"

BUDGET_USD="${BUDGET_USD:?set the total budget in USD, for example BUDGET_USD=40}"
ROW_BUDGET_USD="${ROW_BUDGET_USD:-4}"
SWITCH_FRACTION="${SWITCH_FRACTION:-0.6}"
MAX_ATTEMPTS="${MAX_ATTEMPTS:-2}"
ROW_TIMEOUT="${ROW_TIMEOUT:-90m}"
DRY_RUN="${DRY_RUN:-0}"

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

# ---- budget and model choice ---------------------------------------------------------------------

spent() {
	cat "$STATE_DIR/spent_usd" 2>/dev/null || echo 0
}

add_spent() {
	awk -v a="$(spent)" -v b="$1" 'BEGIN { printf "%.4f\n", a + b }' > "$STATE_DIR/spent_usd"
}

past_switch_point() {
	awk -v s="$(spent)" -v b="$BUDGET_USD" -v f="$SWITCH_FRACTION" 'BEGIN { exit !(s >= b * f) }'
}

budget_used_up() {
	awk -v s="$(spent)" -v b="$BUDGET_USD" 'BEGIN { exit !(s >= b) }'
}

# Haiku by default, Sonnet for starred rows and for a second attempt, Haiku for everything after
# the switch point (the "60% rule").
choose_model() {
	local tier="$1" attempt="$2"
	if past_switch_point; then
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
	local out="$STATE_DIR/logs/row-${id}-$(date +%H%M%S).json"
	if ! timeout "$ROW_TIMEOUT" nice -n 15 claude -p "$prompt" --model "$model" \
		--settings "$SETTINGS" --permission-mode dontAsk --max-budget-usd "$ROW_BUDGET_USD" \
		--output-format json --no-session-persistence < /dev/null > "$out" 2> "$out.err"; then
		log "row ${id}: the session ended with an error, see ${out}"
	fi
	local cost
	cost="$(grep -o '"total_cost_usd":[0-9.eE+-]*' "$out" 2>/dev/null | head -1 | cut -d: -f2 || true)"
	add_spent "${cost:-0}"
	log "row ${id}: session cost \$${cost:-0}, total \$$(spent)"
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
	log "dry run: budget \$${BUDGET_USD}, per-row cap \$${ROW_BUDGET_USD}, switch to Haiku at ${SWITCH_FRACTION} of the budget"
	while IFS='|' read -r id name tier; do
		log "  row ${id}: ${name}  [$(choose_model "$tier" 1)]"
	done < <(open_rows)
	log "prompt for the first open row:"
	IFS='|' read -r id name tier < <(open_rows | head -1)
	[[ -n "${id:-}" ]] && build_prompt "$id" "$name" | sed 's/^/    /'
}

# ---- main ----------------------------------------------------------------------------------------

if [[ "$DRY_RUN" == "1" ]]; then
	show_plan
	exit 0
fi

exec 9> "$STATE_DIR/lock"
flock -n 9 || { echo "another autobuild run is already active"; exit 1; }
rm -f "$STATE_DIR/STOPPED"

while true; do
	[[ -e "$STATE_DIR/STOP" ]] && { log "stop requested, finishing"; break; }
	budget_used_up && { log "budget used up (\$$(spent) of \$${BUDGET_USD})"; break; }

	next="$(open_rows | head -1)"
	[[ -z "$next" ]] && { log "all rows are done"; break; }
	IFS='|' read -r id name tier <<< "$next"

	tree_is_clean || stop_run "the working tree has uncommitted changes before row ${id}"

	attempt=1
	while (( attempt <= MAX_ATTEMPTS )); do
		model="$(choose_model "$tier" "$attempt")"
		log "row ${id} (${name}): attempt ${attempt} on ${model}"
		run_session "$id" "$model" "$(build_prompt "$id" "$name")"
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

log "finished: spent \$$(spent) of \$${BUDGET_USD}"
