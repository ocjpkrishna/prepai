#!/usr/bin/env bash
# Prints the prompt for one runbook row, for a cloud session:
#   claude --cloud "$(scripts/cloud-prompt.sh 3)" --model sonnet
# BASE=row-2 scripts/cloud-prompt.sh 3   starts the row from the unmerged branch origin/row-2 and opens the PR against it
set -euo pipefail
id="${1:?usage: cloud-prompt.sh <row id from BUILD-RUNBOOK.md, for example 3 or 9b>}"
BASE="${BASE:-main}"
cd "$(dirname "$0")/.."

name="$(awk -F'|' -v id="$id" '/^\| *[0-9]+[a-z]? *\|/ { gsub(/^ +| +$/, "", $2); gsub(/^ +| +$/, "", $3); if ($2 == id) print $3 }' BUILD-RUNBOOK.md)"
[[ -n "$name" ]] || { echo "row $id not found in BUILD-RUNBOOK.md" >&2; exit 1; }

cat <<PROMPT
Complete row ${id} (${name}) of BUILD-RUNBOOK.md in this repository, then stop.

Read CLAUDE.md first (including its "Cloud sessions" section), then BUILD-RUNBOOK.md, prepai/ARCHITECTURE.md, the module's MODULE.md, and only the spec sections it links to (load them with scripts/spec-section.sh). Decisions are in BUILD-DECISIONS.md.

Nobody will answer questions: do not ask any. If a choice is not covered, take the simplest option that matches the spec and mention it in the pull request description. Use fakes for anything that needs a real key or service.

Start from the branch origin/${BASE} (git fetch origin ${BASE} && git checkout -b row-${id} origin/${BASE}), and work on the new branch row-${id}, never on main. Check your work with ./prepai/gradlew -p prepai -q check -PskipDbTests (this sandbox has no PostgreSQL or Redis, so tag any test that needs them with @Tag("db")). Do not edit BUILD-RUNBOOK.md.

When the row is done and checks are green: commit (a feat/fix/test/docs/chore message), push the branch, and open a pull request against ${BASE}. Put the handoff in the PR description: what is done, what is next, and any problem. Report in 10 lines or fewer.
PROMPT
