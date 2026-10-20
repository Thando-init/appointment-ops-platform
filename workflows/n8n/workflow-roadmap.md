# n8n workflow roadmap

The importable workflow is [`workflows/n8n/booking-intake.json`](../workflows/n8n/booking-intake.json). The operational steps for importing and testing it are in the [n8n workflow guide](../workflows/n8n/README.md). This page records current scope and what remains before production use.

## Current milestone status

| Area | Current state |
| --- | --- |
| Booking intake | Webhook validates the event, fetches the authoritative booking from the API, and returns success or validation-error responses. Manually tested locally. |
| Owner notification | Email node is configured in n8n and was observed sending the local demo notification. Requires the user's own n8n credential. |
| Owner decision | Approval and rejection update the booking through the API. The owner-approval webhook is a demo path, not an authenticated public approval system. |
| Payment link | The approved path returns a local demo payment URL. It does not create a real provider payment link. |
| Payment confirmation | The demo webhook can mark an approved booking `PAID`; it does not verify a provider signature or transaction. |
| Calendar | The workflow creates a Google Calendar event and saves its event ID through the API. The API's saved ID is checked before a retry. The external calendar credential is real even though payment is simulated. |
| Client confirmation / messaging | Not a production-ready provider integration. |

These checks describe a manually exercised local demo, not a production service-level guarantee. Use fictional booking details and inspect the API state and n8n execution before retrying a failed run.

## Target flow

```
Website or other client
  → API validates and saves booking
  → durable event delivery
  → n8n fetches current booking
  → owner is notified and reviews request
  → approved request receives provider payment link
  → provider-signed callback is verified by the API
  → calendar event is created once and its ID persisted
  → client receives confirmation
```

The API and PostgreSQL remain the source of truth. n8n coordinates external actions and must not be trusted to invent booking state.

## Production work still required

Before real customer use, add owner authentication or expiring signed approval links; authenticate or sign incoming webhooks; replace the demo payment adapter with a payment provider and server-side verification; add end-to-end idempotency for notifications, payment links, and calendar creation; and add backups, privacy/retention controls, monitoring, and retry handling. A transactional outbox would close the remaining gap between committing a booking and reliably delivering its event.

See the [owner approval setup](owner-approval-setup.md) and [demo operations](real_demo_operations.md) for the current implementation details.