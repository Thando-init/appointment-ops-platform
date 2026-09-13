# Availability API Contract

<!-- This contract defines the frontend/backend boundary for service-aware availability. -->

## Purpose

The booking page must display only slots that can fit the selected service duration. The backend remains the final authority because another customer may select a slot between availability lookup and booking submission.

## Service-aware availability request

```
GET /api/v1/availability/slots?serviceId=gel-overlay-art&date=2026-10-03
```

### Query parameters

| Parameter | Type | Required | Description |
| --- | --- | --- | --- |
| `serviceId` | string | Yes | Stable service identifier from the catalogue |
| `date` | string | Yes | Date in `YYYY-MM-DD` format |

## Successful availability response

```
200 OK
```

```json
{
  "serviceId": "gel-overlay-art",
  "date": "2026-10-03",
  "durationMinutes": 120,
  "bufferMinutes": 15,
  "slots": [
    {
      "start": "2026-10-03T08:00:00+02:00",
      "end": "2026-10-03T10:00:00+02:00",
      "label": "08:00"
    },
    {
      "start": "2026-10-03T10:30:00+02:00",
      "end": "2026-10-03T12:30:00+02:00",
      "label": "10:30"
    }
  ]
}
```

The backend should exclude:

- Existing confirmed bookings.

- Active temporary holds.

- Blocked periods such as breaks or leave.

- Slots that cannot fit the appointment plus its buffer before closing time.

## No available slots

A date with no suitable slots is still a successful lookup.

```
200 OK
```

```json
{
  "serviceId": "gel-overlay-art",
  "date": "2026-10-03",
  "durationMinutes": 120,
  "bufferMinutes": 15,
  "slots": []
}
```

## Availability service error

```
503 Service Unavailable
```

```json
{
  "error": "availability_unavailable",
  "message": "The studio schedule could not be checked"
}
```

## Create-booking payload

The frontend sends the selected service and slot to n8n or the Booking Service.

```json
{
  "clientName": "Amina Patel",
  "phone": "+27821234567",
  "email": "amina@example.com",
  "serviceId": "gel-overlay-art",
  "serviceRequested": "Gel overlay with nail art",
  "durationMinutes": 120,
  "quotedAmount": 450,
  "requiresApproval": true,
  "preferredDate": "2026-10-03",
  "preferredTime": "14:00",
  "selectedStart": "2026-10-03T14:00:00+02:00",
  "selectedEnd": "2026-10-03T16:00:00+02:00",
  "notes": "Chrome finish with floral detail",
  "source": "website"
}
```

## Final conflict check

The server must recalculate or recheck the selected slot before creating a booking.

If the slot was taken after the availability lookup:

```
409 Conflict
```

```json
{
  "error": "slot_no_longer_available",
  "message": "The selected appointment time is no longer available.",
  "requestedStart": "2026-10-03T14:00:00+02:00",
  "requestedEnd": "2026-10-03T16:00:00+02:00"
}
```

The frontend should refresh availability and ask the client to choose another displayed slot. It must not silently change the requested appointment time.

## Future temporary hold endpoint

A later version can reserve a selected slot while the client completes payment.

```
POST /api/v1/bookings/{bookingId}/hold
```

Request:

```json
{
  "selectedStart": "2026-10-03T14:00:00+02:00",
  "selectedEnd": "2026-10-03T16:00:00+02:00"
}
```

Response:

```json
{
  "bookingId": "SALON-20260913-0001",
  "status": "held",
  "expiresAt": "2026-09-13T20:10:00+02:00"
}
```

A hold must expire automatically if payment is not completed within the configured period.