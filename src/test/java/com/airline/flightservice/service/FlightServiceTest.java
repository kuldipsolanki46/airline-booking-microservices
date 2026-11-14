package com.airline.flightservice.service;


import com.airline.flightservice.dto.FlightRequest;
import com.airline.flightservice.dto.FlightResponse;
import com.airline.flightservice.entity.Flight;
import com.airline.flightservice.exception.DuplicateFlightException;
import com.airline.flightservice.exception.FlightNotFoundException;
import com.airline.flightservice.repository.FlightRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FlightServiceTest {

    @Mock
    private FlightRepository flightRepository;

    @InjectMocks
    private FlightService flightService;


    // Helper method to create valid requests
    private FlightRequest createValidRequest() {

        return FlightRequest.builder()
                .flightNumber("AI202")
                .source("Delhi")
                .destination("Mumbai")
                .departure(LocalDateTime.now().plusDays(2))
                .arrival(LocalDateTime.now().plusDays(2).plusHours(2))
                .totalSeats(100)
                .price(new BigDecimal("5000.00"))
                .build();
    }


    // Helper method to create an existing flight
    private Flight createExistingFlight() {

        return Flight.builder()
                .id(1L)
                .flightNumber("AI202")
                .source("delhi")
                .destination("mumbai")
                .departure(LocalDateTime.now().plusDays(2))
                .arrival(LocalDateTime.now().plusDays(2).plusHours(2))
                .totalSeats(100)
                .availableSeats(70)
                .price(new BigDecimal("5000.00"))
                .build();
    }


    // 1. Create flight successfully
    @Test
    void shouldCreateFlightSuccessfully() {

        FlightRequest request = createValidRequest();

        when(flightRepository.existsByFlightNumber("AI202"))
                .thenReturn(false);

        when(flightRepository.save(any(Flight.class)))
                .thenAnswer(invocation -> {
                    Flight flight = invocation.getArgument(0);
                    flight.setId(1L);
                    return flight;
                });

        FlightResponse response = flightService.createFlight(request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("AI202", response.getFlightNumber());
        assertEquals(100, response.getTotalSeats());
        assertEquals(100, response.getAvailableSeats());

        verify(flightRepository).save(any(Flight.class));
    }


    // 2. Reject duplicate flight number
    @Test
    void shouldRejectDuplicateFlightNumber() {

        FlightRequest request = createValidRequest();

        when(flightRepository.existsByFlightNumber("AI202"))
                .thenReturn(true);

        assertThrows(
                DuplicateFlightException.class,
                () -> flightService.createFlight(request)
        );

        verify(flightRepository, never()).save(any(Flight.class));
    }


    // 3. Throw exception for nonexistent flight
    @Test
    void shouldThrowExceptionWhenFlightDoesNotExist() {

        when(flightRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                FlightNotFoundException.class,
                () -> flightService.getFlightById(99L)
        );
    }


    // 4. Update total seats while preserving booked seats
    @Test
    void shouldUpdateTotalSeatsAndPreserveBookedSeats() {

        Flight existingFlight = createExistingFlight();

        FlightRequest request = createValidRequest();

        // Keep the existing schedule unchanged
        request.setDeparture(existingFlight.getDeparture());
        request.setArrival(existingFlight.getArrival());

        request.setTotalSeats(120);

        when(flightRepository.findById(1L))
                .thenReturn(Optional.of(existingFlight));

        when(flightRepository.existsByFlightNumberAndIdNot("AI202", 1L))
                .thenReturn(false);

        FlightResponse response =
                flightService.updateFlight(1L, request);

        assertEquals(120, response.getTotalSeats());
        assertEquals(90, response.getAvailableSeats());
    }


    // 5. Reject reducing total seats below booked seats
    @Test
    void shouldRejectReducingTotalSeatsBelowBookedSeats() {

        Flight existingFlight = createExistingFlight();

        // 30 seats are already booked

        FlightRequest request = createValidRequest();
        request.setTotalSeats(20);

        when(flightRepository.findById(1L))
                .thenReturn(Optional.of(existingFlight));

        when(flightRepository.existsByFlightNumberAndIdNot("AI202", 1L))
                .thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> flightService.updateFlight(1L, request)
        );
    }


    // 6. Reject route changes when bookings exist
    @Test
    void shouldRejectRouteChangeWhenBookingsExist() {

        Flight existingFlight = createExistingFlight();

        // Existing flight has 30 booked seats

        FlightRequest request = createValidRequest();
        request.setSource("Bangalore");
        request.setDestination("Chennai");

        when(flightRepository.findById(1L))
                .thenReturn(Optional.of(existingFlight));

        when(flightRepository.existsByFlightNumberAndIdNot("AI202", 1L))
                .thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> flightService.updateFlight(1L, request)
        );
    }


    // 7. Reject invalid departure and arrival times
    @Test
    void shouldRejectInvalidDepartureAndArrivalTimes() {

        FlightRequest request = createValidRequest();

        // Arrival is before departure
        request.setArrival(request.getDeparture().minusHours(1));

        assertThrows(
                IllegalArgumentException.class,
                () -> flightService.createFlight(request)
        );

        verify(flightRepository, never()).save(any(Flight.class));
    }

    @Test
    void shouldNormalizeFlightDetailsWhenCreatingFlight() {

        FlightRequest request = createValidRequest();

        request.setFlightNumber(" ai202 ");
        request.setSource(" Delhi ");
        request.setDestination(" Mumbai ");

        when(flightRepository.existsByFlightNumber("AI202"))
                .thenReturn(false);

        when(flightRepository.save(any(Flight.class)))
                .thenAnswer(invocation -> {
                    Flight flight = invocation.getArgument(0);
                    flight.setId(1L);
                    return flight;
                });

        FlightResponse response = flightService.createFlight(request);

        assertEquals("AI202", response.getFlightNumber());
        assertEquals("delhi", response.getSource());
        assertEquals("mumbai", response.getDestination());
    }

    @Test
    void shouldRejectScheduleChangeWhenBookingsExist() {

        Flight existingFlight = createExistingFlight();

        FlightRequest request = createValidRequest();

        // Change both times while keeping arrival after departure
        request.setDeparture(
                existingFlight.getDeparture().plusHours(1)
        );

        request.setArrival(
                existingFlight.getArrival().plusHours(1)
        );

        when(flightRepository.findById(1L))
                .thenReturn(Optional.of(existingFlight));

        when(flightRepository.existsByFlightNumberAndIdNot("AI202", 1L))
                .thenReturn(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> flightService.updateFlight(1L, request)
        );
    }

}