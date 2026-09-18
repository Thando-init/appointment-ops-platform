package com.sablestone.booking.service;

import com.sablestone.booking.domain.BookingModels;
import com.sablestone.booking.repository.ScheduleRepository;
import com.sablestone.booking.repository.ServiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Calculates bookable start times from schedule rules and seed data.
 *
 * <p>The algorithm deliberately treats the backend as the authority. The
 * frontend may display a slot, but the booking endpoint must repeat the same
 * conflict check immediately before saving a booking.</p>
 *
 * <p>For every candidate start time, the service calculates:</p>
 * <pre>
 * appointmentEnd = start + serviceDuration
 * occupiedEnd    = appointmentEnd + cleanupBuffer
 * </pre>
 * A candidate is returned only when {@code occupiedEnd} is within business
 * hours and the occupied interval does not overlap a blocked period or an
 * existing non-cancelled booking.
 *
 * <p>The maps and list below are temporary seed data. The database phase will
 * replace them with repositories without changing this public calculation
 * contract.</p>
 */

@Service
public class AvailabilityService {

    /** Cleanup/reset time reserved after every appointment. */
    public static final int BUFFER_MINUTES = 15;
    /** Candidate start times are offered at half-hour increments. */
    private static final int SLOT_INTERVAL_MINUTES = 30;
    private static final LocalTime OPENING_TIME = LocalTime.of(8, 0);
    private static final LocalTime CLOSING_TIME = LocalTime.of(17, 0);
    private static final DateTimeFormatter LABEL_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    // Fallback catalogue used only by the no-argument unit-test constructor.
    private final Map<String, BookingModels.ServiceDefinition> services = Map.of(
            "basic-manicure", new BookingModels.ServiceDefinition("basic-manicure", "Basic manicure", 45, 250, false),
            "gel-overlay", new BookingModels.ServiceDefinition("gel-overlay", "Gel overlay", 60, 300, false),
            "gel-overlay-art", new BookingModels.ServiceDefinition("gel-overlay-art", "Gel overlay with nail art", 120, 450, true),
            "acrylic-extensions", new BookingModels.ServiceDefinition("acrylic-extensions", "Acrylic extensions", 150, 600, true),
            "not-sure", new BookingModels.ServiceDefinition("not-sure", "Not sure yet", 60, 0, true)
    );

    // Fallback schedule used only by isolated unit tests; production reads PostgreSQL.
    private final Map<LocalDate, List<BookingModels.TimePeriod>> blockedPeriods = Map.of(
            LocalDate.of(2026, 10, 3), List.of(
                    new BookingModels.TimePeriod(LocalTime.of(9, 30), LocalTime.of(10, 30), "Existing booking"),
                    new BookingModels.TimePeriod(LocalTime.of(12, 0), LocalTime.of(13, 0), "Studio break")
            )
    );

    // Fallback bookings used only by isolated unit tests; production reads PostgreSQL.
    private final List<BookingModels.ExistingBooking> existingBookings = List.of(
    );

    private final ServiceRepository serviceRepository;
    private final ScheduleRepository scheduleRepository;

    /** Spring-managed constructor used by the running application. */
    @Autowired
    public AvailabilityService(ServiceRepository serviceRepository, ScheduleRepository scheduleRepository) {
        this.serviceRepository = serviceRepository;
        this.scheduleRepository = scheduleRepository;
    }

    /** Test-friendly constructor that keeps calculation tests independent of PostgreSQL. */
    public AvailabilityService() {
        this.serviceRepository = null;
        this.scheduleRepository = null;
    }

    /**
     * Gets all valid client-facing slots for a service/date pair.
     *
     * @param serviceId stable identifier from the service catalogue
     * @param date local studio calendar date
     * @return response containing service duration, buffer, and available slots
     * @throws IllegalArgumentException when {@code serviceId} is not in the catalogue
     */
    public BookingModels.AvailabilityResponse getSlots(String serviceId, LocalDate date) {
        BookingModels.ServiceDefinition service = findService(serviceId);
        if (service == null) {
            throw new IllegalArgumentException("Unknown service: " + serviceId);
        }

        // A closed day is a valid lookup, so return 200 with an empty slot list.
        if (!isOpenOn(date.getDayOfWeek())) {
            return response(service, date, List.of());
        }

        List<BookingModels.AvailabilitySlot> slots = calculateSlots(service, date);
        return response(service, date, slots);
    }

