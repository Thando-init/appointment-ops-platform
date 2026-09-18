# Frontend API integration

The static booking form now talks directly to the Spring Boot booking API while
running locally.

## Local URLs

| Component | URL |
|---|---|
| Static booking form | `http://localhost:8000` |
| Spring Boot API | `http://localhost:8080` |
| Availability endpoint | `GET /api/v1/availability/slots` |
| Booking endpoint | `POST /api/v1/bookings` |

## Browser flow

```text
Choose service
    ↓
Choose date
    ↓
Frontend requests available slots
    ↓
Choose returned time
    ↓
Submit contact details
    ↓
Frontend posts the selected service/date/time to Spring Boot
    ↓
Backend recalculates duration and checks conflicts
    ↓
Success reference or error is shown in the form
```

The browser sends only the fields accepted by the booking API. It does not send
client-controlled duration, price, approval, or end-time values as authoritative
booking data. The backend looks those values up from PostgreSQL.

## Run locally

Start PostgreSQL:

```powershell
docker compose up -d postgres
```

Start the API from `services/booking-service`:

```powershell
mvn spring-boot:run
```

Serve the static form from `apps/booking-form` with any local HTTP server. For
example, if Python is installed:

```powershell
py -m http.server 8000
```

Then open:

```text
http://localhost:8000
```

## Error behaviour

| API result | Form behaviour |
|---|---|
| `200 OK` | Shows the server-generated booking reference |
| `400 Bad Request` | Asks the client to check the date, time, or details |
| `409 Conflict` | Explains that the slot was just taken and asks for another time |
| Other error | Shows a generic submission error without exposing server internals |

## n8n boundary

The `N8N_WEBHOOK_URL` constant remains empty for now. When n8n automation is
ready, it can be configured to receive the same validated booking payload or,
preferably, consume the booking reference after the API has persisted the row.
