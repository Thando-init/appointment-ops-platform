package com.sablestone.booking.service;

import com.sablestone.booking.domain.BookingModels;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/** Calculates bookable start times from schedule rules and in-memory seed data. */
@Service
public class AvailabilityService {

    public static final int BUFFER_MINUTES = 15;
    private static final int SLOT_INTERVAL_MINUTES = 30;
    private static final LocalTime OPENING_TIME = LocalTime.of(8, 0);
    private static final LocalTime CLOSING_TIME = LocalTime.of(17, 0);
    private static final DateTimeFormatter LABEL_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    // The catalogue will move to a database after the calculation is proven.
    private final Map<String, BookingModels.ServiceDefinition> services = Map.of(
            "basic-manicure", new BookingModels.ServiceDefinition("basic-manicure", "Basic manicure", 45, 250, false),
            "gel-overlay", new BookingModels.ServiceDefinition("gel-overlay", "Gel overlay", 60, 300, false),
            "gel-overlay-art", new BookingModels.ServiceDefinition("gel-overlay-art", "Gel overlay with nail art", 120, 450, true),
            "acrylic-extensions", new BookingModels.ServiceDefinition("acrylic-extensions", "Acrylic extensions", 150, 600, true),
            "not-sure", new BookingModels.ServiceDefinition("not-sure", "Not sure yet", 60, 0, true)
    );

    // This seed data intentionally mirrors the frontend demo date.
    private final Map<LocalDate, List<BookingModels.TimePeriod>> blockedPeriods = Map.of(
            LocalDate.of(2026, 10, 3), List.of(
                    new BookingModels.TimePeriod(LocalTime.of(9, 30), LocalTime.of(10, 30), "Existing booking"),
                    new BookingModels.TimePeriod(LocalTime.of(12, 0), LocalTime.of(13, 0), "Studio break")
            )
    );

    private final List<BookingModels.ExistingBooking> existingBookings = List.of(
            // Additional records can be moved into the bookings table later.
    );

    public BookingModels.AvailabilityResponse getSlots(String serviceId, LocalDate date) {
        BookingModels.ServiceDefinition service = services.get(serviceId);
        if (service == null) {
            throw new IllegalArgumentException("Unknown service: " + serviceId);
        }

        if (!isOpenOn(date.getDayOfWeek())) {
            return response(service, date, List.of());
        }

        List<BookingModels.AvailabilitySlot> slots = calculateSlots(service, date);
        return response(service, date, slots);
    }

    private List<BookingModels.AvailabilitySlot> calculateSlots(
            BookingModels.ServiceDefinition service,
            LocalDate date
    ) {
        List<BookingModels.TimePeriod> blocked = blockedPeriods.getOrDefault(date, List.of());
        int requiredMinutes = service.durationMinutes() + BUFFER_MINUTES;
        int openingMinutes = OPENING_TIME.toSecondOfDay() / 60;
        int closingMinutes = CLOSING_TIME.toSecondOfDay() / 60;
        java.util.ArrayList<BookingModels.AvailabilitySlot> slots = new java.util.ArrayList<>();

        // A start is valid only when the service and its buffer fit before closing.
        for (int start = openingMinutes; start + requiredMinutes <= closingMinutes; start += SLOT_INTERVAL_MINUTES) {
            LocalTime startTime = LocalTime.of(start / 60, start % 60);
            LocalTime appointmentEnd = startTime.plusMinutes(service.durationMinutes());
            LocalTime occupiedEnd = appointmentEnd.plusMinutes(BUFFER_MINUTES);

            if (!overlapsAny(startTime, occupiedEnd, blocked) && !overlapsExisting(date, startTime, occupiedEnd)) {
                LocalDateTime startDateTime = LocalDateTime.of(date, startTime);
                slots.add(new BookingModels.AvailabilitySlot(
                        startDateTime,
                        LocalDateTime.of(date, appointmentEnd),
                        LABEL_FORMAT.format(startDateTime)
                ));
            }
        }

        return slots;
    }

    private boolean overlapsAny(LocalTime start, LocalTime end, List<BookingModels.TimePeriod> periods) {
        return periods.stream().anyMatch(period -> start.isBefore(period.end()) && end.isAfter(period.start()));
    }

    private boolean overlapsExisting(LocalDate date, LocalTime start, LocalTime end) {
        return existingBookings.stream()
                .filter(booking -> booking.date().equals(date))
                .filter(booking -> !booking.status().equalsIgnoreCase("cancelled"))
                .anyMatch(booking -> start.isBefore(booking.end()) && end.isAfter(booking.start()));
    }

    private boolean isOpenOn(DayOfWeek day) {
        // The studio is closed Sunday and Monday, matching the booking-page copy.
        return day != DayOfWeek.SUNDAY && day != DayOfWeek.MONDAY;
    }

    private BookingModels.AvailabilityResponse response(
            BookingModels.ServiceDefinition service,
            LocalDate date,
            List<BookingModels.AvailabilitySlot> slots
    ) {
        return new BookingModels.AvailabilityResponse(service.id(), date, service.durationMinutes(), BUFFER_MINUTES, slots);
    }
}

