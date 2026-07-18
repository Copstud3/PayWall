#!/usr/bin/env bash

set -Eeuo pipefail

APP_NAME="payment-gateway"
HEALTH_URL="http://localhost:8080/actuator/health/readiness"
MAX_ATTEMPTS=20
WAIT_SECONDS=10

echo "======================================"
echo "Deploying Ficmart Payment Gateway"
echo "======================================"

cd "$(dirname "$0")"

if [ ! -f ".env" ]; then
    echo "Deployment failed: .env file was not found."
    exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
    echo "Deployment failed: Docker is not installed."
    exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
    echo "Deployment failed: Docker Compose is not installed."
    exit 1
fi

echo
echo "Pulling the latest code from GitHub..."
git pull --ff-only

echo
echo "Building and starting containers..."
docker compose up --build -d

echo
echo "Waiting for the application to become healthy..."

for attempt in $(seq 1 "$MAX_ATTEMPTS"); do
    if curl --fail --silent "$HEALTH_URL" >/dev/null; then
        echo
        echo "Deployment completed successfully."
        echo "Health endpoint: $HEALTH_URL"
        docker compose ps
        exit 0
    fi

    echo "Health check attempt $attempt/$MAX_ATTEMPTS failed. Retrying in ${WAIT_SECONDS}s..."
    sleep "$WAIT_SECONDS"
done

echo
echo "Deployment failed: the application did not become healthy."
echo "Recent application logs:"
docker compose logs --tail=100 "$APP_NAME"

exit 1