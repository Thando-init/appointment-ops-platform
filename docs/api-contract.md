# Appointment Operations Platform API Contract

## 1. Overview

The Booking Service exposes a REST API for receiving and retrieving appointment requests.

The service is responsible for:

- Validating booking requests.
- Generating unique booking IDs.
- Persisting bookings.
- Returning structured responses.
- Providing booking status information.

The service does not send emails, WhatsApp messages, or payment links directly. Those responsibilities belong to n8n workflows and future integration services.

## 2. Base URL

Local development:

```text
http://localhost:7070
```

All API examples in this document use:

```text
http://localhost:7070
```

## 3. General conventions

### Content type

Requests containing a body must use:

```http
Content-Type: application/json
```

### Date format

Dates must use ISO 8601 format:

```text
YYYY-MM-DD
```

Example:

```text
2026-10-03
```

### Time format

Times must use 24-hour format:

```text
HH:mm
```

Example:

```text
14:00
```

### Required fields

The following fields are required when creating a booking:

- `clientName`
- `phone`
- `serviceRequested`
- `preferredDate`
- `preferredTime`

The following fields are optional:

- `email`
- `notes`
- `source`

## 4. Booking status model

### Booking status

```text
new_request
```

The booking has been received but has not yet been reviewed.

Future booking statuses:

```text
awaiting_quote
quote_approved
awaiting_slot_selection
awaiting_payment
confirmed
completed
cancelled
rejected
expired
```

### Approval status

```text
not_started
pending
approved
rejected
```

### Payment status

```text
not_started
pending
under_review
successful
failed
refunded
```

In Stage 1, every newly created booking starts with:

```json
{
  "bookingStatus": "new_request",
  "approvalStatus": "not_started",
  "paymentStatus": "not_started"
}
```

## 5. Health check

Checks whether the Booking Service is running.

### Request

```http
GET /health
```

### Successful response

Status:

```http
200 OK
```

Response:

```json
{
  "status": "ok",
  "service": "booking-service"
}
```

## 6. Create a booking

Creates a new appointment request.

### Request

```http
POST /api/v1/bookings
```

Headers:

```http
Content-Type: application/json
```

Body:

```json
{
  "clientName": "Amina Patel",
  "phone": "+27821234567",
  "email": "amina@example.com",
  "serviceRequested": "Gel overlay with nail art",
  "preferredDate": "2026-10-03",
  "preferredTime": "14:00",
  "notes": "Chrome finish with floral detail",
  "source": "website"
}
```

### Request field definitions

| Field | Type | Required | Description |
|---|---|---:|---|
| `clientName` | string | Yes | Client's full name |
| `phone` | string | Yes | Client's phone number |
| `email` | string | No | Client's email address |
| `serviceRequested` | string | Yes | Service requested by the client |
| `preferredDate` | string | Yes | Preferred appointment date in `YYYY-MM-DD` format |
| `preferredTime` | string | Yes | Preferred appointment time in `HH:mm` format |
| `notes` | string | No | Additional information from the client |
| `source` | string | No | Channel where the request originated |

Valid initial `source` values:

```text
website
whatsapp
manual
```

If `source` is not provided, the service should use:

```text
website
```

### Successful response

Status:

```http
201 Created
```

Response:

```json
{
  "bookingId": "SALON-20260913-0001",
  "clientName": "Amina Patel",
  "phone": "+27821234567",
  "email": "amina@example.com",
  "serviceRequested": "Gel overlay with nail art",
  "preferredDate": "2026-10-03",
  "preferredTime": "14:00",
  "notes": "Chrome finish with floral detail",
  "source": "website",
  "bookingStatus": "new_request",
  "approvalStatus": "not_started",
  "paymentStatus": "not_started",
  "createdAt": "2026-09-13T21:00:00Z",
  "updatedAt": "2026-09-13T21:00:00Z",
  "message": "Booking request received"
}
```

### Validation error

Returned when one or more required fields are missing or invalid.

Status:

```http
400 Bad Request
```

Response:

```json
{
  "error": "validation_failed",
  "message": "The booking request contains invalid fields",
  "fields": [
    {
      "field": "clientName",
      "message": "clientName is required"
    },
    {
      "field": "preferredDate",
      "message": "preferredDate must use YYYY-MM-DD format"
    }
  ]
}
```

### Example: missing required fields

Request:

```http
POST /api/v1/bookings
Content-Type: application/json
```

```json
{
  "email": "amina@example.com",
  "notes": "I would prefer an afternoon appointment."
}
```

Response:

```http
400 Bad Request
```

```json
{
  "error": "validation_failed",
  "message": "The booking request contains invalid fields",
  "fields": [
    {
      "field": "clientName",
      "message": "clientName is required"
    },
    {
      "field": "phone",
      "message": "phone is required"
    },
    {
      "field": "serviceRequested",
      "message": "serviceRequested is required"
    },
    {
      "field": "preferredDate",
      "message": "preferredDate is required"
    },
    {
      "field": "preferredTime",
      "message": "preferredTime is required"
    }
  ]
}
```

