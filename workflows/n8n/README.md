# My n8n booking automation guide

## Why I am using n8n

I am using n8n for the work that happens around a booking rather than for the core booking record itself.

The Spring Boot API owns the important booking data and business rules. PostgreSQL stores that data. n8n coordinates outside actions such as sending messages, creating payment links, and adding confirmed appointments to a calendar.

This separation matters because a temporary WhatsApp, payment, calendar, or n8n problem should not make the booking disappear from my database.

In simple terms:

```
Spring Boot decides whether a booking is valid and saves it.
n8n helps people and other services respond to that booking.
```

## The real-world customer journey

The finished salon workflow should look like this:

```
Client submits a request from the website or WhatsApp
        ↓
API checks the service, date, time, and contact details
        ↓
API saves the request in PostgreSQL
        ↓
API sends a booking.created event to n8n
        ↓
n8n notifies the business owner
        ↓
Owner approves or rejects the request
        ↓
If approved, n8n creates and sends a payment link
        ↓
Payment provider sends a payment webhook
        ↓
API records the payment result
        ↓
n8n creates a calendar event when the rules are satisfied
        ↓
n8n sends the final confirmation to the client
```

The first version will use the website. I can add WhatsApp after the website-to-API-to-n8n journey is working.

## Where JMS and ActiveMQ now fit

The Java project now includes a first JMS message path:

```
Spring Boot saves the booking
        ↓ after the transaction commits
ActiveMQ queue: booking.created
        ↓
Java JMS consumer reads the JSON message
        ↓ next implementation step
Consumer calls the n8n webhook
```

The current consumer logs the booking reference. This proves the API-to-broker-to-consumer path before I add a real HTTP request to n8n. The database is still the source of truth, and n8n remains responsible for external actions.

I can start the broker from the project root with:

```bash
docker compose up -d postgres activemq
```

The ActiveMQ development console is available at `http://localhost:8161`. The local credentials are documented in `.env.example` and must be changed before any public deployment.

## The pattern I am using first

I am starting with **Pattern A: the API calls n8n after the booking has been saved**.

```
Frontend
   ↓
Spring Boot API
   ↓
Validate the booking
   ↓
Save booking in PostgreSQL
   ↓
Call the n8n webhook
   ↓
n8n performs notifications and follow-up work
```

This is better than calling n8n before saving because the API remains the source of truth.

If n8n is unavailable, I still want the booking to exist. I can then retry the automation instead of losing the customer's request.

## What the API should send

I should send a stable event envelope instead of sending random fields directly from the browser.

The event needs identifiers that allow me to trace and safely retry it:

- `eventId` identifies the event.

- `eventType` explains what happened.

- `bookingReference` identifies the booking.

- `occurredAt` records when the event happened.

- The status fields explain the current state.

Example:

```json
{
  "eventType": "booking.created",
  "eventId": "evt_demo_001",
  "occurredAt": "2026-10-03T12:00:00+02:00",
  "bookingReference": "SALON-AB12CD34",
  "bookingStatus": "PENDING",
  "approvalStatus": "PENDING_REVIEW",
  "paymentStatus": "NOT_STARTED",
  "client": {
    "name": "Amina Patel",
    "phone": "+27821234567",
    "email": "amina@example.com"
  },
  "appointment": {
    "serviceId": "gel-overlay-art",
    "startsAt": "2026-10-03T14:00:00+02:00",
    "endsAt": "2026-10-03T16:00:00+02:00"
  }
}
```

The API must calculate authoritative values such as service duration and appointment end time. I should not trust the browser or n8n to calculate those values.

## What the current workflow does

The current `booking-intake.json` is a starter workflow. It is intentionally small so I can understand the data before adding real providers.

It currently demonstrates:

```
Webhook receives an event
        ↓
Workflow checks required fields
        ↓
Valid event is acknowledged
        ↓
Invalid event is rejected
```

It does not yet send a real WhatsApp message, create a real payment link, or create a real calendar event.

## How I import and test the workflow

1. Start n8n locally or open my hosted n8n workspace.

1. Import `booking-intake.json`.

1. Open the Webhook node.

1. Use the test URL while the workflow is listening for a test event.

1. Send the contents of `test-payload.json` with an HTTP client.

1. Inspect the execution data in each node.

1. Confirm that the event fields remain linked correctly.

1. Test an invalid payload as well.

The payload file is:

```
workflows/n8n/test-payload.json
```

## Example request to the webhook

The exact URL depends on whether I am using n8n locally or a hosted workspace. The request body can be sent with PowerShell like this:

```
$payload = Get-Content .\workflows\n8n\test-payload.json -Raw

Invoke-RestMethod `
  -Uri "http://localhost:5678/webhook-test/booking-created" `
  -Method Post `
  -ContentType "application/json" `
  -Body $payload
```

