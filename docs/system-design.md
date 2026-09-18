# System design

## Purpose

The Appointment Operations Platform is a learning project for a small beauty business. It accepts appointment requests from a website and creates a reliable record that can later be automated through n8n.

## Design goals

1. Do not lose a booking when an external provider is unavailable.
2. Keep scheduling rules on the server, not only in browser JavaScript.
3. Keep booking, approval, and payment states independent.
4. Make the first version easy to run locally and inspect.
5. Add automation around a reliable booking record rather than replacing the record with a workflow execution.

## High-level architecture

```text
+-----------------------+
| Static booking form   |
| HTML/CSS/JavaScript   |
+-----------+-----------+
            |
            | HTTP JSON
            v
+-----------------------+        +-----------------------+
| Spring Boot API       |------->| PostgreSQL            |
| validation/services   | JDBC   | services/schedule/   |
| booking endpoints     |        | bookings/holds        |
+-----------+-----------+        +-----------------------+
            |
            | future booking.created event
            v
+-----------------------+
| n8n orchestration     |
| WhatsApp/payment/     |
| calendar notifications|
+-----------------------+
```

## Component responsibilities

### Static frontend

The browser presents the service catalogue, asks the API for available slots, collects contact details, and sends the minimum booking payload. It is not trusted to decide duration, price, approval, or whether a slot is still available.

### Spring Boot API

The API owns request validation, service lookup, business-hour checks, blocked-period checks, overlap checks, status assignment, and database transactions.

### PostgreSQL

PostgreSQL is the durable source of truth. Flyway creates the schema. The database stores the service catalogue, opening hours, blocked periods, bookings, and temporary holds.

### n8n

n8n coordinates side effects after persistence: sending messages, generating payment links, receiving payment webhooks, and creating calendar events. n8n should not be the only place where a booking exists.

## Booking creation flow

```text
1. Browser selects service/date/time.
2. Browser requests availability.
3. API calculates candidate slots from database rules.
4. Browser submits client details and selected slot.
5. API looks up trusted service duration.
6. API derives the end time.
7. API adds the cleanup buffer.
8. API checks business hours and blocked periods.
9. API performs a final active-booking overlap check.
10. API inserts a PENDING booking.
11. API returns a booking reference.
12. n8n can perform external follow-up work.
```

## Data ownership

| Data | Owner | Reason |
|---|---|---|
| Service duration and price | PostgreSQL/API | Browser values can be changed |
| Opening hours | PostgreSQL/API | All clients must use the same schedule |
| Slot availability | API | Must be recalculated at submission |
| Booking lifecycle | PostgreSQL/API | Durable business record |
| WhatsApp delivery | n8n/provider | External side effect |
| Payment confirmation | Payment provider webhook/API | Client claims are not proof |
| Calendar event | n8n/calendar provider | Derived side effect |

## Status model

The system uses three independent state dimensions:

```text
booking_status  = PENDING, CONFIRMED, CANCELLED
approval_status = PENDING_REVIEW, APPROVED, REJECTED
payment_status  = NOT_STARTED, PENDING, PAID, FAILED, REFUNDED
```

This avoids a single status field becoming ambiguous. For example, an appointment can be approved but still unpaid.

## Important trade-offs

### PostgreSQL instead of SQLite

PostgreSQL better represents the expected deployed system, supports multiple concurrent clients, and gives a realistic persistence and integration experience. SQLite would be simpler for a single-user prototype, but it would hide important concurrency and deployment considerations.

### Static frontend instead of React

The static frontend keeps the first version easy to open, inspect, and learn. A framework can be introduced later when the form needs routing, reusable components, or a larger dashboard.

### n8n after API persistence

Persisting first avoids losing requests when messaging, payment, or calendar providers fail. It also gives n8n an idempotent booking reference to use when retrying.

## Current limitations

- The API does not yet publish an outbound event automatically.
- Status-update endpoints are not yet implemented.
- Payment and WhatsApp providers are not connected.
- PostgreSQL repository integration tests are not yet automated with Testcontainers.
- A production deployment still needs HTTPS, authentication, rate limiting, backups, and monitoring.

## Next architecture milestone

Add a transactional outbox or post-commit event publisher:

```text
Booking transaction
        ↓
Outbox event stored with booking
        ↓
Publisher sends booking.created
        ↓
n8n processes event idempotently
```

An outbox prevents a booking from being saved successfully while its automation event is lost.
