package com.airline.flightservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlightRequest {
    private String flightNumber;

    private String source;
    private String destination;

    private LocalDateTime departure;
    private LocalDateTime arrival;

    private int totalSeats;

    private BigDecimal price;
}
