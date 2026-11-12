package com.airline.flightservice.service;

import com.airline.flightservice.dto.FlightRequest;
import com.airline.flightservice.dto.FlightResponse;
import com.airline.flightservice.entity.Flight;
import com.airline.flightservice.exception.DuplicateFlightException;
import com.airline.flightservice.exception.FlightNotFoundException;
import com.airline.flightservice.repository.FlightRepository;

import java.util.List;
import java.util.Objects;


import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;


@Service
public class FlightService {
    private final FlightRepository flightRepository;

    public FlightService(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    private String normalizeLocation(String location) {
        return location.trim().toLowerCase();
    }

    private String normalizeFlightNumber(String flightNumber) {
        return flightNumber.trim().toUpperCase();
    }

    private Flight flightBuild (FlightRequest flightRequest) {
        Flight flight = Flight.builder()
                .flightNumber(normalizeFlightNumber(flightRequest.getFlightNumber()))
                .source(normalizeLocation(flightRequest.getSource()))
                .destination(normalizeLocation(flightRequest.getDestination()))
                .departure(flightRequest.getDeparture())
                .arrival(flightRequest.getArrival())
                .totalSeats(flightRequest.getTotalSeats())
                .price(flightRequest.getPrice())
                .build();

        return flight;
    }

    private FlightResponse responseBuild (Flight flight) {
        FlightResponse flightResponse = FlightResponse.builder()
                .id(flight.getId())
                .flightNumber(flight.getFlightNumber())
                .source(flight.getSource())
                .destination(flight.getDestination())
                .departure(flight.getDeparture())
                .arrival(flight.getArrival())
                .totalSeats(flight.getTotalSeats())
                .availableSeats(flight.getAvailableSeats())
                .price(flight.getPrice())
                .createdAt(flight.getCreatedAt())
                .updatedAt(flight.getUpdatedAt())
                .build();

        return flightResponse;
    }

    private void validateFlightRequest(FlightRequest request) {

        if (!request.getDeparture().isBefore(request.getArrival())) {
            throw new IllegalArgumentException("Departure must be before arrival");
        }

        if (normalizeLocation(request.getSource()).equals(normalizeLocation(request.getDestination()))) {

            throw new IllegalArgumentException("Source and destination cannot be the same");
        }
    }

    public Page<FlightResponse> getAllFlights(Pageable pageable) {
        return flightRepository.findAll(pageable)
                .map(this::responseBuild);
    }

    public FlightResponse getFlightById(Long id) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new FlightNotFoundException(id));

        return responseBuild(flight);
    }

    public FlightResponse createFlight(FlightRequest flightRequest) {
        validateFlightRequest(flightRequest);

        String flightNumber = normalizeFlightNumber(flightRequest.getFlightNumber());

        if (flightRepository.existsByFlightNumber((flightNumber))) {
            throw new DuplicateFlightException(flightNumber);
        }

        Flight flight = flightBuild(flightRequest);
        flight.setAvailableSeats(flight.getTotalSeats());

        flightRepository.save(flight);

        return responseBuild(flight);
    }

    @Transactional
    public FlightResponse updateFlight(Long id, FlightRequest flightRequest) {
        validateFlightRequest(flightRequest);


        Flight existingFlight = flightRepository.findById(id).orElseThrow(() -> new FlightNotFoundException(id));
        String flightNumber = normalizeFlightNumber(flightRequest.getFlightNumber());

        if (flightRepository.existsByFlightNumberAndIdNot((flightNumber), id)) {
            throw new DuplicateFlightException(flightNumber);
        }

        int bookedSeats = existingFlight.getTotalSeats()
                - existingFlight.getAvailableSeats();

        if (bookedSeats > 0) {
            boolean routeChanged = !Objects.equals(existingFlight.getSource(), normalizeLocation(flightRequest.getSource())) ||
                                    !Objects.equals(existingFlight.getDestination(), normalizeLocation(flightRequest.getDestination()));
            if (routeChanged) {
                throw new IllegalArgumentException( "Cannot change flight route when bookings exist");
            }
        }

        if (flightRequest.getTotalSeats() < bookedSeats) {
            throw new IllegalArgumentException("Total seats cannot be less than booked seats");
        }

        boolean scheduleChanged = !Objects.equals(existingFlight.getDeparture(), flightRequest.getDeparture()) ||
                                    !Objects.equals(existingFlight.getArrival(), flightRequest.getArrival());

        if (bookedSeats > 0 && scheduleChanged) {
            throw new IllegalArgumentException("Cannot change flight schedule when bookings exist");
        }

        existingFlight.setFlightNumber(flightNumber);
        existingFlight.setSource(normalizeLocation(flightRequest.getSource()));
        existingFlight.setDestination(normalizeLocation(flightRequest.getDestination()));
        existingFlight.setDeparture(flightRequest.getDeparture());
        existingFlight.setArrival(flightRequest.getArrival());
        existingFlight.setTotalSeats(flightRequest.getTotalSeats());
        existingFlight.setAvailableSeats(flightRequest.getTotalSeats() - bookedSeats);
        existingFlight.setPrice(flightRequest.getPrice());

//        flightRepository.save(existingFlight);
        // No need to explicitly save the existingFlight here because the @Transactional annotation ensures that changes to the managed entity are automatically persisted when the transaction commits.

        return responseBuild(existingFlight);
    }

    public void deleteFlight(Long id) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new FlightNotFoundException(id));

        flightRepository.delete(flight);
    }

    public List<FlightResponse> searchFlights(String source, String destination) {
        List<Flight> flights = flightRepository.findBySourceAndDestination(normalizeLocation(source), normalizeLocation(destination));
        List<FlightResponse> flightResponses = flights.stream()
                .map(this::responseBuild)
                .toList();
        return flightResponses;
    }

    public List<FlightResponse> searchFlightsByAvailableSeats(int availableSeats) {
        List<Flight> flights = flightRepository.findByAvailableSeatsGreaterThanEqual(availableSeats);
        List<FlightResponse> flightResponses = flights.stream()
                .map(this::responseBuild)
                .toList();
        return flightResponses;
    }

    public int getAvailableSeats(Long id) {
        Flight flight = flightRepository.findById(id).orElseThrow(() -> new FlightNotFoundException(id));

        return flight.getAvailableSeats();
    }
}
