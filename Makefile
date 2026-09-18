# This Makefile gives me short, repeatable commands for common project tasks.
#
# Linux and macOS:
#   make help
#   make db-up
#   make test
#   make frontend
#
# Windows:
#   Make is available through Git Bash, WSL, or Chocolatey.
#   If I do not have make installed, I can use the PowerShell commands in
#   README.md and docs/local-development.md instead.

API_DIR := services/booking-service
FRONTEND_DIR := apps/booking-form

.PHONY: help test api-test frontend api db-up db-down db-reset clean

help:
	@echo "make test      - run Java unit tests and JavaScript checks"
	@echo "make api-test  - run Python API acceptance tests"
	@echo "make db-up     - start PostgreSQL"
	@echo "make db-down   - stop PostgreSQL but keep the database volume"
	@echo "make db-reset  - delete and recreate the local database volume"
	@echo "make api       - start the Spring Boot API on port 8080"
	@echo "make frontend  - serve the booking form on port 8000"
	@echo "make clean     - remove Maven build output"

# Run the Java unit tests and basic JavaScript syntax checks.
test:
	cd $(API_DIR) && mvn clean test
	node --check $(FRONTEND_DIR)/app.js
	node --check $(FRONTEND_DIR)/tilt-effects.js

# Run this only while the API is already running on localhost:8080.
api-test:
	python3 tests/acceptance/test_api.py

# Start PostgreSQL in the background.
db-up:
	docker compose up -d postgres

# Stop PostgreSQL without deleting the named volume.
db-down:
	docker compose down

# Delete local database data and recreate the PostgreSQL container.
# This is useful when Flyway reports a migration checksum mismatch.
db-reset:
	docker compose down -v
	docker compose up -d postgres

# Start Spring Boot. Keep this terminal open while testing the frontend.
api:
	cd $(API_DIR) && mvn spring-boot:run

# Serve the static frontend through HTTP instead of opening index.html directly.
frontend:
	python3 -m http.server 8000 --directory $(FRONTEND_DIR)

# Remove generated Maven files. Docker data is not removed by this target.
clean:
	cd $(API_DIR) && mvn clean
