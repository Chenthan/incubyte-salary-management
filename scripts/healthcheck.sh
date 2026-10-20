#!/bin/bash
set -eu
exec 3<>/dev/tcp/127.0.0.1/8080
printf 'GET /actuator/health/readiness HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n' >&3
IFS= read -r salary_status <&3
case "$salary_status" in *' 200 '*) exit 0;; *) exit 1;; esac
