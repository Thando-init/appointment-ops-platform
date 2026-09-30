# Owner Approval Setup

## Current demo behavior

The booking-created workflow sends the owner an email after the Spring Boot API creates a booking and n8n fetches the authoritative record. The approval webhook is currently a demo adapter:

```text
POST /webhook/owner-approval
```

The owner can review the request in the static dashboard at:

```text
http://localhost:8000/owner.html?bookingReference=SALON-XXXXXXXX
```

The dashboard loads the authoritative booking details and provides **Approve request** and **Reject request** buttons. The buttons call the existing API approval endpoint; the owner does not need a terminal. Before exposing this publicly, protect the approval action with owner authentication or a signed, expiring approval token.

## Recommended email node

In the canonical n8n workflow, configure the existing `Notify Owner by Email` node after `Fetch Authoritative Booking` and before `Acknowledge Intake`.

Use the saved SMTP credential and real owner address. Do not store passwords in node fields or exported workflow JSON.

### Subject

```text
New appointment request {{$json.bookingReference}}
```

### Message

```text
New appointment request

Booking reference: {{$json.bookingReference}}
Client: {{$json.clientName}}
Phone: {{$json.phone}}
Email: {{$json.email || 'Not provided'}}
Service: {{$json.serviceId}}
Date: {{$json.date}}
Time: {{$json.start}} - {{$json.end}}
Notes: {{$json.notes || 'None'}}

Approve or reject this request in the owner workflow.
```

## Add a dashboard link to the email

In the n8n email node, add this line to the message:

```text
Review this request: http://localhost:8000/owner.html?bookingReference={{$json.bookingReference}}
```

For a deployed site, replace `http://localhost:8000` with the HTTPS owner-dashboard URL. The current local dashboard is intentionally a demo adapter and has no login.

## Test the owner decision

Use a booking reference from a successful intake execution:

```bash
curl -i -X POST \
  "http://localhost:5678/webhook/owner-approval" \
  -H "Content-Type: application/json" \
  --data-raw '{"bookingReference":"SALON-XXXXXXXX","approvalStatus":"APPROVED"}'
```

For test mode, click **Execute workflow** on the owner approval webhook and use `/webhook-test/owner-approval` instead.

An approved request should return the demo payment link. A rejected request should update the booking and return the rejection response.

## Production hardening

The demo endpoint is intentionally simple. Before public deployment:

- Require owner authentication.
- Use a short-lived, single-use approval token rather than exposing a raw booking reference.
- Validate that the token maps to the intended booking and owner.
- Record who approved or rejected the request and when.
- Use HTTPS for all webhook and approval URLs.
- Keep payment verification provider-signed and separate from the demo payment endpoint.

## Dashboard decision

Email is the best first owner experience because it is already integrated and easy to demonstrate. A dashboard should be the next product iteration after authentication is added; it should list pending bookings, show payment/calendar status, and call the same protected approval API rather than duplicating business rules.
