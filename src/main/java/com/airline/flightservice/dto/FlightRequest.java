package com.airline.flightservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Future;

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
    @NotBlank(message = "Flight number is required")
    private String flightNumber;

    @NotBlank(message = "Source is required")
    private String source;
    @NotBlank(message = "Destination is required")
    private String destination;

    @NotNull(message = "Departure time is required")
    @Future(message = "Departure must be in the future")
    private LocalDateTime departure;

    @NotNull(message = "Arrival time is required")
    @Future(message = "Arrival must be in the future")
    private LocalDateTime arrival;

    @NotNull(message = "Total seats are required")
    @Positive(message = "Total seats must be greater than zero")
    private Integer totalSeats;

    @NotNull(message = "Price is required")
    @PositiveOrZero(message = "Price cannot be negative")
    private BigDecimal price;
}
