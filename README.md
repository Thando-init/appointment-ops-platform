# Appointment Operations Platform

## What I am building

I am building a practical appointment-management system for a small nail technician, beauty therapist, or salon business.

The idea is simple: a client should be able to choose a service, see the times that are actually available, and request an appointment without needing to send several back-and-forth messages. The business owner should then be able to review the request, approve it, collect payment, and add the confirmed appointment to a calendar.

This is also my learning project for combining software engineering with AI automation. I am keeping the first version simple enough to understand, but I am designing it around real business needs rather than treating it as only a school exercise.

## What works now

The current version has three main parts:

```
Static booking form
        |
        | GET available slots / POST booking
        v
Spring Boot booking API
        |
        | JDBC + Flyway
        v
PostgreSQL database
```

At the moment, I can:

- Show a service catalogue.

- Let a client choose a date.

- Calculate available appointment times from the database.

- Use the service duration and cleanup buffer when calculating slots.

- Exclude blocked periods and existing bookings.

- Save booking requests in PostgreSQL.

- Keep booking, approval, and payment statuses separate.

- Run unit tests for the availability and booking rules.

- Run API acceptance tests against a running application.

- Import a starter n8n workflow for the next automation stage.

The frontend and API can work without n8n. n8n is added for the actions around a booking, such as notifications, payment-link creation, and calendar updates.

## What the finished product does

The longer-term workflow is:

```
Client uses website or WhatsApp
        ↓
Client chooses a service and available time
        ↓
API validates and saves the booking request
        ↓
Business receives a notification
        ↓
Business approves or rejects the request
        ↓
If approved, client receives a payment link
        ↓
Payment provider confirms payment through a webhook
        ↓
The confirmed appointment is added to a calendar
        ↓
Client receives the final confirmation
```

I am deliberately keeping the Spring Boot API responsible for important business data. n8n should coordinate outside services, but it should not be the only place where a booking exists.

## Current architecture

```
+-----------------------+
| Booking form          |
| HTML, CSS, JavaScript |
+-----------+-----------+
            |
            | HTTP requests
            v
+-----------------------+
| Spring Boot API       |
| validation and rules  |
+-----------+-----------+
            |
            | JDBC
            v
+-----------------------+
| PostgreSQL            |
| bookings and schedule |
+-----------------------+

After persistence:

Spring Boot API ── booking.created event ──> n8n
                                           |
                                           +--> WhatsApp or email
                                           +--> payment link
                                           +--> calendar
                                           +--> owner notifications
```

## Project structure

```
appointment-ops-platform/
├── .env.example                         # Safe configuration template
├── .gitignore                           # Files Git must not track
├── docker-compose.yml                   # Local PostgreSQL container
├── Makefile                             # Shortcuts for common commands
├── README.md                            # Main project guide
│
├── .github/
│   └── workflows/
│       └── ci.yml                       # Automated checks on GitHub
│
├── apps/
│   └── booking-form/                    # Static HTML/CSS/JavaScript frontend
│       ├── index.html
│       ├── styles.css
│       ├── app.js
│       └── tilt-effects.js
│
├── services/
│   └── booking-service/                 # Spring Boot API
│       ├── pom.xml
│       └── src/
│           ├── main/java/com/sablestone/booking/
│           │   ├── api/                 # HTTP controllers
│           │   ├── domain/              # Request and response records
│           │   ├── repository/          # PostgreSQL queries
│           │   └── service/             # Business rules
│           ├── main/resources/
│           │   ├── application.properties
│           │   └── db/migration/        # Flyway SQL migrations
│           └── test/                    # Unit tests
│
├── tests/
│   └── acceptance/                      # Tests against a running API
│
├── workflows/
│   └── n8n/                             # Automation workflow and test payload
│
├── docs/                                # Design, API, testing, and learning notes
└── scripts/                             # Repeatable PowerShell and shell tests
```

## Technologies I am practising

- Java 21

- Spring Boot

- REST APIs

- JDBC

- PostgreSQL

- Flyway database migrations

- HTML, CSS, and vanilla JavaScript

- Docker Compose

- JUnit and Mockito

- Python acceptance tests

- Git and GitHub Actions

- n8n workflow automation

- Later: JMS and ActiveMQ for reliable event messaging

I chose a static frontend instead of React for the main booking form because I want to understand the browser, HTTP requests, API responses, and form behaviour clearly before adding another framework.

## Prerequisites

I need the following installed:

1. Java 21.

1. Maven.

1. Docker Desktop with Docker Compose.

1. Python 3, so I can serve the frontend and run acceptance tests.

1. Git.

I can check the installations with:

```
java -version
mvn -version
docker --version
docker compose version
py --version
git --version
```

On Linux or macOS, I use `python3` instead of `py`:

```bash
java -version
mvn -version
docker --version
docker compose version
python3 --version
git --version
```

The same Linux commands work in WSL if Docker Desktop is configured to integrate with my WSL distribution. I can also use the Makefile from Linux, macOS, Git Bash, or WSL:

```bash
make help
make db-up
make api
make frontend
make test
make api-test
```

If `make` is not installed on Windows, I use the PowerShell commands in this README and `docs/local-development.md` instead.

## First-time setup on Windows

I run these commands from the repository root, which is the folder containing `README.md` and `docker-compose.yml`.

```
Copy-Item .env.example .env
docker compose up -d postgres
docker compose ps
```

The PostgreSQL container should be running or healthy.

### Start the API

I open a second PowerShell window and run:

