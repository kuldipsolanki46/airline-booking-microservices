package com.airline.flightservice.service;

import com.airline.flightservice.dto.FlightRequest;
import com.airline.flightservice.dto.FlightResponse;
import com.airline.flightservice.entity.Flight;
import com.airline.flightservice.exception.DuplicateFlightException;
import com.airline.flightservice.exception.FlightNotFoundException;
import com.airline.flightservice.repository.FlightRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class FlightServiceTest {

    // Shared timestamps avoid accidental differences between test fixtures.
    private static final LocalDateTime DEPARTURE = LocalDateTime.now().plusDays(3).withNano(0);
    private static final LocalDateTime ARRIVAL = DEPARTURE.plusHours(2);

    @Mock
    private FlightRepository flightRepository;

    @InjectMocks
    private FlightService flightService;

    private FlightRequest createValidRequest() {
        return FlightRequest.builder()
                .flightNumber("AI202")
                .source("Delhi")
                .destination("Mumbai")
                .departure(DEPARTURE)
                .arrival(ARRIVAL)
                .totalSeats(100)
                .price(new BigDecimal("5000.00"))
                .build();
    }

    private Flight createExistingFlight() {
        return Flight.builder()
                .id(1L)
                .flightNumber("AI202")
                .source("delhi")
                .destination("mumbai")
                .departure(DEPARTURE)
                .arrival(ARRIVAL)
                .totalSeats(100)
                .availableSeats(70) // 30 seats already booked
                .price(new BigDecimal("5000.00"))
                .build();
    }

    @Test
    void shouldCreateFlightSuccessfully() {
        // Arrange
        FlightRequest request = createValidRequest();
        when(flightRepository.save(any(Flight.class))).thenAnswer(invocation -> {
            Flight flight = invocation.getArgument(0);
            flight.setId(1L);
            return flight;
        });

        // Act
        FlightResponse response = flightService.createFlight(request);

        // Assert: output and data sent to the repository
        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(1L, response.getId()),
                () -> assertEquals("AI202", response.getFlightNumber()),
                () -> assertEquals(100, response.getTotalSeats()),
                () -> assertEquals(100, response.getAvailableSeats())
        );

        ArgumentCaptor<Flight> savedFlight = ArgumentCaptor.forClass(Flight.class);
        verify(flightRepository).existsByFlightNumber("AI202");
        verify(flightRepository).save(savedFlight.capture());
        assertEquals("AI202", savedFlight.getValue().getFlightNumber());
        assertEquals(DEPARTURE, savedFlight.getValue().getDeparture());
        assertEquals(100, savedFlight.getValue().getAvailableSeats());
    }

    @Test
    void shouldRejectDuplicateFlightNumber() {
        FlightRequest request = createValidRequest();
        when(flightRepository.existsByFlightNumber("AI202")).thenReturn(true);

        assertThrows(DuplicateFlightException.class,
                () -> flightService.createFlight(request));

        verify(flightRepository).existsByFlightNumber("AI202");
        verify(flightRepository, never()).save(any(Flight.class));
    }

    @Test
    void shouldThrowExceptionWhenFlightDoesNotExist() {
        when(flightRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(FlightNotFoundException.class,
                () -> flightService.getFlightById(99L));

        verify(flightRepository).findById(99L);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingMissingFlight() {
        when(flightRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(FlightNotFoundException.class,
                () -> flightService.updateFlight(99L, createValidRequest()));

        verify(flightRepository).findById(99L);
        verify(flightRepository, never()).save(any(Flight.class));
    }

    @Test
    void shouldUpdateTotalSeatsAndPreserveBookedSeats() {
        Flight existingFlight = createExistingFlight();
        FlightRequest request = createValidRequest();
        request.setTotalSeats(120); // Only the capacity changes.

        when(flightRepository.findById(1L)).thenReturn(Optional.of(existingFlight));

        FlightResponse response = flightService.updateFlight(1L, request);

        assertEquals(120, response.getTotalSeats());
        assertEquals(90, response.getAvailableSeats()); // 120 - 30 booked
        verify(flightRepository).findById(1L);
    }

    @Test
    void shouldRejectReducingTotalSeatsBelowBookedSeats() {
        Flight existingFlight = createExistingFlight();
        FlightRequest request = createValidRequest();
        request.setTotalSeats(20); // Only capacity changes: 20 < 30 booked.

        when(flightRepository.findById(1L)).thenReturn(Optional.of(existingFlight));

        assertThrows(IllegalArgumentException.class,
                () -> flightService.updateFlight(1L, request));
        verify(flightRepository, never()).save(any(Flight.class));
    }

    @Test
    void shouldRejectRouteChangeWhenBookingsExist() {
        Flight existingFlight = createExistingFlight();
        FlightRequest request = createValidRequest();
        request.setSource("Bangalore");
        request.setDestination("Chennai");

        when(flightRepository.findById(1L)).thenReturn(Optional.of(existingFlight));

        assertThrows(IllegalArgumentException.class,
                () -> flightService.updateFlight(1L, request));
        verify(flightRepository, never()).save(any(Flight.class));
    }

    @Test
    void shouldRejectInvalidDepartureAndArrivalTimes() {
        FlightRequest request = createValidRequest();
        request.setArrival(DEPARTURE.minusHours(1));

        assertThrows(IllegalArgumentException.class,
                () -> flightService.createFlight(request));
        verify(flightRepository, never()).save(any(Flight.class));
    }

    @Test
    void shouldNormalizeFlightDetailsWhenCreatingFlight() {
        FlightRequest request = createValidRequest();
        request.setFlightNumber(" ai202 ");
        request.setSource(" Delhi ");
        request.setDestination(" Mumbai ");

        when(flightRepository.save(any(Flight.class))).thenAnswer(invocation -> {
            Flight flight = invocation.getArgument(0);
            flight.setId(1L);
            return flight;
        });

        FlightResponse response = flightService.createFlight(request);

        assertEquals("AI202", response.getFlightNumber());
        assertEquals("delhi", response.getSource());
        assertEquals("mumbai", response.getDestination());

        ArgumentCaptor<Flight> savedFlight = ArgumentCaptor.forClass(Flight.class);
        verify(flightRepository).existsByFlightNumber("AI202");
        verify(flightRepository).save(savedFlight.capture());
        assertEquals("AI202", savedFlight.getValue().getFlightNumber());
        assertEquals("delhi", savedFlight.getValue().getSource());
        assertEquals("mumbai", savedFlight.getValue().getDestination());
    }

    @Test
    void shouldRejectScheduleChangeWhenBookingsExist() {
        Flight existingFlight = createExistingFlight();
        FlightRequest request = createValidRequest();
        request.setDeparture(DEPARTURE.plusHours(1));
        request.setArrival(ARRIVAL.plusHours(1));

        when(flightRepository.findById(1L)).thenReturn(Optional.of(existingFlight));

        assertThrows(IllegalArgumentException.class,
                () -> flightService.updateFlight(1L, request));
        verify(flightRepository, never()).save(any(Flight.class));
    }
}