### Duplicate booking

A duplicate request should not create a second booking accidentally.

Status:

```http
409 Conflict
```

Response:

```json
{
  "error": "duplicate_booking",
  "message": "A matching booking request already exists",
  "bookingId": "SALON-20260913-0001"
}
```

For Stage 1, a request can be considered a possible duplicate when the same client submits the same service, date, and time within a short period.

The duplicate-detection rule can be improved later by using an idempotency key.

### Internal server error

Returned when an unexpected error occurs while processing the request.

Status:

```http
500 Internal Server Error
```

Response:

```json
{
  "error": "internal_server_error",
  "message": "The booking could not be created"
}
```

Do not expose database credentials, stack traces, SQL queries, or other sensitive information in the response.

## 7. Get a booking by booking ID

Retrieves one booking.

### Request

```http
GET /api/v1/bookings/{bookingId}
```

Example:

```http
GET /api/v1/bookings/SALON-20260913-0001
```

### Successful response

Status:

```http
200 OK
```

Response:

```json
{
  "bookingId": "SALON-20260913-0001",
  "clientName": "Amina Patel",
  "phone": "+27821234567",
  "email": "amina@example.com",
  "serviceRequested": "Gel overlay with nail art",
  "preferredDate": "2026-10-03",
  "preferredTime": "14:00",
  "notes": "Chrome finish with floral detail",
  "source": "website",
  "bookingStatus": "new_request",
  "approvalStatus": "not_started",
  "paymentStatus": "not_started",
  "createdAt": "2026-09-13T21:00:00Z",
  "updatedAt": "2026-09-13T21:00:00Z"
}
```

### Booking not found

Status:

```http
404 Not Found
```

Response:

```json
{
  "error": "booking_not_found",
  "message": "No booking was found for the supplied booking ID",
  "bookingId": "SALON-20260913-9999"
}
```

## 8. List bookings

Retrieves bookings for the dashboard or administrative tools.

### Request

```http
GET /api/v1/bookings
```

Optional query parameters:

```text
status
date
limit
offset
```

Example:

```http
GET /api/v1/bookings?status=new_request&limit=20&offset=0
```

### Query parameter definitions

| Parameter | Type | Required | Description |
|---|---|---:|---|
| `status` | string | No | Filter by booking status |
| `date` | string | No | Filter by preferred date using `YYYY-MM-DD` |
| `limit` | integer | No | Maximum number of records to return |
| `offset` | integer | No | Number of records to skip |

Default values:

```text
limit = 20
offset = 0
```

Maximum limit:

```text
100
```

### Successful response

Status:

```http
200 OK
```

Response:

```json
{
  "items": [
    {
      "bookingId": "SALON-20260913-0001",
      "clientName": "Amina Patel",
      "phone": "+27821234567",
      "email": "amina@example.com",
      "serviceRequested": "Gel overlay with nail art",
      "preferredDate": "2026-10-03",
      "preferredTime": "14:00",
      "bookingStatus": "new_request",
      "approvalStatus": "not_started",
      "paymentStatus": "not_started",
      "createdAt": "2026-09-13T21:00:00Z",
      "updatedAt": "2026-09-13T21:00:00Z"
    }
  ],
  "pagination": {
    "limit": 20,
    "offset": 0,
    "total": 1
  }
}
```

## 9. Update booking status

This endpoint will be used in later stages for owner approval, payment confirmation, cancellation, and completion.

For Stage 1, you may implement the route but keep the supported transitions limited.

### Request

```http
PATCH /api/v1/bookings/{bookingId}/status
```

Headers:

```http
Content-Type: application/json
```

Body:

```json
{
  "bookingStatus": "cancelled",
  "reason": "Client requested cancellation"
}
```

### Successful response

Status:

```http
200 OK
```

Response:

```json
{
  "bookingId": "SALON-20260913-0001",
  "previousStatus": "new_request",
  "bookingStatus": "cancelled",
  "message": "Booking status updated"
}
```

### Invalid status transition

Status:

```http
409 Conflict
```

Response:

```json
{
  "error": "invalid_status_transition",
  "message": "The booking cannot transition from confirmed to new_request",
  "bookingId": "SALON-20260913-0001",
  "currentStatus": "confirmed",
  "requestedStatus": "new_request"
}
```

## 10. Future approval endpoint

This endpoint will be implemented in a later stage.

### Request

```http
PATCH /api/v1/bookings/{bookingId}/approval
```

Body:

```json
{
  "approvalStatus": "approved",
  "quotedAmount": 450.00,
  "durationMinutes": 120,
  "approvedBy": "salon-owner"
}
```

Expected future response:

```json
{
  "bookingId": "SALON-20260913-0001",
  "approvalStatus": "approved",
  "quotedAmount": 450.00,
  "durationMinutes": 120,
  "message": "Booking request approved"
}
```

This endpoint must not be used by the AI without human approval.

## 11. Future payment endpoint

This endpoint will eventually receive payment-provider webhook events.

### Request

```http
POST /api/v1/payments/webhook
```

