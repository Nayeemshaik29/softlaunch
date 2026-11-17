#!/bin/bash
# Builds all service jars on your Mac, then the Docker images.
# Usage: ./docker/build-images.sh              (all services)
#        ./docker/build-images.sh user-service  (just one, after changing it)
set -euo pipefail
cd "$(dirname "$0")/.."
SERVICES=${*:-"registry-service config-server api-gateway user-service match-service notification-service discovery-service chat-service verification-service safety-service ai-service"}
for s in $SERVICES; do
  echo "📦 packaging $s"
  (cd "$s" && ./mvnw -q -DskipTests package)   # tests run in CI (Phase 11 Step 4), not on every image build
done
echo "🐳 building images"
docker compose --profile all build $SERVICES
docker images "softlaunch/*" --format "table {{.Repository}}\t{{.Size}}"
