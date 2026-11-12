package com.airline.flightservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.*;

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
    @Size(max = 20, message = "Flight number cannot exceed 20 characters")
    private String flightNumber;

    @NotBlank(message = "Source is required")
    @Size(max = 100, message = "Source cannot exceed 100 characters")
    private String source;

    @NotBlank(message = "Destination is required")
    @Size(max = 100, message = "Destination cannot exceed 100 characters")
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
