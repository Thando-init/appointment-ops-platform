# Booking API contract

## Create a booking

```http
POST /api/v1/bookings
Content-Type: application/json
```

The client sends the selected service and start time. The API looks up the
service duration from PostgreSQL and derives the end time. Client-supplied
end-time or duration values are intentionally not accepted as authoritative.

### Request

```json
{
  "clientName": "Amina Patel",
  "phone": "+27821234567",
  "email": "amina@example.com",
  "serviceId": "gel-overlay-art",
  "preferredDate": "2026-10-03",
  "preferredTime": "14:00",
  "notes": "Chrome finish with floral detail",
  "source": "website"
}
```

### Successful response

```http
200 OK
```

```json
{
  "bookingReference": "SALON-AB12CD34",
  "serviceId": "gel-overlay-art",
  "date": "2026-10-03",
  "start": "14:00",
  "end": "16:00",
  "bookingStatus": "PENDING",
  "approvalStatus": "PENDING_REVIEW",
  "paymentStatus": "NOT_STARTED"
}
```

## Validation and conflict behaviour

| Situation | Response |
|---|---:|
| Missing required client or slot field | `400 Bad Request` |
| Unknown or inactive service | `400 Bad Request` |
| Slot overlaps an active booking | `409 Conflict` |
| Slot is available and valid | `200 OK` |

The service reserves the appointment plus the configured cleanup buffer when
checking conflicts. For example, a 14:00–16:00 service with a 15-minute buffer
blocks the studio until 16:15.

## Current implementation boundary

The endpoint now persists bookings through `BookingRepository`, but it does not
yet create payment links or send WhatsApp messages. Those are later n8n steps
triggered after the API returns the booking reference.
