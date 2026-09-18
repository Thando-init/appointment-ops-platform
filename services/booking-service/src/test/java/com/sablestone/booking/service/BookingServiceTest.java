package com.sablestone.booking.service;

import com.sablestone.booking.domain.BookingModels;
import com.sablestone.booking.domain.BookingRequests;
import com.sablestone.booking.repository.BookingRepository;
import com.sablestone.booking.repository.ScheduleRepository;
import com.sablestone.booking.repository.ServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for booking orchestration.
 *
 * <p>These tests mock repositories so they verify the service decisions without
 * requiring Docker or PostgreSQL. Database SQL is a separate repository concern;
 * the service tests focus on trusted duration, status selection, and conflicts.</p>
 */
class BookingServiceTest {

    private BookingRepository bookingRepository;
    private ServiceRepository serviceRepository;
    private ScheduleRepository scheduleRepository;
    private BookingService bookingService;


    @BeforeEach
    void setUp() {
        // Each test receives fresh mocks, preventing state from leaking between scenarios.
        bookingRepository = mock(BookingRepository.class);
        serviceRepository = mock(ServiceRepository.class);
        scheduleRepository = mock(ScheduleRepository.class);
        when(scheduleRepository.findBusinessHours(any())).thenReturn(
                Optional.of(new BookingModels.BusinessHours(LocalTime.of(8, 0), LocalTime.of(17, 0))));
        when(scheduleRepository.findBlockedPeriods(any())).thenReturn(List.of());
        bookingService = new BookingService(bookingRepository, serviceRepository, scheduleRepository);
    }

    @Test
    void createsPendingBookingAndRequestsApprovalWhenServiceRequiresIt() {
        BookingModels.ServiceDefinition service = new BookingModels.ServiceDefinition(
                "gel-overlay-art", "Gel overlay with nail art", 120, 450, true);
        BookingRequests.CreateBookingRequest request = requestFor("gel-overlay-art", LocalTime.of(14, 0));

        when(serviceRepository.findActiveById("gel-overlay-art")).thenReturn(Optional.of(service));
        when(bookingRepository.hasActiveOverlap(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        when(bookingRepository.insert(eq(request), any(LocalDateTime.class), any(LocalDateTime.class), eq(true)))
                .thenReturn("SALON-AB12CD34");

        BookingRequests.BookingResponse response = bookingService.create(request);

        assertEquals("SALON-AB12CD34", response.bookingReference());
        assertEquals(LocalTime.of(16, 0), response.end());
        assertEquals("PENDING", response.bookingStatus());
        assertEquals("PENDING_REVIEW", response.approvalStatus());
        assertEquals("NOT_STARTED", response.paymentStatus());

        // The conflict check includes the 15-minute cleanup buffer: 16:15 is occupied.
        verify(bookingRepository).hasActiveOverlap(
                LocalDateTime.of(2026, 10, 3, 14, 0),
                LocalDateTime.of(2026, 10, 3, 16, 15));
    }

    @Test
    void automaticallyApprovesServiceThatDoesNotRequireReview() {
        BookingModels.ServiceDefinition service = new BookingModels.ServiceDefinition(
                "gel-overlay", "Gel overlay", 60, 300, false);
        BookingRequests.CreateBookingRequest request = requestFor("gel-overlay", LocalTime.of(10, 0));

        when(serviceRepository.findActiveById("gel-overlay")).thenReturn(Optional.of(service));
        when(bookingRepository.hasActiveOverlap(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(false);
        when(bookingRepository.insert(eq(request), any(LocalDateTime.class), any(LocalDateTime.class), eq(false)))
                .thenReturn("SALON-CD34EF56");

        BookingRequests.BookingResponse response = bookingService.create(request);

        assertEquals("APPROVED", response.approvalStatus());
        assertEquals(LocalTime.of(11, 0), response.end());
    }

    @Test
    void rejectsUnknownServiceBeforeCheckingTheRequestedSlot() {
        BookingRequests.CreateBookingRequest request = requestFor("missing-service", LocalTime.of(10, 0));
        when(serviceRepository.findActiveById("missing-service")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> bookingService.create(request));
        verify(bookingRepository, never()).hasActiveOverlap(any(), any());
    }

    @Test
    void rejectsSlotThatBecameUnavailableAfterAvailabilityLookup() {
        BookingModels.ServiceDefinition service = new BookingModels.ServiceDefinition(
                "gel-overlay", "Gel overlay", 60, 300, false);
        BookingRequests.CreateBookingRequest request = requestFor("gel-overlay", LocalTime.of(10, 0));

        when(serviceRepository.findActiveById("gel-overlay")).thenReturn(Optional.of(service));
        when(bookingRepository.hasActiveOverlap(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(true);

        assertThrows(BookingService.SlotUnavailableException.class, () -> bookingService.create(request));
        verify(bookingRepository, never()).insert(any(), any(), any(), any(Boolean.class));
    }

    @Test
    void rejectsAppointmentWhoseBufferRunsPastClosingTime() {
        BookingModels.ServiceDefinition service = new BookingModels.ServiceDefinition(
                "gel-overlay", "Gel overlay", 60, 300, false);
        BookingRequests.CreateBookingRequest request = requestFor("gel-overlay", LocalTime.of(16, 0));
        when(serviceRepository.findActiveById("gel-overlay")).thenReturn(Optional.of(service));

        assertThrows(BookingService.ScheduleUnavailableException.class,
                () -> bookingService.create(request));
        verify(bookingRepository, never()).hasActiveOverlap(any(), any());
    }

    @Test
    void rejectsClosedDayWithoutCheckingBookingOverlap() {
        BookingRequests.CreateBookingRequest request = requestFor("gel-overlay", LocalTime.of(10, 0));
        BookingModels.ServiceDefinition service = new BookingModels.ServiceDefinition(
                "gel-overlay", "Gel overlay", 60, 300, false);
        when(serviceRepository.findActiveById("gel-overlay")).thenReturn(Optional.of(service));
        when(scheduleRepository.findBusinessHours(any())).thenReturn(Optional.empty());

        assertThrows(BookingService.ScheduleUnavailableException.class,
                () -> bookingService.create(request));
        verify(bookingRepository, never()).hasActiveOverlap(any(), any());
    }

    @Test
    void rejectsBlockedPeriodOverlap() {
        BookingModels.ServiceDefinition service = new BookingModels.ServiceDefinition(
                "gel-overlay", "Gel overlay", 60, 300, false);
        BookingRequests.CreateBookingRequest request = requestFor("gel-overlay", LocalTime.of(12, 0));
        when(serviceRepository.findActiveById("gel-overlay")).thenReturn(Optional.of(service));
        when(scheduleRepository.findBlockedPeriods(any())).thenReturn(List.of(
                new BookingModels.TimePeriod(LocalTime.of(12, 30), LocalTime.of(13, 0), "Studio break")));

        assertThrows(BookingService.ScheduleUnavailableException.class,
                () -> bookingService.create(request));
        verify(bookingRepository, never()).hasActiveOverlap(any(), any());
    }

    /** Creates the same shape of request that the website will submit. */
    private BookingRequests.CreateBookingRequest requestFor(String serviceId, LocalTime time) {
        return new BookingRequests.CreateBookingRequest(
                "Amina Patel", "+27821234567", "amina@example.com", serviceId,
                LocalDate.of(2026, 10, 3), time, "Chrome finish", "website");
    }
}

