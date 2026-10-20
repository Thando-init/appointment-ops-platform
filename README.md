# Appointment Operations Platform

A portfolio and learning project for managing salon appointments. Clients can browse services and availability, submit booking requests, and move a request through owner review, demo payment confirmation, and calendar scheduling.

## Current status

The local prototype uses a static HTML/CSS/JavaScript frontend, a Spring Boot API, PostgreSQL, ActiveMQ Artemis, and an n8n workflow. The API owns booking data and business rules; n8n coordinates notifications and external actions.

The end-to-end demo has been exercised locally, including owner email, approval, a simulated payment, and Google Calendar event creation. **The payment step is a demo adapter, not a payment provider.** Calendar creation uses the connected Google account when that n8n node runs. This project is not ready for real public bookings: owner actions and webhooks need production-grade authentication, payment callbacks need provider verification, and the service needs production hosting, backups, and privacy controls. Use fictional data in portfolio materials.

## Architecture

```text
Booking form → Spring Boot API → PostgreSQL
                       │
                       └→ ActiveMQ → Java JMS consumer → optional n8n webhook
                                                           ├→ Owner email
                                                           ├→ Demo approval/payment flow
                                                           └→ Google Calendar
```

The API validates availability and persists booking state. n8n is not the source of truth. The JMS consumer can forward booking-created events when `N8N_BOOKING_CREATED_WEBHOOK_URL` is configured; without it, the event is consumed and logged.

## Run locally

Prerequisites: Java 21, Maven, Docker Compose, and Python 3.

1. From the repository root, create local configuration and start the database and broker:

   ```bash
   cp .env.example .env
   docker compose up -d postgres activemq
   ```

2. In a terminal, start the API:

   ```bash
   cd services/booking-service
   mvn spring-boot:run
   ```

3. In another terminal, serve the frontend:

   ```bash
   cd apps/booking-form
   python3 -m http.server 8000
   ```

4. Open `http://localhost:8000`. The API health endpoint is `http://localhost:8080/api/v1/health`.

For the local n8n demo, start the `n8n` service with Docker Compose, import [`workflows/n8n/booking-intake.json`](workflows/n8n/booking-intake.json), and follow the [n8n guide](workflows/n8n/README.md). n8n testing can send email and create an event in the connected calendar, so use fake booking data.

## Tests

Run unit tests from `services/booking-service`:

```bash
mvn test
```

With the API running, run acceptance tests from the repository root:

```bash
python tests/acceptance/test_api.py
```

## Repository guide

Start at [`docs/README.md`](docs/README.md) for the documentation index. It links to local setup, API contracts, system design, testing, owner approval, and portfolio notes.

## Technologies

Java 21 · Spring Boot · PostgreSQL · JDBC · Flyway · ActiveMQ Artemis · n8n · HTML/CSS/JavaScript · JUnit/Mockito · GitHub Actions
