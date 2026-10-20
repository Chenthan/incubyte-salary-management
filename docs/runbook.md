# Local deployment and operations

## Startup and prerequisites

Use Docker with Compose v2.20 or later. The verified cloud client was Docker 28.4.0 / Compose 2.40.3. On Windows/macOS use Docker Desktop's Linux containers; WSL2 is the normal Windows backend. Java/Node are build stages, not host prerequisites for startup.

Run `./scripts/run.sh` from a clone, or `scripts/run.ps1` in PowerShell. The first run writes `.env`, owner-readable on Unix, with independent 192-bit random DB/HR passwords. No shared password is committed. Startup waits for health, including the DB dependency and completed startup runners. HR credentials are displayed only on the explicit `credentials` subcommand.

The app is bound to 127.0.0.1:8080; PostgreSQL is accessible only on the Compose network. Override `APP_PORT` in `.env` or the process environment if needed. Default local HTTP uses a non-Secure, HttpOnly, SameSite=Strict session cookie; framework defaults require Secure cookies outside this explicit local configuration. HTTP is appropriate only within this chosen loopback deployment.

The application process runs as UID/GID 10001 with dropped capabilities, no-new-privileges, a read-only root filesystem, and a temporary `/tmp`. App memory is capped at 768 MiB and PostgreSQL at 512 MiB; build-stage memory is controlled by the Docker engine. Logs rotate at 10 MiB × 3 files. There is one app instance; sessions do not survive restart.

## Persistence and seeding

The `postgres-data` named volume contains durable application data. `run.sh stop` / `docker compose down` preserve it. Normal startup never runs `down --volumes`. Seeding is opt-in (`DEMO_SEED=true` in generated local configuration), uses stable IDs/seed keys, inserts missing seed keys, and preserves edits. On a fresh seed: 10,000 employees, six countries, 9,500 active. User/browser-created records are additional; count seed keys when verifying the original dataset.

Keep `.env` with the matching database volume. `POSTGRES_PASSWORD` initializes a new database; editing it later does not rotate a persisted PostgreSQL role's password. Likewise, HR bootstrap creates a missing username but does not overwrite an existing password hash. Password/user rotation requires an intentional database/account-management procedure; there is no administration UI or recovery email. Do not delete a live volume to solve an authentication issue.

Changing the HR bootstrap username creates another HR account and does not revoke the old one. This single-role assessment does not implement account lifecycle administration. Do not expose it publicly as a production HR system.

## Checks and troubleshooting

```sh
docker compose ps
docker compose logs --tail=100 app
docker compose logs --tail=100 db
docker compose config --quiet
docker compose exec -T db psql -U salary -d salary -c 'SELECT count(*) FROM employees WHERE demo_seed_key IS NOT NULL'
```

Avoid printing resolved Compose configuration or `.env` into public logs because they contain local passwords. Readiness: `/actuator/health/readiness`; it exposes status without DB details. Liveness: `/actuator/health/liveness`.

If a port is busy, select another `APP_PORT`. If Docker is unavailable, start the local Docker engine. If credentials fail after configuration changes, restore the matching configuration or perform an authorized password rotation. Migration errors stop startup; inspect the error and restore a consistent schema/version rather than disabling Flyway validation.

## Backup and recovery

Before real changes to any retained dataset, create a private dump:

```sh
mkdir -p backups
umask 077
docker compose exec -T db pg_dump -U salary -d salary -Fc > backups/salary.dump
```

Keep backups and runtime configuration outside version control. Restore into a **separate** PostgreSQL 17 database first, using `pg_restore`, and validate schema/row counts and application readiness. Never restore over a database you intend to preserve without an explicit recovery decision. A dump command alone is not proof of a tested disaster-recovery procedure.

Flyway migrations are versioned; applied migrations are not edited. This release has one initial migration. Application rollback requires a compatible schema and a known previous image; destructive migration rollback is not automated. There is no claim of public hosting, TLS termination, external monitoring, scheduled backups, SSO, multi-instance sessions, or login abuse protection.

## Verification environments

`./scripts/test.sh` builds/runs backend and frontend checks in Docker and uses a separate tmpfs `salary_test` database. It stops that test DB afterward and never resets the demo volume. Backend tests refuse to truncate a database whose name does not end in `_test`.

The managed cloud requires proxy/CA support. `run.sh` automatically uses `compose.cloud.yml` when `CODEX_PROXY_CERT` is present. A BuildKit secret supplies the session CA; temporary Java trust settings are deleted in the same build step and are absent from the final image. Ordinary local deployment does not need this override.

Linux startup and actual Chromium flows are validated in the cloud. The PowerShell launcher is provided for Windows; a native Windows Docker Desktop run remains a separate platform check unless recorded in the validation report.
