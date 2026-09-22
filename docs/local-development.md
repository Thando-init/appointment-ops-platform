# Local development

I run the database, Java API, and static frontend as three separate local processes. This makes it easier for me to see which part of the system is working.

The commands below include both PowerShell and Linux/macOS examples. The Linux commands also work in WSL and Git Bash when the required tools are available.

## 1. Check the prerequisites

I need Java 21, Maven, Docker Compose, Python 3, and Git.

### PowerShell

```
java -version
mvn -version
docker --version
docker compose version
py --version
git --version
```

### Linux or macOS

```bash
java -version
mvn -version
docker --version
docker compose version
python3 --version
git --version
```

## 2. Create the local environment file

I run this from the repository root.

### PowerShell

```
Copy-Item .env.example .env
```

### Linux or macOS

```bash
cp .env.example .env
```

The `.env` file is for local values. I do not commit it to Git.

## 3. Start PostgreSQL

Docker runs PostgreSQL for me so I do not need to install PostgreSQL directly on my computer.

### PowerShell, Linux, or macOS

```bash
docker compose up -d postgres activemq
docker compose ps
```

The database is exposed at `localhost:5432`. ActiveMQ listens for JMS connections on `localhost:61616` and exposes its local web console at `http://localhost:8161`. Both containers should be running.

If I have GNU Make installed, I can use:

```bash
make db-up
```

The `make db-up` target starts PostgreSQL. If I also want ActiveMQ, I run:

```bash
docker compose up -d postgres activemq
```

## 4. Start the booking API

The API runs on port `8080`.

### PowerShell

```
Set-Location services/booking-service
mvn clean test
mvn spring-boot:run
```

### Linux or macOS

```bash
cd services/booking-service
mvn clean test
mvn spring-boot:run
```

I leave this terminal open while using the frontend.

If I am at the repository root and have Make installed, I can also run:

```bash
make api
```

On first startup, Flyway reads the migration files under:

```
services/booking-service/src/main/resources/db/migration/
```

The first migration creates:

- `services`

- `business_hours`

- `blocked_periods`

- `bookings`

- `booking_holds`

It also seeds the service catalogue and the opening hours used by the prototype.

## 5. Verify the API

I open another terminal while Spring Boot is running.

### PowerShell

```
Invoke-RestMethod http://localhost:8080/api/v1/health
Invoke-RestMethod "http://localhost:8080/api/v1/availability/slots?serviceId=gel-overlay-art&date=2026-10-03"
```

### Linux or macOS with curl

```bash
curl http://localhost:8080/api/v1/health
curl "http://localhost:8080/api/v1/availability/slots?serviceId=gel-overlay-art&date=2026-10-03"
```

The health response should show that the booking API is running.

## 6. Start the frontend

The frontend runs on port `8000`. I serve it through HTTP instead of double-clicking `index.html`.

### PowerShell

```
Set-Location apps/booking-form
py -m http.server 8000
```

### Linux or macOS

```bash
cd apps/booking-form
python3 -m http.server 8000
```

Then I open:

```
http://localhost:8000
```

From the repository root, Make provides the equivalent command:

```bash
make frontend
```

## 7. Run the tests

## 7A. Start n8n for the automation demo

From the repository root, start the local n8n container:

```bash
docker compose up -d n8n
docker compose ps
```

Open n8n at `http://localhost:5678`, import `workflows/n8n/booking-intake.json`, and use the Webhook node's **Execute workflow** button before sending a test request. The test URL is:

```
http://localhost:5678/webhook-test/booking-created
```

For a persistent demo, activate the workflow and use:

```
http://localhost:5678/webhook/booking-created
```

The n8n HTTP Request node reaches an API running on the Windows host through `host.docker.internal:8080`.

## 7B. Quick-start sequence

Use four terminals from the repository root:

```bash
# Terminal 1 — database and broker
docker compose up -d postgres activemq n8n

# Terminal 2 — Spring Boot API
cd services/booking-service
mvn spring-boot:run

# Terminal 3 — booking website
cd apps/booking-form
py -m http.server 8000

# Terminal 4 — optional checks
curl http://localhost:8080/api/v1/health
```

Then open the website at `http://localhost:8000` and n8n at `http://localhost:5678`.

To stop the stack without deleting database data:

```bash
docker compose down
```

To stop it and reset the local database volume:

```bash
docker compose down -v
```

## 8. Run the tests

### Java unit tests

From the API folder:

```bash
mvn clean test
```

From the repository root with Make:

```bash
make test
```

The unit tests use mocked repositories, so they do not require PostgreSQL.

### API acceptance tests

The API must already be running on port `8080`.

PowerShell:

```
python tests/acceptance/test_api.py
```

Linux or macOS:

```bash
python3 tests/acceptance/test_api.py
```

From the repository root with Make:

```bash
make api-test
```

## 8. Stop the local services

To stop only the frontend or API, I press `Ctrl+C` in the terminal running it.

To stop PostgreSQL and ActiveMQ while keeping their named volumes:

```bash
docker compose down
```

Or:

```bash
make db-down
```

The database volume is retained, so local data survives a normal stop.

## 9. Reset the local development infrastructure

If Flyway reports a migration checksum mismatch, or if I want to start with an empty local database, I can remove the Docker volume.

> This deletes local development data. I must not use it if I have important real bookings that are not backed up.

PowerShell, Linux, or macOS:

```bash
docker compose down -v
docker compose up -d postgres activemq
```

With Make:

```bash
make db-reset
```

The reset target recreates both the PostgreSQL and ActiveMQ development volumes.

## 10. Useful Make commands

From the repository root:

```bash
make help
make db-up
make api
make frontend
make test
make api-test
make db-down
make db-reset
make clean
```

The Makefile is mainly for Linux, macOS, Git Bash, and WSL. On Windows PowerShell, I can use the equivalent commands in this guide.

## Common problems

### The frontend says that availability is unavailable

I check these in order:

```
1. PostgreSQL is running.
2. Spring Boot started successfully.
3. http://localhost:8080/api/v1/health works.
4. The availability URL works directly.
5. The frontend is open at http://localhost:8000, not file://.
```

### Flyway reports a checksum mismatch

For a local learning database, I use the reset commands above. I do not casually edit an already-applied migration in a production database.

### Port 8080 is already in use

I stop the other application using the port, or change the Spring Boot port in `application.properties` and update the frontend API URL accordingly.

### Port 8000 is already in use

I can use another frontend port:

```bash
python3 -m http.server 8001 --directory apps/booking-form
```

Then I open `http://localhost:8001`. The API remains on port `8080`.