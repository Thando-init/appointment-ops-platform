# n8n workflow guide

The importable workflow is [`booking-intake.json`](booking-intake.json). It contains the booking-intake, owner-approval, and demo-payment webhook paths. Keep this file as the versioned workflow export; n8n credentials are configured in the n8n instance, not stored as secrets in this guide.

## What the local demo does

The API remains the source of truth. The workflow fetches the booking from the API before acting on it.

```
Booking-created webhook
  → validate event
  → fetch authoritative booking
  → acknowledge intake / reject invalid event
  → send owner email

Owner-approval webhook
  → update approval through the API
  → approved: create a demo payment link
  → rejected: update the booking

Demo payment-confirmed webhook
  → update payment status through the API
  → check for an existing calendar event
  → create Google Calendar event when needed
  → save the returned event ID in the API
```

The payment link is a **simulation**. Sending `paymentStatus: PAID` to the demo webhook does not verify or collect real payment. The calendar node uses the Google Calendar credential configured in n8n and can create a real event. Only test with fictional booking details unless you intentionally want to create an event in that calendar.

The workflow has been manually exercised against the local API: the intake path returned an acknowledgement and sent the owner email; approval produced a demo payment URL; payment confirmation created a calendar event and saved its event ID. A calendar-credential failure can occur after the booking is marked paid. Check the booking and n8n execution before retrying, since a retry may duplicate an external side effect if an event was created but its ID was not saved.

## Import and configure locally

1. Start the local services from the repository root:

   ```bash
   docker compose up -d postgres activemq n8n
   ```

1. Start the Spring Boot API as described in the repository's [local development guide](../../docs/local-development.md).

1. Open n8n at `http://localhost:5678` and import `booking-intake.json`.

1. Configure the **Send an Email** node using the intended test mailbox. Configure the **Create Calendar Event** node with the intended Google Calendar credential.

1. Keep n8n's local HTTP Request URLs pointed at the local API. The imported workflow uses `host.docker.internal` for this local setup.

Do not publish n8n credentials, OAuth tokens, webhook secrets, execution data containing personal details, or live customer records. The exported workflow may still reveal implementation details such as endpoint paths and calendar configuration; review it before sharing screenshots or exports publicly.

## Test webhook paths

For each test request, click **Execute workflow** in n8n immediately before sending it. Test webhooks register for one call at a time. Use the test paths below while working locally; use production paths only after activating the workflow and setting the correct deployment URLs.

| Purpose | Local test path |
| --- | --- |
| Booking intake | `http://localhost:5678/webhook-test/booking-created` |
| Owner approval | `http://localhost:5678/webhook-test/owner-approval` |
| Demo payment confirmation | `http://localhost:5678/webhook-test/payment-confirmed` |

Send the intake example with:

```bash
curl -i -X POST "http://localhost:5678/webhook-test/booking-created" \
  -H "Content-Type: application/json" \
  --data-binary @workflows/n8n/test-payload.json
```

[`test-payload.json`](test-payload.json) is a sample event. [`invalid-test-payload.json`](invalid-test-payload.json) exercises validation failure. A valid intake request should return HTTP 200; an invalid event should be rejected with HTTP 400.

To exercise the owner-approval webhook, replace the reference with a disposable test booking and send:

```bash
curl -i -X POST "http://localhost:5678/webhook-test/owner-approval" \
  -H "Content-Type: application/json" \
  -d '{"bookingReference":"SALON-REPLACE-ME","approvalStatus":"APPROVED"}'
```

The approved branch creates a demo payment URL. The browser payment page sends the demo payment confirmation webhook when its simulation button is clicked. Do not repeat an approval or payment request after an error until you inspect the booking API state and the failed n8n execution.

## API paths used by the workflow

The workflow calls the API to fetch a booking and update approval, payment, and calendar-event state:

```
GET  /api/v1/bookings/{bookingReference}
POST /api/v1/bookings/{bookingReference}/approval
POST /api/v1/bookings/{bookingReference}/payment-link
POST /api/v1/bookings/{bookingReference}/payment-confirmation
POST /api/v1/bookings/{bookingReference}/calendar-event
```

The calendar-event ID guard supports a safe retry after the API has recorded the created event. It cannot eliminate every possible duplicate if the external calendar creates an event but the API never receives or stores its ID.

## Before real customers

This is a local portfolio prototype, not a production payment system. Before using real customer data, add owner authentication, authenticated or signed webhooks, HTTPS, payment-provider verification, idempotency for every external side effect, database backups, privacy/retention controls, and monitored retry/error handling. See the [owner approval notes](../../docs/owner-approval-setup.md), [demo operations](../../docs/real_demo_operations.md), and [portfolio case study](../../docs/portfolio-case-study.md).