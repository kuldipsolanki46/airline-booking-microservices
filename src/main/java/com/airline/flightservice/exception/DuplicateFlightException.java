package com.airline.flightservice.exception;

public class DuplicateFlightException extends RuntimeException {

    public DuplicateFlightException(String flightNumber) {
        super("Flight with flight number " + flightNumber + " already exists.");
    }
}
