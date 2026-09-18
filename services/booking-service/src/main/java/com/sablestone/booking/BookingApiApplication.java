package com.sablestone.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Starts the Sable & Stone booking and availability API.
 *
 * <p>{@link SpringBootApplication} combines three important Spring features:
 * component scanning, automatic configuration, and configuration of the
 * application context. Because this class is at the root of the
 * {@code com.sablestone.booking} package, Spring can discover the controller
 * and service classes below it automatically.</p>
 */

@SpringBootApplication
public class BookingApiApplication {

    public static void main(String[] args) {
        // Spring creates the embedded web server and wires application objects here.
        SpringApplication.run(BookingApiApplication.class, args);
    }
}

