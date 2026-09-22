# Appointment Ops — Canonical n8n Workflow

This is the single workflow plan for the appointment operations platform. It is intentionally built as one flow architecture rather than a sequence of replacement exports.

## Current canonical path

```
Booking Created Webhook
        ↓
Validate Booking Event
        ↓ valid
Fetch Authoritative Booking
        ↓
Acknowledge Event
```

Invalid events go to `Reject Invalid Event` and return HTTP 400. The authoritative lookup calls the Spring Boot API using the booking reference before n8n is allowed to perform side effects.

## Complete target architecture

```
┌──────────────────────────┐
│ Booking Created Webhook  │
└────────────┬─────────────┘
             ↓
┌──────────────────────────┐
│ Validate Event            │──invalid──> Reject HTTP 400
└────────────┬─────────────┘
             ↓
┌──────────────────────────┐
│ Fetch Authoritative      │
│ Booking from API         │
└────────────┬─────────────┘
             ↓
┌──────────────────────────┐
│ Register Event Idempotency│──duplicate──> Acknowledge Duplicate
└────────────┬─────────────┘
             ↓ new event
┌──────────────────────────┐
│ Notify Business Owner    │
└────────────┬─────────────┘
             ↓
┌──────────────────────────┐
│ Owner Approval Webhook   │
└───────┬───────────┬──────┘
        │ approved  │ rejected
        ↓           ↓
┌──────────────┐  ┌────────────────────┐
│ Create        │  │ Update Booking    │
│ Payment Link  │  │ Rejected          │
└──────┬───────┘  └─────────┬──────────┘
       ↓                    ↓
┌──────────────┐  ┌────────────────────┐
│ Send Payment  │  │ Notify Client     │
│ Link          │  │ Rejection         │
└──────┬───────┘  └────────────────────┘
       ↓
┌──────────────────────────┐
│ Payment Provider Webhook │
└────────────┬─────────────┘
             ↓
┌──────────────────────────┐
│ Verify Payment with API  │
└────────────┬─────────────┘
             ↓ paid
┌──────────────────────────┐
│ Create Calendar Event    │
└────────────┬─────────────┘
             ↓
┌──────────────────────────┐
│ Update Booking Confirmed │
└────────────┬─────────────┘
             ↓
┌──────────────────────────┐
│ Send Final Confirmation  │
└──────────────────────────┘
```

## Build order inside this same workflow

| Phase | Nodes | Current state |
| --- | --- | --- |
| Intake | Booking webhook, validation, authoritative lookup, acknowledgement | Working and tested |
| Idempotency | Register/check `eventId` or booking reference | Next implementation |
| Owner decision | Approval webhook, approved/rejected branches, booking update | Planned |
| Payment | Payment-link creation, payment webhook, provider verification | Planned; requires provider credentials |
| Calendar | Calendar creation and duplicate protection | Planned; requires calendar credentials |
| Communications | Owner/client notifications and failure alerts | Planned; requires provider credentials |

## Endpoint contracts needed before provider nodes

```
GET  /api/v1/bookings/{bookingReference}
POST /api/v1/events/{eventId}/claim
POST /api/v1/bookings/{bookingReference}/approval
POST /api/v1/bookings/{bookingReference}/payment-verification
POST /api/v1/bookings/{bookingReference}/confirmation
```

The first endpoint exists. The remaining endpoints must be implemented before their corresponding n8n nodes are enabled; placeholder nodes should not silently mutate booking state.

## Operating rule

The imported workflow file is the canonical artifact. Future changes should be made by editing and re-importing the same workflow only when necessary, or directly in the n8n editor after the workflow has been imported. We should not create separate replacement workflows for each step.