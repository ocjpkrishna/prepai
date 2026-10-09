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
├── grafana/
│   ├── alert-rules.json        8 PrepAI alert rules (compact form; scripts/grafana-sync.sh expands them)
│   └── dashboards/             4 dashboards: lesson pipeline, LLM cost and retries, errors, business funnel
├── prometheus/prometheus.yml   scrapes 127.0.0.1:9091 (installed to /etc/prepai/prometheus.yml)
├── logrotate/prepai            30 daily rotations of /var/log/prepai/*.log
└── systemd/
    ├── prepai.service          the Spring Boot app, user prepai, EnvironmentFile /etc/prepai/prepai.env
    └── prepai-prometheus.service   Prometheus on 127.0.0.1:9095, 15-day retention
```
Scripts in `scripts/`: `install.sh` (bootstrap), `deploy.sh` (build, release, health check, rollback), `grafana-sync.sh` (dashboards, alert rules, data source in the `PrepAI` folder only; `--dry-run` prints the payloads without contacting Grafana).

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
- [x] `scripts/deploy.sh`, `ops/systemd/prepai.service` (row 12). Not run on a real VPS yet
- [x] Prometheus unit and config, `scripts/grafana-sync.sh`, `ops/grafana/` (row 12). Not run against the real Grafana yet
- [x] logrotate config (row 12)
- [ ] VoiceStudio unit (its source is not in the repo, spec 10.1)
- [ ] Monitoring script for disk, RAM and VoiceStudio/PostgreSQL/Redis health (spec 9.2 item 7); needs a health source for the alerts in spec 8.4

## Gotchas (row 12)
- `deploy.sh` runs as root (`sudo`), like `install.sh`. A restart takes the single app down for a few seconds; in-flight requests finish within the 30 s graceful stop. Spec 9.2 asks for zero downtime, which needs a second instance and is not built.
- Only the `prepai` alert rule "PrepAI app is down" checks availability (`up`). Spec 8.4 also asks for alerts on VoiceStudio, PostgreSQL, Redis, disk and RAM. Prometheus has no metric for them, so those alerts are not built.
- The `PrepAI` Grafana folder and data source are created by `grafana-sync.sh`. Routing the `app=prepai` alerts to email is a manual step in the Grafana UI (TODO-5).
