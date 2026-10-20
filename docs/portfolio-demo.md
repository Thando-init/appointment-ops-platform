# Appointment Operations Platform — Demo

A local portfolio prototype for a salon appointment journey, from checking availability through owner review and calendar automation. The demo combines a static booking website, a Spring Boot API, PostgreSQL, ActiveMQ Artemis, and n8n.

> **Demo boundary:** payment is simulated. The demo does not charge a card or verify a real payment-provider callback. The workflow can send an owner email and create a real Google Calendar event through the configured n8n credential. Use fictional test data when reproducing the flow.

## Demo journey

1. A client selects a service and an available time.
2. The API validates and persists the booking request.
3. The booking-created event enters the automation workflow and an owner notification is sent.
4. The owner approves the request; the API records the approval and the demo flow returns a payment link.
5. The payment page simulates a successful payment callback.
6. n8n creates a calendar event and saves the provider event ID through the API, with an existing-event check before retrying.

## Screenshots

### 1. Service and availability selection

The booking form shows available services, pricing, and appointment slots.

![Booking service and available slots](assets/portfolio-demo/01-booking-service-and-available-slots.png)

### 2. Booking request details

The client enters contact details and appointment preferences before submitting the request.

![Booking request details](assets/portfolio-demo/02-booking-request-details.png)

### 3. Booking request received

The form confirms that the request was submitted for studio review.

![Booking request confirmed](assets/portfolio-demo/03-booking-request-confirmed.png)

### 4. Owner notification

n8n sends an owner email with the request details and a review link.

![Owner notification email](assets/portfolio-demo/04-owner-notification-email.png)

### 5. n8n workflow overview

The workflow separates booking intake, owner approval, and payment/calendar processing into connected automation paths.

![n8n workflow overview](assets/portfolio-demo/05-n8n-workflow-overview.png)

### 6. Owner review

The owner-facing page shows the request and its approval state.

![Owner review status](assets/portfolio-demo/06-owner-review-status.png)

### 7. Approved request and payment link

After approval, the owner view exposes the demo payment step.

![Approved booking and payment link](assets/portfolio-demo/07-approved-booking-payment-link.png)

### 8. Demo payment page

The page explicitly states that no real money is processed and offers a simulated success action.

![Demo payment pending](assets/portfolio-demo/08-demo-payment-pending.png)

### 9. Demo payment confirmed

The page reports the simulated payment confirmation and that the appointment is being added to Google Calendar.

![Demo payment confirmed](assets/portfolio-demo/09-demo-payment-confirmed.png)

## Architecture and engineering highlights

```text
Browser booking form
        ↓ HTTP
Spring Boot API ── JDBC/Flyway ── PostgreSQL
        ↓ booking.created after commit
ActiveMQ Artemis → Java JMS consumer → n8n webhook
                                          ├── owner notification
                                          ├── approval and demo payment
                                          └── Google Calendar event
                                                  ↓
                                      calendar event ID saved via API
```

The API is the source of truth for bookings and applies server-side availability rules, overlap checks, and lifecycle transitions. n8n coordinates external effects rather than owning booking state. The calendar event ID is persisted so later executions can check for an existing event before attempting another creation.

## Technology

Java 21, Spring Boot, PostgreSQL, JDBC, Flyway, ActiveMQ Artemis, n8n, HTML, CSS, vanilla JavaScript, Maven, JUnit, Mockito, Docker Compose, and GitHub Actions.

## Running the local demo

See the repository's root [README](../README.md) for the quick start and `docs/local-development.md` for the full setup. The complete lifecycle also requires the local n8n workflow and its configured email and Google Calendar credentials. The workflow can produce real email and calendar side effects even though its payment step is simulated.

## Production work not represented by this demo

This prototype is not ready for real public bookings. A production version needs owner authentication, authenticated or signed webhook requests, real payment-provider integration and server-side verification, stronger idempotency across external side effects, secure hosted URLs, database backups, monitoring, and privacy/retention controls.
