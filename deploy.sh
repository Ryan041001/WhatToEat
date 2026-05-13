#!/usr/bin/env bash
set -euo pipefail

if [ ! -f .env ]; then
  echo "Missing .env. Copy .env.example to .env and fill production values first." >&2
  exit 1
fi

docker compose --env-file .env -f compose.prod.yaml up -d --build

echo "Waiting for services to report health..."
sleep "${DEPLOY_HEALTH_WAIT_SECONDS:-20}"

docker compose --env-file .env -f compose.prod.yaml ps
