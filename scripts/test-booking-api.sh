#!/usr/bin/env bash
# This script performs the same basic API smoke checks as the PowerShell script.
# Run it after PostgreSQL and Spring Boot are running.
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

curl --fail --silent "$BASE_URL/api/v1/health"
printf '\n'

curl --fail --silent \
  "$BASE_URL/api/v1/availability/slots?serviceId=gel-overlay-art&date=2026-10-03"
printf '\n'

curl --fail --silent \
  -X POST "$BASE_URL/api/v1/bookings" \
  -H 'Content-Type: application/json' \
  --data '{
    "clientName": "Amina Patel",
    "phone": "+27821234567",
    "email": "amina@example.com",
    "serviceId": "gel-overlay-art",
    "preferredDate": "2026-10-03",
    "preferredTime": "14:00",
    "notes": "Automated API smoke test",
    "source": "website"
  }'
printf '\n'
