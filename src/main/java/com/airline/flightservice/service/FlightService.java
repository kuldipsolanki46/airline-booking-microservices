package com.airline.flightservice.service;

import com.airline.flightservice.dto.FlightRequest;
import com.airline.flightservice.dto.FlightResponse;
import com.airline.flightservice.entity.Flight;
import com.airline.flightservice.repository.FlightRepository;

import java.util.List;


import org.springframework.stereotype.Service;




@Service
public class FlightService {
    private final FlightRepository flightRepository;

    FlightService(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    private Flight flightBuild (FlightRequest flightRequest) {
        Flight flight = Flight.builder()
                .flightNumber(flightRequest.getFlightNumber())
                .source(flightRequest.getSource())
                .destination(flightRequest.getDestination())
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

    private void validateFlightTimes(FlightRequest flightRequest) {
        if (!flightRequest.getDeparture()
                .isBefore(flightRequest.getArrival())) {
            throw new IllegalArgumentException(
                    "Departure must be before arrival"
            );
        }
    }

    public List<FlightResponse> getAllFlights() {
        return flightRepository.findAll()
                .stream()
                .map(this::responseBuild)
                .toList();
    }

    public FlightResponse getFlightById(Long id) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Flight not found"));

        return responseBuild(flight);
    }

    public FlightResponse createFlight(FlightRequest flightRequest) {
        validateFlightTimes(flightRequest);

        Flight flight = flightBuild(flightRequest);
        flight.setAvailableSeats(flight.getTotalSeats());

        flightRepository.save(flight);

        return responseBuild(flight);
    }

    public FlightResponse updateFlight(Long id, FlightRequest flightRequest) {
        validateFlightTimes(flightRequest);

        Flight existingFlight = flightRepository.findById(id).orElse(null);
        if (existingFlight != null) {
            existingFlight.setFlightNumber(flightRequest.getFlightNumber());
            existingFlight.setSource(flightRequest.getSource());
            existingFlight.setDestination(flightRequest.getDestination());
            existingFlight.setDeparture(flightRequest.getDeparture());
            existingFlight.setArrival(flightRequest.getArrival());
            existingFlight.setTotalSeats(flightRequest.getTotalSeats());
            existingFlight.setPrice(flightRequest.getPrice());
            existingFlight.setUpdatedAt(java.time.LocalDateTime.now());
            flightRepository.save(existingFlight);

            return responseBuild(existingFlight);
        }
        throw new RuntimeException("Flight not found");
    }

    public void deleteFlight(Long id) {
        Flight flight = flightRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Flight not found"));

        flightRepository.delete(flight);
    }
    public List<FlightResponse> searchFlights(String source, String destination) {
        List<Flight> flights = flightRepository.findBySourceAndDestination(source, destination);
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
        Flight flight = flightRepository.findById(id).orElse(null);
        if (flight != null) {
            return flight.getAvailableSeats();
        }
        throw new RuntimeException("Flight not found");
    }
}