Example body:

```json
{
  "paymentId": "pay_123",
  "bookingId": "SALON-20260913-0001",
  "amount": 450.00,
  "currency": "ZAR",
  "status": "successful",
  "providerReference": "provider-ref-123",
  "occurredAt": "2026-10-01T12:00:00Z"
}
```

The payment webhook must verify:

1. The booking exists.
2. The payment amount matches the expected amount.
3. The payment event has not already been processed.
4. The booking is eligible for payment confirmation.
5. The payment provider event is authentic.

A successful payment must not automatically create a duplicate calendar event.

## 12. Idempotency

Future write endpoints should support an idempotency key.

Example request header:

```http
Idempotency-Key: booking-request-abc-123
```

If the same request is received more than once with the same key, the service should return the original result instead of creating a duplicate booking.

Example:

```http
POST /api/v1/bookings
Idempotency-Key: booking-request-abc-123
Content-Type: application/json
```

The service should store the idempotency key with the result.

Expected behaviour:

```text
First request:
    Creates booking SALON-20260913-0001

Repeated request:
    Returns SALON-20260913-0001
    Does not create another booking
```

## 13. Example curl commands

### Check service health

```bash
curl -i http://localhost:7070/health
```

### Create a booking

```bash
curl -i \
  -X POST http://localhost:7070/api/v1/bookings \
  -H "Content-Type: application/json" \
  -d '{
    "clientName": "Amina Patel",
    "phone": "+27821234567",
    "email": "amina@example.com",
    "serviceRequested": "Gel overlay with nail art",
    "preferredDate": "2026-10-03",
    "preferredTime": "14:00",
    "notes": "Chrome finish with floral detail",
    "source": "website"
  }'
```

### Get a booking

```bash
curl -i \
  http://localhost:7070/api/v1/bookings/SALON-20260913-0001
```

### List bookings

```bash
curl -i \
  "http://localhost:7070/api/v1/bookings?status=new_request&limit=20&offset=0"
```

## 14. n8n integration contract

The n8n workflow receives the website submission and sends a request to the Booking Service.

### n8n input

```json
{
  "clientName": "Amina Patel",
  "phone": "+27821234567",
  "email": "amina@example.com",
  "serviceRequested": "Gel overlay with nail art",
  "preferredDate": "2026-10-03",
  "preferredTime": "14:00",
  "notes": "Chrome finish with floral detail",
  "source": "website"
}
```

### n8n HTTP Request node

```text
Method: POST
URL: http://localhost:7070/api/v1/bookings
Authentication: None for local development
Send Body: JSON
```

When n8n and the Booking Service run in the same Docker Compose network, use:

```text
http://booking-service:7070/api/v1/bookings
```

When n8n runs locally outside Docker, use:

```text
http://localhost:7070/api/v1/bookings
```

### n8n success path

If the Booking Service returns `201 Created`:

```text
1. Send an email to the salon owner.
2. Send an acknowledgement to the client.
3. Log the booking ID.
```

### n8n failure path

If the Booking Service returns an error:

```text
1. Do not send a confirmation to the client.
2. Notify the salon owner.
3. Record the error.
4. Return a controlled failure response.
```

## 15. Error response format

All API errors should use this general structure:

```json
{
  "error": "machine_readable_error_code",
  "message": "Human-readable explanation",
  "fields": [],
  "timestamp": "2026-09-13T21:00:00Z"
}
```

The `fields` property is only required for validation errors.

Example:

```json
{
  "error": "validation_failed",
  "message": "The booking request contains invalid fields",
  "fields": [
    {
      "field": "phone",
      "message": "phone must not be empty"
    }
  ],
  "timestamp": "2026-09-13T21:00:00Z"
}
```

## 16. HTTP status codes

| Status code | Meaning |
|---:|---|
| `200` | Request succeeded |
| `201` | Resource created |
| `400` | Request was invalid |
| `404` | Resource was not found |
| `409` | Conflict or duplicate request |
| `422` | Request structure is valid but business rules reject it |
| `500` | Unexpected server error |
| `503` | Service or dependency temporarily unavailable |

## 17. Stage 1 implementation scope

Implement these endpoints first:

```text
GET  /health
POST /api/v1/bookings
GET  /api/v1/bookings/{bookingId}
GET  /api/v1/bookings
```

Implement these later:

```text
PATCH /api/v1/bookings/{bookingId}/status
PATCH /api/v1/bookings/{bookingId}/approval
POST  /api/v1/payments/webhook
POST  /api/v1/calendar/availability
```

## 18. Stage 1 definition of done

Stage 1 is complete when:

- `GET /health` returns `200 OK`.
- A valid booking can be created.
- An invalid booking is rejected with `400 Bad Request`.
- A unique `bookingId` is generated.
- The booking is stored in PostgreSQL.
- A booking can be retrieved by `bookingId`.
- Bookings can be listed.
- Duplicate requests are handled.
- Errors use the documented response format.
- n8n can call `POST /api/v1/bookings`.
- The API contract matches the implemented behaviour.
