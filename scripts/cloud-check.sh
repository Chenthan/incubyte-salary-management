#!/bin/bash
# Development/verification helper for the managed cloud. Local deployment only needs run.sh.
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p artifacts .cache/m2
case "${1:-help}" in
  deps)
    (cd frontend && npm install --save-dev --save-exact @axe-core/playwright --no-fund)
    python3 - <<'PY'
import urllib.request, zipfile, io, hashlib
from pathlib import Path
base = 'https://repo.maven.apache.org/maven2/'
with urllib.request.urlopen(base + 'org/apache/maven/wrapper/maven-wrapper-distribution/3.3.4/maven-wrapper-distribution-3.3.4-only-script.zip', timeout=30) as r:
    archive = zipfile.ZipFile(io.BytesIO(r.read()))
for name in ['mvnw', 'mvnw.cmd']:
    target = Path('backend') / name
    target.write_bytes(archive.read(name).replace(b'\r\n', b'\n'))
    if name == 'mvnw': target.chmod(0o755)
with urllib.request.urlopen(base + 'org/apache/maven/apache-maven/3.9.16/apache-maven-3.9.16-bin.zip', timeout=30) as r:
    digest = hashlib.sha256(r.read()).hexdigest()
p = Path('backend/.mvn/wrapper/maven-wrapper.properties')
lines = [line for line in p.read_text().splitlines() if not line.startswith('distributionSha256Sum=')]
p.write_text('\n'.join(lines) + '\ndistributionSha256Sum=' + digest + '\n')
print('Only-script wrapper and Maven distribution checksum pinned.')
PY
    ;;
  backend)
    if [ ! -f artifacts/test.env ]; then
      python3 - <<'PY'
from pathlib import Path
import secrets
p = Path('artifacts/test.env')
p.write_text('DB_USERNAME=salary\nDB_PASSWORD=' + secrets.token_hex(24) + '\nHR_PASSWORD=' + secrets.token_hex(24) + '\n')
p.chmod(0o600)
PY
    fi
    docker compose --env-file artifacts/test.env --profile test up --detach --wait test-db
    salary_mounts=()
    if [ -n "${CODEX_PROXY_CERT:-}" ]; then salary_mounts+=(--mount "type=bind,src=$CODEX_PROXY_CERT,dst=/run/secrets/proxy_ca,readonly"); fi
    shift
    if [ "$#" -eq 0 ]; then set -- spotless:check verify -Pintegration; fi
    if ! docker run --rm --entrypoint sh --user "$(id -u):$(id -g)" --network salary-management_default \
      --env-file artifacts/test.env -e DB_URL=jdbc:postgresql://test-db:5432/salary_test -e MAVEN_CONFIG=/tmp/.m2 \
      --mount "type=bind,src=$PWD/backend,dst=/work" --mount "type=bind,src=$PWD/scripts,dst=/scripts,readonly" \
      --mount "type=bind,src=$PWD/.cache/m2,dst=/cache" "${salary_mounts[@]}" -w /work \
      maven:3.9.16-eclipse-temurin-21@sha256:99e61abcff91a9b1333463bd8451fb18495d6eba9250ac66a338b518f8278320 \
      /scripts/maven-network.sh -Dmaven.repo.local=/cache "$@" >artifacts/backend-tests.log 2>&1; then
      tail -n 65 artifacts/backend-tests.log
      exit 1
    fi
    tail -n 22 artifacts/backend-tests.log
    ;;
  up) ./scripts/run.sh ;;
  e2e)
    cd frontend
    if [ -x /usr/bin/chromium ]; then export PLAYWRIGHT_CHROMIUM_EXECUTABLE=/usr/bin/chromium; fi
    npx playwright test
    ;;
  *) echo 'Usage: bash scripts/cloud-check.sh [deps|backend [maven goals...]|up|e2e]' >&2; exit 1 ;;
esac