```
Set-Location services/booking-service
mvn clean spring-boot:run
```

I leave this terminal open. The API should be available at:

```
http://localhost:8080
```

### Start the frontend

I open a third PowerShell window and run:

```
Set-Location apps/booking-form
py -m http.server 8000
```

Then I open:

```
http://localhost:8000
```

I must not double-click `index.html`, because the browser will then use a `file://` URL. The frontend is meant to run through the local HTTP server.

On Linux or macOS, I run the equivalent commands like this:

```bash
cd services/booking-service
mvn clean spring-boot:run
```

In another terminal:

```bash
cd apps/booking-form
python3 -m http.server 8000
```

I can also serve the frontend from the repository root with:

```bash
make frontend
```

## Check that the API is running

Health check:

```
Invoke-RestMethod http://localhost:8080/api/v1/health
```

Linux or macOS with `curl`:

```bash
curl http://localhost:8080/api/v1/health
```

Expected result:

```
status service
------ -------
ok     booking-api
```

Availability check:

```
Invoke-RestMethod "http://localhost:8080/api/v1/availability/slots?serviceId=gel-overlay-art&date=2026-10-03"
```

Linux or macOS:

```bash
curl "http://localhost:8080/api/v1/availability/slots?serviceId=gel-overlay-art&date=2026-10-03"
```

## Create a test booking

```
$body = @{
    clientName = "Amina Patel"
    phone = "+27821234567"
    email = "amina@example.com"
    serviceId = "gel-overlay-art"
    preferredDate = "2026-10-03"
    preferredTime = "14:00"
    notes = "Chrome finish with floral detail"
    source = "website"
} | ConvertTo-Json

Invoke-RestMethod `
  -Uri "http://localhost:8080/api/v1/bookings" `
  -Method Post `
  -ContentType "application/json" `
  -Body $body
```

The API calculates the duration and end time from the service stored in the database. This is important because I should not trust the browser to tell the backend how long a service takes or what it costs.

## Understanding the three statuses

I keep these statuses separate because they describe different decisions:

| Status group | Example values | What it means |
| --- | --- | --- |
| Booking status | `PENDING`, `CONFIRMED`, `CANCELLED`, `COMPLETED` | The lifecycle of the appointment |
| Approval status | `PENDING_REVIEW`, `APPROVED`, `REJECTED` | Whether the business accepted the request |
| Payment status | `NOT_STARTED`, `PENDING`, `PAID`, `FAILED`, `REFUNDED` | What happened with payment |

For example, a booking can be approved but not paid yet. It should only become a calendar appointment when the business rules say that it is ready.

## Testing

Unit tests do not require Docker because the repositories are mocked:

```
Set-Location services/booking-service
mvn clean test
```

The acceptance tests need the API to be running:

```
python tests/acceptance/test_api.py
```

I can also use the scripts in `scripts/` for quick API checks.

## n8n integration plan

I am using the following boundary:

```
1. API validates the booking.
2. API saves the booking in PostgreSQL.
3. API sends a booking.created event to n8n.
4. n8n performs outside actions.
```

This is called **Pattern A**. It is the right first version for this project because the database remains the source of truth. If n8n is temporarily unavailable, the booking should still be saved rather than disappearing.

The first n8n workflow will validate the event and acknowledge it. I will add WhatsApp, payment, and calendar integrations only after that simple flow works.

For a more production-ready version, I plan to add a transactional outbox. That means the API saves the booking and an outgoing event in the same database transaction. A publisher can retry delivery to n8n if the first request fails.

## Data and security rules

Before using this with real clients, I need to add:

- HTTPS for public traffic.

- Authentication for owner-only operations.

- A protected n8n webhook using a secret or signature.

- Real payment-provider verification through webhooks.

- Privacy and retention rules for client contact details and inspiration images.

- Backups for PostgreSQL.

- Idempotency so retries do not create duplicate bookings, payment links, messages, or calendar events.

- Monitoring and an operator-friendly error view.

The current version is a working local prototype. It is not yet ready to be exposed publicly with real customer data.

## My learning sequence

I am following this order so I understand each layer instead of hiding problems behind automation tools:

1. Run PostgreSQL, the API, and the frontend locally.

1. Test availability directly.

1. Create a booking from the form.

1. Test conflicts and invalid requests.

1. Send a sample booking event to n8n.

1. Connect the API to the n8n webhook after persistence.

1. Add owner notifications.

1. Add approval handling.

1. Add a payment provider in test mode.

1. Add payment status updates.

1. Add calendar creation.

1. Improve the delivery mechanism with an outbox and retries.

1. Add JMS and ActiveMQ as a second event-driven implementation for learning.

## Git checkpoint

When I have tested a meaningful change, I can save it with:

```
git add .
git commit -m "feat(booking ): improve appointment workflow"
git status
```

I never commit `.env`, real API keys, passwords, or generated `target/` files.

## Other documentation

- `docs/local-development.md` explains local commands.

- `docs/system-design.md` explains the system boundaries.

- `docs/booking-api-contract.md` explains the API requests and responses.

- `docs/booking-state-machine.md` explains the booking states.

- `docs/testing-guide.md` explains the different test types.

- `docs/ci-cd.md` explains the GitHub Actions checks.

- `workflows/n8n/README.md` explains the automation connection.

I am treating this as both a portfolio project and the foundation of a small-business product. The next goal is not to add every possible feature. The next goal is to make one complete journey reliable: request, review, payment, and calendar confirmation.