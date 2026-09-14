package com.sablestone.booking.api;

import com.sablestone.booking.domain.BookingModels;
import com.sablestone.booking.service.AvailabilityService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

/** HTTP boundary consumed by the static booking form and, later, n8n. */
@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = {"http://localhost:8000", "http://127.0.0.1:8000"})
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    public AvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping("/health")
    public HealthResponse health() {
        return new HealthResponse("ok", "booking-api");
    }

    @GetMapping("/availability/slots")
    public BookingModels.AvailabilityResponse slots(
            @RequestParam String serviceId,
            @RequestParam LocalDate date
    ) {
        try {
            return availabilityService.getSlots(serviceId, date);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    public record HealthResponse(String status, String service) {}
}


