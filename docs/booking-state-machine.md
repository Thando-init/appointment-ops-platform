# Booking state model

The booking system tracks three related but independent workflows. Keeping them
separate avoids forcing one status field to represent several processes that do
not always change at the same time.

## Booking status

`booking_status` describes whether the appointment request exists and whether
its time is still being held by the business.

```text
PENDING
  ├── CONFIRMED
  ├── CANCELLED
  └── EXPIRED

CONFIRMED
  ├── COMPLETED
  └── CANCELLED
```

| State | Meaning | Reserves the slot? |
|---|---|---:|
| `PENDING` | Request received and awaiting business review or completion | Yes, according to hold policy |
| `CONFIRMED` | Appointment accepted and placed on the calendar | Yes |
| `CANCELLED` | Appointment cancelled and no longer active | No |
| `EXPIRED` | Request or temporary hold passed its deadline | No |
| `COMPLETED` | Appointment took place | No future slot reservation |

## Approval status

`approval_status` describes the business review of the requested service. This
is separate because some services, such as custom nail art or extensions, may
require review while a basic manicure may be automatically accepted.

```text
NOT_STARTED → PENDING_REVIEW → APPROVED
                              └→ REJECTED
```

## Payment status

`payment_status` describes the deposit or payment workflow. Payment may happen
after approval, and a payment failure should not be represented as a cancelled
appointment automatically without an explicit business rule.

```text
NOT_STARTED → PAYMENT_REQUIRED → PENDING
                                  ├→ PAID
                                  └→ FAILED
```

A later refund flow can add:

```text
PAID → REFUNDED
```

## Example combinations

| Booking | Approval | Payment | Interpretation |
|---|---|---|---|
| `PENDING` | `PENDING_REVIEW` | `NOT_STARTED` | Request received and awaiting review |
| `PENDING` | `APPROVED` | `PAYMENT_REQUIRED` | Service accepted; deposit still needed |
| `CONFIRMED` | `APPROVED` | `PAID` | Valid appointment on the calendar |
| `CANCELLED` | `APPROVED` | `PAID` | Appointment cancelled; refund policy may apply |
| `EXPIRED` | `NOT_STARTED` | `FAILED` | Request is no longer active |

## Availability rule

The availability calculation should treat a booking as blocking time when its
`booking_status` is an active state, normally:

```text
PENDING
CONFIRMED
```

It should ignore:

```text
CANCELLED
EXPIRED
COMPLETED
```

The exact treatment of `PENDING` will be controlled by the future temporary-hold
policy. If pending requests do not reserve time, the booking service must use a
separate `booking_holds` record with an `expires_at` value.
