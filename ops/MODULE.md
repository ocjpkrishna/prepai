# ops

**Purpose:** everything that keeps PrepAI running on the VPS: the bootstrap script, the reverse proxy, deployment, monitoring and the Grafana sync. No application code lives here.
**Built by:** Agent 9 (DevOps) | **Spec:** 8.3, 9.2 (Agent 9), 10.1, 10.4

## Context
- The VPS is shared with live trading services, so every script here is idempotent and changes only PrepAI's own units, files and databases.
- Scripts live in `scripts/` and config templates in `nginx/`, as spec 9.1.1 places them. This folder holds the operational notes and runbooks.

## Package map
```
ops/
├── MODULE.md
└── (runbooks and monitoring config are added by Agent 9)
```

## Other modules may call
Nothing. Code never imports from `ops`.

## Data
None. Configuration comes from `/etc/prepai/prepai.env` (mode 600). The sample is `.env.example` at the repository root.

## Rules and gotchas
- Never touch MongoDB, QuestDB, Grafana or the trading services (CLAUDE.md, "Shared VPS: hard limits").
- `scripts/install.sh` installs only what is missing and never upgrades shared runtimes.
- Nginx is the only public entry point. Actuator (9091) and Prometheus (9095) stay on 127.0.0.1.

## Status
- [x] `scripts/install.sh`, `nginx/prepai.conf.template`, `.env.example` (row 1)
- [ ] `scripts/deploy.sh` and the systemd unit (row 12)
- [ ] Prometheus and Grafana sync (row 12)
