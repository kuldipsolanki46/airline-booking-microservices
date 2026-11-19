package com.airline.flightservice.integration;

import com.airline.flightservice.entity.Flight;
import com.airline.flightservice.repository.FlightRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

// Keep @SpringBootTest so this remains compatible with the dependencies in your ZIP.
// Once you confirm spring-boot-starter-data-jpa-test is present, @DataJpaTest is
// a faster alternative for repository-only tests.
@SpringBootTest
@Testcontainers
@Transactional // Roll back each test, including database constraint tests.
class FlightRepositoryIntegrationTest {

    private static final LocalDateTime DEPARTURE = LocalDateTime.now().plusDays(3).withNano(0);
    private static final LocalDateTime ARRIVAL = DEPARTURE.plusHours(2);

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =   new PostgreSQLContainer<>("postgres:16-alpine")
                                                        .withDatabaseName("airline_flight_db")
                                                        .withUsername("test")
                                                        .withPassword("test");

    @Autowired
    private FlightRepository flightRepository;

    @Autowired
    private EntityManager entityManager;

    private Flight createFlight(String flightNumber) {
        return Flight.builder()
                .flightNumber(flightNumber)
                .source("delhi")
                .destination("mumbai")
                .departure(DEPARTURE)
                .arrival(ARRIVAL)
                .totalSeats(100)
                .availableSeats(100)
                .price(new BigDecimal("5000.00"))
                .build();
    }

    @Test
    void shouldSaveAndReadValidFlight() {
        Flight saved = flightRepository.saveAndFlush(createFlight("AI999"));
        assertNotNull(saved.getId());

        // Clear the first-level JPA cache to force retrieval from PostgreSQL.
        entityManager.clear();
        Flight loaded = flightRepository.findById(saved.getId()).orElseThrow();

        assertEquals("AI999", loaded.getFlightNumber());
        assertEquals(100, loaded.getTotalSeats());
        assertEquals(100, loaded.getAvailableSeats());
        assertEquals(DEPARTURE, loaded.getDeparture());
    }

    @Test
    void shouldRejectDuplicateFlightNumber() {
        flightRepository.saveAndFlush(createFlight("AI202"));

        assertThrows(DataIntegrityViolationException.class,
                () -> flightRepository.saveAndFlush(createFlight("AI202")));
    }

    @Test
    void shouldRejectZeroTotalSeats() {
        Flight flight = createFlight("AI203");
        flight.setTotalSeats(0);

        assertThrows(DataIntegrityViolationException.class,
                () -> flightRepository.saveAndFlush(flight));
    }

    @Test
    void shouldRejectNegativeAvailableSeats() {
        Flight flight = createFlight("AI204");
        flight.setAvailableSeats(-5);

        assertThrows(DataIntegrityViolationException.class,
                () -> flightRepository.saveAndFlush(flight));
    }

    @Test
    void shouldRejectAvailableSeatsGreaterThanTotalSeats() {
        Flight flight = createFlight("AI205");
        flight.setAvailableSeats(120);

        assertThrows(DataIntegrityViolationException.class,
                () -> flightRepository.saveAndFlush(flight));
    }

    @Test
    void shouldRejectZeroPrice() {
        Flight flight = createFlight("AI206");
        flight.setPrice(BigDecimal.ZERO);

        assertThrows(DataIntegrityViolationException.class,
                () -> flightRepository.saveAndFlush(flight));
    }

    @Test
    void shouldRejectInvalidFlightSchedule() {
        Flight flight = createFlight("AI207");
        flight.setArrival(DEPARTURE.minusHours(1));

        assertThrows(DataIntegrityViolationException.class,
                () -> flightRepository.saveAndFlush(flight));
    }
}