    /** Generates candidates and removes any that violate scheduling constraints. */
    private List<BookingModels.AvailabilitySlot> calculateSlots(
            BookingModels.ServiceDefinition service,
            LocalDate date
    ) {
        List<BookingModels.TimePeriod> blocked = findBlockedPeriods(date);
        List<BookingModels.ExistingBooking> bookings = findExistingBookings(date);
        // The buffer affects availability but is not included in the client-facing end time.
        int requiredMinutes = service.durationMinutes() + BUFFER_MINUTES;
        int openingMinutes = OPENING_TIME.toSecondOfDay() / 60;
        int closingMinutes = CLOSING_TIME.toSecondOfDay() / 60;
        java.util.ArrayList<BookingModels.AvailabilitySlot> slots = new java.util.ArrayList<>();

        // Generate predictable candidates from opening time in 30-minute increments.
        for (int start = openingMinutes; start + requiredMinutes <= closingMinutes; start += SLOT_INTERVAL_MINUTES) {
            LocalTime startTime = LocalTime.of(start / 60, start % 60);
            LocalTime appointmentEnd = startTime.plusMinutes(service.durationMinutes());
            LocalTime occupiedEnd = appointmentEnd.plusMinutes(BUFFER_MINUTES);

            // Compare the whole occupied interval, not only the visible appointment.
            if (!overlapsAny(startTime, occupiedEnd, blocked) && !overlapsExisting(bookings, startTime, occupiedEnd)) {
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

    /** Uses half-open interval logic to detect partial or complete overlaps. */
    private boolean overlapsAny(LocalTime start, LocalTime end, List<BookingModels.TimePeriod> periods) {
        return periods.stream().anyMatch(period -> start.isBefore(period.end()) && end.isAfter(period.start()));
    }

    /** Ignores cancelled bookings because they no longer reserve studio time. */
    private boolean overlapsExisting(List<BookingModels.ExistingBooking> bookings, LocalTime start, LocalTime end) {
        return bookings.stream()
                .filter(booking -> !booking.status().equalsIgnoreCase("cancelled"))
                .anyMatch(booking -> start.isBefore(booking.end()) && end.isAfter(booking.start()));
    }

    /** Uses database records in production and seed records in isolated unit tests. */
    private BookingModels.ServiceDefinition findService(String serviceId) {
        if (serviceRepository != null) {
            return serviceRepository.findActiveById(serviceId).orElse(null);
        }
        return services.get(serviceId);
    }

    /** Loads date-specific blocked periods from PostgreSQL when the app is running. */
    private List<BookingModels.TimePeriod> findBlockedPeriods(LocalDate date) {
        if (scheduleRepository != null) {
            return scheduleRepository.findBlockedPeriods(date);
        }
        return blockedPeriods.getOrDefault(date, List.of());
    }

    /** Loads active bookings from PostgreSQL when the app is running. */
    private List<BookingModels.ExistingBooking> findExistingBookings(LocalDate date) {
        if (scheduleRepository != null) {
            return scheduleRepository.findActiveBookings(date);
        }
        return existingBookings.stream().filter(booking -> booking.date().equals(date)).toList();
    }

    /** Returns whether the studio accepts appointments on a weekday. */
    private boolean isOpenOn(DayOfWeek day) {
        // The studio is closed Sunday and Monday, matching the booking-page copy.
        return day != DayOfWeek.SUNDAY && day != DayOfWeek.MONDAY;
    }

    /** Keeps response construction in one place so the API shape stays consistent. */
    private BookingModels.AvailabilityResponse response(
            BookingModels.ServiceDefinition service,
            LocalDate date,
            List<BookingModels.AvailabilitySlot> slots
    ) {
        return new BookingModels.AvailabilityResponse(service.id(), date, service.durationMinutes(), BUFFER_MINUTES, slots);
    }
}

