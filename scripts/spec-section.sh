#!/usr/bin/env bash
# Print one section of prepai-spec.md, so a session loads a few hundred tokens instead of the whole spec.
#
#   scripts/spec-section.sh 6.3      section "### 6.3 ..."
#   scripts/spec-section.sh 9        the whole chapter "## 9. ..." with its sub-sections
#   scripts/spec-section.sh agent2   the task card "#### AGENT 2: ..."
#   scripts/spec-section.sh list     all section numbers with their titles
set -euo pipefail

spec="$(cd "$(dirname "$0")/.." && pwd)/prepai-spec.md"
query="${1:?usage: spec-section.sh <section number | agentN | list>}"

awk -v query="$query" '
	/^```/ { inFence = !inFence }
	!inFence && /^##+ / {
		match($0, /^#+/)
		level = RLENGTH
		title = substr($0, level + 2)
		split(title, words, " ")
		id = words[1]
		sub(/\.$/, "", id)
		if (id == "AGENT") {
			id = "agent" words[2]
			sub(/:$/, "", id)
		}
		if (query == "list") { print id "\t" title; next }
		if (found && level <= foundLevel) exit
		if (!found && id == query) { found = 1; foundLevel = level }
	}
	found { print }
	END { if (query != "list" && !found) { print "section not found: " query > "/dev/stderr"; exit 1 } }
' "$spec"
