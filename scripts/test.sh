#!/bin/sh
# Docker-only unit/integration checks. The demo database and volume are never reset.
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
./scripts/run.sh init
salary_compose() {
  if [ -n "${CODEX_PROXY_CERT:-}" ]; then
    docker compose -f compose.yml -f compose.cloud.yml "$@"
  else
    docker compose "$@"
  fi
}
trap 'salary_compose --profile test stop test-db >/dev/null' EXIT
salary_compose --profile test build backend-tests frontend-check
salary_compose --profile test run --rm backend-tests
salary_compose --profile test run --rm frontend-check
