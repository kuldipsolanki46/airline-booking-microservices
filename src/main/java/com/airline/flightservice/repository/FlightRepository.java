package com.airline.flightservice.repository;

import com.airline.flightservice.entity.Flight;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {
    List<Flight> findBySourceAndDestination(String source, String destination);
    List<Flight> findByAvailableSeatsGreaterThan(int availableSeats);
    List<Flight> findByAvailableSeatsGreaterThanEqual(int availableSeats);
}
