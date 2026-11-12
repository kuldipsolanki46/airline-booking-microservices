package com.airline.flightservice.controller;

import com.airline.flightservice.dto.FlightRequest;
import com.airline.flightservice.dto.FlightResponse;
import com.airline.flightservice.service.FlightService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.http.ResponseEntity;

import jakarta.validation.Valid;

import java.util.List;



@RestController
@RequestMapping("/api/v1/flights")
public class FlightController {
    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    //for get requests, we can use @GetMapping
    @GetMapping
    public Page<FlightResponse> getAllFlights(@RequestParam (defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "10") int size,
                                             @RequestParam(defaultValue = "id") String sortBy,
                                             @RequestParam(defaultValue = "asc") String sortDir) {
        if (page < 0) {
            throw new IllegalArgumentException("Page cannot be negative");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100" );
        }

        List<String> allowedSortFields = List.of(
                "id", "flightNumber", "source",
                "destination", "departure", "arrival", "price"
        );

        if (!allowedSortFields.contains(sortBy)) {
            throw new IllegalArgumentException("Invalid sort field");
        }

        if (!sortDir.equalsIgnoreCase("asc") &&  !sortDir.equalsIgnoreCase("desc")) {
            throw new IllegalArgumentException( "Sort direction must be asc or desc");
        }

        Sort sort = sortDir.equalsIgnoreCase("desc")? Sort.by(sortBy).descending(): Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return flightService.getAllFlights(pageable);
    }

    @GetMapping("/{id}")
    public FlightResponse getFlightById(@PathVariable Long id) {
        return flightService.getFlightById(id);
    }

    //for post requests, we can use @PostMapping
    @PostMapping
    public ResponseEntity<FlightResponse> createFlight(@Valid @RequestBody FlightRequest flightRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(flightService.createFlight(flightRequest));
    }

    //for put requests, we can use @PutMapping
    @PutMapping("/{id}")
    public FlightResponse updateFlight(@PathVariable Long id, @Valid @RequestBody FlightRequest flightRequest) {
        return flightService.updateFlight(id, flightRequest);
    }

    //for delete requests, we can use @DeleteMapping
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFlight(@PathVariable Long id) {
        flightService.deleteFlight(id);
        return ResponseEntity.noContent().build();
    }

    //search flight
    @GetMapping("/search")
    public List<FlightResponse> searchFlights(@RequestParam String source, @RequestParam String destination) {
        return flightService.searchFlights(source, destination);
    }

    @GetMapping("/search/availableSeats")
    public List<FlightResponse> searchFlightsByAvailableSeats(@RequestParam int availableSeats) {
        return flightService.searchFlightsByAvailableSeats(availableSeats);
    }

    //Flight seat availibility
    @GetMapping("/{id}/availableSeats")
    public int getAvailableSeats(@PathVariable Long id) {
        return flightService.getAvailableSeats(id);
    }
}