For a hosted n8n instance, I replace the URL with the webhook URL shown by n8n. I do not commit that URL if it contains sensitive information.

On Linux or macOS, I can send the same payload with `curl`:

```bash
curl -X POST \
  "http://localhost:5678/webhook-test/booking-created" \
  -H "Content-Type: application/json" \
  --data-binary @workflows/n8n/test-payload.json
```

If I am already inside `workflows/n8n`, I can use:

```bash
curl -X POST \
  "http://localhost:5678/webhook-test/booking-created" \
  -H "Content-Type: application/json" \
  --data-binary @test-payload.json
```

## The next nodes I plan to add

After the starter workflow works, I will add nodes in this order:

### 1. Fetch authoritative booking data

n8n should use `bookingReference` to call the API and retrieve the current booking. This protects me from acting on stale or modified data.

### 2. Check whether the event was already processed

I will use `eventId` or `bookingReference` to prevent duplicate side effects. A retry must not send two payment links or create two calendar events.

### 3. Notify the business owner

The owner should receive the request with the service, date, time, client name, and any notes or inspiration instructions.

### 4. Handle approval

The owner needs a clear way to approve or reject the request. The API should record that decision, while n8n handles the message or interface around it.

### 5. Create a payment link

Only an approved booking should proceed to payment. The payment provider must create the link, and n8n should send it to the client.

### 6. Receive payment confirmation

A screenshot or client message is not enough for a production system. The payment provider must call a protected webhook, and the API must verify the payment before changing `paymentStatus` to `PAID`.

### 7. Create a calendar event

The calendar event should only be created when the booking has been approved and the payment rule has been satisfied.

### 8. Send the final confirmation

The final message should include the confirmed date, time, service, location information, and cancellation or payment terms.

## Failure handling

I am designing the workflow around the idea that outside services can fail.

| Problem | What should happen |
| --- | --- |
| n8n is unavailable after the booking is saved | Keep the booking and retry the event later |
| WhatsApp is unavailable | Keep the booking and retry the notification |
| Owner rejects the booking | Update approval status and notify the client politely |
| Payment-link creation fails | Keep payment as `NOT_STARTED` and alert the owner |
| Client pays but callback is delayed | Keep payment as `PENDING` until the provider confirms it |
| A duplicate event arrives | Record it as already processed and do not repeat side effects |
| Calendar creation fails | Keep the booking and retry calendar creation |
| Webhook signature is invalid | Reject the request without processing it |

## Security rules I need before real users

Before exposing the workflow publicly, I need to:

- Use HTTPS.

- Protect the webhook with a secret, header authentication, or a signature.

- Store credentials in n8n Credentials rather than inside exported JSON.

- Avoid putting payment secrets in workflow nodes.

- Avoid logging full phone numbers and payment information where possible.

- Verify payment with the provider rather than trusting a client message.

- Add authentication for owner-only approval actions.

- Add idempotency checks for every external side effect.

- Limit which fields are sent to each provider.

- Define how long client information and inspiration pictures are retained.

## Pattern A limitation and the improvement I plan to make

The first Pattern A implementation can make a direct HTTP request to n8n after saving the booking. This is good for learning and for an early prototype.

The stronger production design is a **transactional outbox**:

```
One database transaction:
  1. Save the booking
  2. Save a booking.created event in an outbox table

Publisher:
  3. Read unpublished events
  4. Send each event to n8n
  5. Mark successful events as published
  6. Retry failed events
```

This prevents the gap where the database save succeeds but the application crashes before the n8n request is made.

I am first proving the JMS message path. The next step is for the JMS consumer to call the n8n webhook. After that, I plan to implement the outbox so I can understand why production systems use it.

## JMS and ActiveMQ learning extension

I am using this project to practise the Java and messaging skills from WeThinkCode. The implemented event path is:

```
Spring Boot saves booking
        ↓
Publishes booking.created message to ActiveMQ
        ↓
Java message consumer receives it
        ↓
Consumer calls n8n or another automation service
```

The database remains the source of truth. ActiveMQ provides reliable message delivery, while n8n remains useful for business integrations.

I am keeping the consumer simple for now. Adding the n8n HTTP request is the next small step, followed by retries, idempotency, and a dead-letter queue.

## What makes this a real product rather than only a demo

The product needs more than a form. For a real nail technician or salon, I eventually need:

- Business hours and days off.

- Services with durations and prices.

- Multiple staff members or treatment rooms.

- Blocked periods and leave.

- Booking approval.

- Deposits or full payment.

- Calendar synchronisation.

- Client reminders.

- Cancellation and rescheduling rules.

- Admin authentication.

- Audit history.

- Retry and error handling.

- Backups and privacy controls.

I am building these in stages so that each part is testable and understandable.