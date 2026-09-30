# Appointment Ops Platform — Portfolio Case Study

## Summary

Appointment Ops is a portfolio-ready appointment management platform for a Johannesburg beauty studio. It combines a Spring Boot API, PostgreSQL, ActiveMQ Artemis, a static booking website, and self-hosted n8n automation.

The central design decision is to keep booking state and business rules in the API while using n8n to coordinate external side effects.

## End-to-end lifecycle

```text
Website booking
  -> Spring Boot validation and PostgreSQL persistence
  -> ActiveMQ booking.created event
  -> n8n production webhook
  -> Gmail owner notification
  -> Owner approval/rejection
  -> Demo payment link and payment confirmation
  -> Authenticated Google Calendar API request
  -> Google event ID persisted to PostgreSQL
```

## Engineering highlights

- Server-side availability and overlap checks with a 15-minute cleanup buffer.
- South Africa local-time handling using `Africa/Johannesburg`.
- Transaction-aware JMS publication after a booking commits.
- Authoritative booking lookup in n8n before external side effects.
- Approval-before-payment protection in the API.
- Google Calendar OAuth integration through n8n's authenticated HTTP Request node.
- Calendar event ID persistence and retry-safe idempotency.
- Demo adapters for payment and cancellation so the full journey is demonstrable without processing real money.
- Owner-reviewed cancellation policy with `REFUND_PENDING` rather than automatic refunds.
- Unit-tested service layer with 15 passing tests.

## Why the HTTP Request calendar node is intentional

The built-in n8n Google Calendar node returned an unhelpful Google `400 Bad Request` for dynamic appointment values, even after the n8n upgrade. The authenticated HTTP Request node successfully creates the same official Google Calendar API event and exposes the returned provider ID clearly. This is a maintainable integration, not a workaround that bypasses Google APIs.

## Demo boundaries

The following are intentionally demo-safe rather than production provider implementations:

- Payment confirmation endpoint.
- Payment-link generation.
- Owner approval endpoint without authentication.
- Cancellation without automatic refunds.

A production deployment would add signed provider webhooks, owner authentication, HTTPS, expiring approval tokens, audit history, and a real payment provider.

## Evidence to capture

1. Booking website with available slots.
2. Successful booking API response with booking reference.
3. n8n workflow canvas and successful execution.
4. Owner email with booking details.
5. Approval response with payment URL.
6. Google Calendar event with Johannesburg-local appointment time.
7. API response showing `APPROVED`, `PAID`, `CONFIRMED`, and `calendarEventId`.
8. A replayed payment webhook showing the existing-calendar-event branch and no duplicate event.

## Suggested portfolio description

> Built an appointment operations platform that connects a Spring Boot booking API to PostgreSQL, ActiveMQ, and self-hosted n8n. The system validates availability, routes booking events through an owner approval workflow, verifies demo payment state, creates real Google Calendar events through OAuth, persists the provider event ID, and protects retries against duplicate calendar entries. The architecture separates authoritative business state from external automation side effects and documents the security steps required before production deployment.
