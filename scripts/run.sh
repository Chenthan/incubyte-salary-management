#!/bin/sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
command -v docker >/dev/null 2>&1 || { echo 'Install Docker with Compose v2, then run this command again.' >&2; exit 1; }
docker compose version >/dev/null
if [ ! -f .env ]; then
  umask 077
  salary_db_password=$(od -An -N24 -tx1 /dev/urandom | tr -d ' \n')
  salary_hr_password=$(od -An -N24 -tx1 /dev/urandom | tr -d ' \n')
  cat >.env <<EOF
DB_USERNAME=salary
DB_PASSWORD=$salary_db_password
HR_USERNAME=hr
HR_PASSWORD=$salary_hr_password
APP_PORT=8080
COOKIE_SECURE=false
DEMO_SEED=true
EOF
  echo 'Created private local configuration in .env.'
fi
case "${1:-up}" in
  init) echo 'Local configuration is ready.' ;;
  credentials) sed -n '/^HR_USERNAME=/p; /^HR_PASSWORD=/p' .env ;;
  stop) docker compose down ;;
  logs) docker compose logs --tail=100 -f app ;;
  up)
    if [ -n "${CODEX_PROXY_CERT:-}" ]; then
      docker compose -f compose.yml -f compose.cloud.yml up --build --detach --wait --wait-timeout 180 app
    else
      docker compose up --build --detach --wait --wait-timeout 180 app
    fi
    salary_port="${APP_PORT:-$(sed -n 's/^APP_PORT=//p' .env)}"
    case "$salary_port" in *[!0-9]*|'') echo 'Invalid APP_PORT in .env' >&2; exit 1;; esac
    printf '\nSalary workspace is ready: http://localhost:%s\n' "$salary_port"
    echo 'Run ./scripts/run.sh credentials to display your local HR sign-in.'
    ;;
  *) echo 'Usage: ./scripts/run.sh [up|init|credentials|stop|logs]' >&2; exit 1 ;;
esac
