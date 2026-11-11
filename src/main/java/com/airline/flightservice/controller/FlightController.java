package com.airline.flightservice.controller;

import com.airline.flightservice.dto.FlightRequest;
import com.airline.flightservice.dto.FlightResponse;
import com.airline.flightservice.service.FlightService;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;



@RestController
@RequestMapping("/flights")
public class FlightController {
    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    //for get requests, we can use @GetMapping
    @GetMapping
    public List<FlightResponse> getAllFlights() {
        return flightService.getAllFlights();
    }

    @GetMapping("/{id}")
    public FlightResponse getFlightById(@PathVariable Long id) {
        return flightService.getFlightById(id);
    }

    //for post requests, we can use @PostMapping
    @PostMapping
    public FlightResponse createFlight(@RequestBody FlightRequest flightRequest) {
        return flightService.createFlight(flightRequest);
    }

    //for put requests, we can use @PutMapping
    @PutMapping("/{id}")
    public FlightResponse updateFlight(@PathVariable Long id, @RequestBody FlightRequest flightRequest) {
        return flightService.updateFlight(id, flightRequest);
    }

    //for delete requests, we can use @DeleteMapping
    @DeleteMapping("/{id}")
    public void deleteFlight(@PathVariable Long id) {
        flightService.deleteFlight(id);
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
