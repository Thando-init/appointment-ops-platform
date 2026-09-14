# appointment-ops-platform
A reusable appointment workflow demonstrated through a salon or nail-tech business.

# File Structure

```appointment-ops-platform/
appointment-ops-platform/
├── README.md
├── .gitignore
├── .env.example
├── docker-compose.yml
│
├── apps/
│   └── booking-form/
│       ├── index.html
│       ├── styles.css
│       ├── app.js
│       └── tilt-effects.js
│
├── services/
│   └── booking-service/
│       ├── pom.xml
│       │
│       └── src/
│           ├── main/
│           │   ├── java/
│           │   │   └── com/
│           │   │       └── sablestone/
│           │   │           └── booking/
│           │   │               ├── BookingApiApplication.java
│           │   │               │
│           │   │               ├── config/
│           │   │               │   ├── CorsConfig.java
│           │   │               │   └── JacksonConfig.java
│           │   │               │
│           │   │               ├── api/
│           │   │               │   ├── HealthController.java
│           │   │               │   ├── AvailabilityController.java
│           │   │               │   └── BookingController.java
│           │   │               │
│           │   │               ├── domain/
│           │   │               │   ├── ServiceDefinition.java
│           │   │               │   ├── Booking.java
│           │   │               │   ├── BookingHold.java
│           │   │               │   ├── BlockedPeriod.java
│           │   │               │   ├── BusinessHours.java
│           │   │               │   ├── AvailabilitySlot.java
│           │   │               │   └── BookingStatus.java
│           │   │               │
│           │   │               ├── dto/
│           │   │               │   ├── CreateBookingRequest.java
│           │   │               │   ├── BookingResponse.java
│           │   │               │   ├── AvailabilityResponse.java
│           │   │               │   ├── AvailabilitySlotResponse.java
│           │   │               │   └── ErrorResponse.java
│           │   │               │
│           │   │               ├── service/
│           │   │               │   ├── AvailabilityService.java
│           │   │               │   ├── BookingService.java
│           │   │               │   └── BookingHoldService.java
│           │   │               │
│           │   │               ├── repository/
│           │   │               │   ├── ServiceRepository.java
│           │   │               │   ├── BookingRepository.java
│           │   │               │   ├── BookingHoldRepository.java
│           │   │               │   ├── BlockedPeriodRepository.java
│           │   │               │   └── BusinessHoursRepository.java
│           │   │               │
│           │   │               ├── validation/
│           │   │               │   ├── BookingValidator.java
│           │   │               │   ├── AvailabilityValidator.java
│           │   │               │   └── PhoneNumberValidator.java
│           │   │               │
│           │   │               └── exception/
│           │   │                   ├── BookingConflictException.java
│           │   │                   ├── ServiceNotFoundException.java
│           │   │                   ├── SlotUnavailableException.java
│           │   │                   └── GlobalExceptionHandler.java
│           │   │
│           │   └── resources/
│           │       ├── application.properties
│           │       ├── application-local.properties
│           │       ├── application-test.properties
│           │       └── db/
│           │           └── migration/
│           │               ├── V1__create_services.sql
│           │               ├── V2__create_business_hours.sql
│           │               ├── V3__create_blocked_periods.sql
│           │               ├── V4__create_bookings.sql
│           │               └── V5__create_booking_holds.sql
│           │
│           └── test/
│               └── java/
│                   └── com/
│                       └── sablestone/
│                           └── booking/
│                               ├── api/
│                               │   ├── HealthControllerTest.java
│                               │   ├── AvailabilityControllerTest.java
│                               │   └── BookingControllerTest.java
│                               │
│                               ├── service/
│                               │   ├── AvailabilityServiceTest.java
│                               │   ├── BookingServiceTest.java
│                               │   └── BookingHoldServiceTest.java
│                               │
│                               └── validation/
│                                   └── BookingValidatorTest.java
│
├── workflows/
│   └── n8n/
│       ├── booking-intake.json
│       ├── booking-confirmation.json
│       ├── payment-status.json
│       ├── README.md
│       └── test-payload.json
│
├── infra/
│   ├── postgres/
│   │   ├── init.sql
│   │   └── README.md
│   │
│   ├── activemq/
│   │   ├── README.md
│   │   └── topics.md
│   │
│   └── docker/
│       └── booking-service.Dockerfile
│
├── docs/
│   ├── architecture.md
│   ├── api-contract.md
│   ├── availability-api-contract.md
│   ├── booking-state-machine.md
│   ├── failure-handling.md
│   ├── local-development.md
│   ├── n8n-integration.md
│   │
│   └── decisions/
│       ├── ADR-001-monorepo-and-service-boundary.md
│       ├── ADR-002-availability-calculation.md
│       └── ADR-003-booking-conflict-protection.md
│
├── scripts/
│   ├── start-local.sh
│   ├── stop-local.sh
│   ├── reset-local-db.sh
│   ├── send-test-request.sh
│   └── check-health.sh
│
└── demo/
    ├── screenshots/
    │   ├── booking-form.png
    │   ├── availability-response.png
    │   └── n8n-workflow.png
    │
    └── booking-intake-demo.mp4

```