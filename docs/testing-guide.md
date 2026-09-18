# Testing guide

Testing is divided into layers. Each layer answers a different question.

## 1. Unit tests

Run:

```powershell
Set-Location services/booking-service
mvn clean test
```

Unit tests use mocked repositories. They verify business decisions quickly:

- Service duration determines the end time.
- The cleanup buffer is included in overlap checks.
- Closed days are rejected.
- Requests outside opening hours are rejected.
- Blocked periods are rejected.
- Unknown services are rejected.
- Existing bookings produce a conflict.
- Approval and payment statuses are set correctly.

A passing unit test suite does not prove the database connection works; it proves the Java business logic works in isolation.

## 2. Database/API smoke test

Start PostgreSQL and Spring Boot, then run:

```powershell
.\scripts\test-booking-api.ps1
```

Or test each endpoint manually:

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/health
Invoke-RestMethod "http://localhost:8080/api/v1/availability/slots?serviceId=gel-overlay-art&date=2026-10-03"
```

The booking request should return a booking reference. Repeating the same request should return `409 Conflict`.

## 3. Browser test

Start the static server:

```powershell
Set-Location apps/booking-form
py -m http.server 8000
```

Open `http://localhost:8000`, then:

1. Select a service.
2. Select an open date.
3. Confirm the time dropdown contains slots.
4. Complete the contact fields.
5. Submit the form.
6. Confirm a booking reference is displayed.
7. Try the same slot again and confirm a conflict message appears.

Use browser developer tools if something fails:

- Network tab: inspect request URL, method, status, and response.
- Console tab: inspect JavaScript or CORS errors.

## 4. n8n test

Import `workflows/n8n/booking-intake.json` into n8n and use `workflows/n8n/test-payload.json` as the request body.

Verify:

- Valid `booking.created` events are acknowledged.
- Missing `bookingReference` is rejected.
- An incorrect `eventType` is rejected.
- The workflow does not contain provider credentials.

## 5. Failure tests to perform deliberately

| Scenario | Expected result |
|---|---|
| Stop Spring Boot | Frontend displays an availability error |
| Choose a closed day | No slots are returned |
| Submit outside business hours directly | HTTP `400` |
| Submit an existing slot twice | Second request is HTTP `409` |
| Send unknown service ID | HTTP `400` |
| Send malformed email | HTTP `400` validation response |
| Send invalid n8n event | HTTP `400` from webhook workflow |

## Reading failures

- `404`: the route is missing or the wrong URL is being used.
- `400`: the request is invalid or violates schedule rules.
- `409`: the slot became unavailable.
- `500`: inspect the Spring Boot terminal; this usually indicates a server or database issue.
- CORS error: check the frontend origin and the controller `@CrossOrigin` values.
