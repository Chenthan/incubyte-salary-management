#!/bin/sh
# Optional managed-cloud proxy/CA support; no session trust material enters runtime images.
set -eu
salary_settings="/tmp/salary-maven-settings-$$.xml"
salary_trust="/tmp/salary-java-trust-$$"
trap 'rm -f "$salary_settings" "$salary_trust"' EXIT HUP INT TERM
if [ -f /run/secrets/proxy_ca ]; then
  cp "$JAVA_HOME/lib/security/cacerts" "$salary_trust"
  keytool -importcert -noprompt -alias salary-build-proxy -file /run/secrets/proxy_ca \
    -keystore "$salary_trust" -storepass changeit >/dev/null 2>&1
  MAVEN_OPTS="${MAVEN_OPTS:-} -Djavax.net.ssl.trustStore=$salary_trust -Djavax.net.ssl.trustStorePassword=changeit"
  export MAVEN_OPTS
fi
if [ -n "${HTTPS_PROXY:-}" ]; then
  salary_proxy="${HTTPS_PROXY#*://}"
  salary_proxy="${salary_proxy%/}"
  salary_proxy_host="${salary_proxy%:*}"
  salary_proxy_port="${salary_proxy##*:}"
  case "$salary_proxy_host" in *'@'*|*'<'*|*'>'*|*'&'*) echo 'Unsupported proxy configuration' >&2; exit 1;; esac
  case "$salary_proxy_port" in *[!0-9]*|'') echo 'Invalid proxy port' >&2; exit 1;; esac
  cat >"$salary_settings" <<EOF
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"><proxies><proxy><id>session</id><active>true</active><protocol>http</protocol><host>$salary_proxy_host</host><port>$salary_proxy_port</port><nonProxyHosts>localhost|127.0.0.1|db|test-db</nonProxyHosts></proxy></proxies></settings>
EOF
  mvn -B -ntp --settings "$salary_settings" "$@"
else
  mvn -B -ntp "$@"
fi
