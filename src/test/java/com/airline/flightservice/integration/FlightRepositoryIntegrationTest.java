
package com.airline.flightservice.integration;

import com.airline.flightservice.entity.Flight;
import com.airline.flightservice.repository.FlightRepository;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class FlightRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("airline_flight_db")
                    .withUsername("test")
                    .withPassword("test");

    @Autowired
    private FlightRepository flightRepository;


    // Helper method to create valid flight
    private Flight createFlight(String flightNumber) {

        return Flight.builder()
                .flightNumber(flightNumber)
                .source("delhi")
                .destination("mumbai")
                .departure(LocalDateTime.now().plusDays(2))
                .arrival(LocalDateTime.now().plusDays(2).plusHours(2))
                .totalSeats(100)
                .availableSeats(100)
                .price(new BigDecimal("5000.00"))
                .build();
    }


    // 1. Verify unique flight number constraint
    @Test
    void shouldRejectDuplicateFlightNumber() {

        flightRepository.saveAndFlush(createFlight("AI202"));

        Flight duplicateFlight = createFlight("AI202");

        assertThrows(
                DataIntegrityViolationException.class,
                () -> flightRepository.saveAndFlush(duplicateFlight)
        );
    }


    // 2. Verify total seats CHECK constraint
    @Test
    void shouldRejectZeroTotalSeats() {

        Flight flight = createFlight("AI203");
        flight.setTotalSeats(0);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> flightRepository.saveAndFlush(flight)
        );
    }


    // 3. Verify available seats CHECK constraint
    @Test
    void shouldRejectNegativeAvailableSeats() {

        Flight flight = createFlight("AI204");
        flight.setAvailableSeats(-5);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> flightRepository.saveAndFlush(flight)
        );
    }


    // 4. Verify available seats cannot exceed total seats
    @Test
    void shouldRejectAvailableSeatsGreaterThanTotalSeats() {

        Flight flight = createFlight("AI205");
        flight.setTotalSeats(100);
        flight.setAvailableSeats(120);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> flightRepository.saveAndFlush(flight)
        );
    }


    // 5. Verify price CHECK constraint
    @Test
    void shouldRejectZeroPrice() {

        Flight flight = createFlight("AI206");
        flight.setPrice(BigDecimal.ZERO);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> flightRepository.saveAndFlush(flight)
        );
    }


    // 6. Verify arrival must be after departure
    @Test
    void shouldRejectInvalidFlightSchedule() {

        Flight flight = createFlight("AI207");

        flight.setArrival(
                flight.getDeparture().minusHours(1)
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> flightRepository.saveAndFlush(flight)
        );
    }

}
