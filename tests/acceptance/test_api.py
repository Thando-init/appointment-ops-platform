"""Small API acceptance tests for the local appointment service.

These tests use only Python's standard library so a beginner can run them
without installing a separate test framework. Start PostgreSQL and Spring Boot
before running this file.
"""

import json
import os
import sys
import urllib.error
import urllib.request


BASE_URL = os.getenv("API_BASE_URL", "http://localhost:8080")


def request_json(method, path, payload=None):
    """Send JSON to the API and return the response status and decoded body."""
    body = None
    headers = {"Accept": "application/json"}
    if payload is not None:
        body = json.dumps(payload).encode("utf-8")
        headers["Content-Type"] = "application/json"

    request = urllib.request.Request(
        f"{BASE_URL}{path}", data=body, headers=headers, method=method
    )
    try:
        with urllib.request.urlopen(request, timeout=5) as response:
            return response.status, json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as error:
        response_body = error.read().decode("utf-8")
        try:
            response_body = json.loads(response_body)
        except json.JSONDecodeError:
            response_body = {"raw": response_body}
        return error.code, response_body


def check(condition, message):
    """Raise a useful error instead of silently accepting a broken response."""
    if not condition:
        raise AssertionError(message)


def main():
    """Run the acceptance scenarios in a predictable order."""
    status, health = request_json("GET", "/api/v1/health")
    check(status == 200, f"Health returned HTTP {status}: {health}")
    check(health.get("status") == "ok", f"Unexpected health response: {health}")

    status, availability = request_json(
        "GET",
        "/api/v1/availability/slots?serviceId=gel-overlay-art&date=2026-10-03",
    )
    check(status == 200, f"Availability returned HTTP {status}: {availability}")
    check(availability.get("serviceId") == "gel-overlay-art", "Wrong service returned")
    check("slots" in availability, "Availability response has no slots field")

    # Use the first returned slot, so this test remains valid when the schedule changes.
    check(availability["slots"], "The seeded test date has no available slots")
    chosen_slot = availability["slots"][0]["label"]
    booking = {
        "clientName": "Acceptance Test Client",
        "phone": "+27820000000",
        "email": "acceptance@example.com",
        "serviceId": "gel-overlay-art",
        "preferredDate": "2026-10-03",
        "preferredTime": chosen_slot,
        "notes": "Created by the local acceptance test",
        "source": "acceptance-test",
    }

    status, created = request_json("POST", "/api/v1/bookings", booking)
    check(status == 200, f"Booking creation returned HTTP {status}: {created}")
    check(created.get("bookingReference"), "Booking response has no reference")
    check(created.get("bookingStatus") == "PENDING", "New booking is not pending")

    # The same slot should now be protected by the overlap check.
    status, conflict = request_json("POST", "/api/v1/bookings", booking)
    check(status == 409, f"Duplicate booking returned HTTP {status}: {conflict}")

    print("ACCEPTANCE_TESTS_PASSED")


if __name__ == "__main__":
    try:
        main()
    except (AssertionError, urllib.error.URLError) as error:
        print(f"ACCEPTANCE_TESTS_FAILED: {error}", file=sys.stderr)
        sys.exit(1)
