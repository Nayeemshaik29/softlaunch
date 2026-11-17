#!/bin/bash
# Usage: healthcheck.sh <port>. No curl in the image on purpose: bash can speak HTTP over /dev/tcp.
exec 3<>/dev/tcp/127.0.0.1/"$1" || exit 1
printf 'GET /actuator/health/readiness HTTP/1.0\r\nHost: localhost\r\n\r\n' >&3
grep -q '"status":"UP"' <&3
