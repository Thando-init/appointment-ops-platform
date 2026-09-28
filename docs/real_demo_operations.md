# Real Demo Operations

## Booking to n8n

After a booking commits, the Spring Boot JMS consumer can forward the serialized `booking.created` message to the active n8n production webhook. Configure the API before starting it:

```bash
export N8N_BOOKING_CREATED_WEBHOOK_URL=http://localhost:5678/webhook/booking-created
```

On Windows PowerShell:

```powershell
$env:N8N_BOOKING_CREATED_WEBHOOK_URL = "http://localhost:5678/webhook/booking-created"
```

The n8n workflow must be **active** for this production URL. Test URLs are still useful for manual testing, but the API should not point at `/webhook-test/...` for normal demo operation.

If the variable is blank, the JMS consumer remains logging-only, so local API development does not depend on n8n being online.

## Owner email

The canonical workflow includes an unconnected, disabled `Notify Owner by Email` node. This preserves the working intake path until SMTP credentials are configured.

In n8n:

1. Create an SMTP credential in **Credentials → New → Send Email (SMTP)**.
2. Open `Notify Owner by Email`.
3. Set the sender and owner email.
4. Connect `Fetch Authoritative Booking` to `Notify Owner by Email`, then connect it to `Acknowledge Intake`.
5. Enable the node and activate the workflow.

The node uses the n8n Send Email node and should be tested with a real email address only after SMTP credentials are saved in n8n.

## Cancellation policy for this demo

The selected first policy is deliberately safe:

- The website/API can cancel a booking.
- A cancellation changes `bookingStatus` to `CANCELLED`.
- If the booking was paid, `refundStatus` becomes `REFUND_PENDING`.
- No money is refunded automatically.
- An owner must review and execute any real refund through the payment provider.

Endpoint:

```text
POST /api/v1/bookings/{bookingReference}/cancellation
```

Example:

```bash
curl -i -X POST \
  "http://localhost:8080/api/v1/bookings/SALON-9475E2CB/cancellation" \
  -H "Content-Type: application/json" \
  -d '{"reason":"Client requested cancellation"}'
```

## Calendar behavior

The current calendar step is a demo adapter:

```text
POST /api/v1/bookings/{bookingReference}/calendar-confirmation
```

It marks the booking `CONFIRMED`. A real Google Calendar or Microsoft Graph node should be inserted before this API call, and the created provider event ID should be persisted before confirmation. The booking API remains the source of truth.

## Amendments and AI

The recommended design is:

1. A website amendment form is the authoritative path.
2. An email or WhatsApp message may be parsed by AI into a proposed change.
3. The proposed change is shown to the owner for approval.
4. Only the approved change updates the booking and calendar.

AI must not independently cancel bookings or issue refunds. This avoids ambiguous dates, identity mistakes, and irreversible financial actions.
