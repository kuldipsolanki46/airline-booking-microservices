
package com.airline.flightservice.controller;

import com.airline.flightservice.dto.FlightRequest;
import com.airline.flightservice.dto.FlightResponse;
import com.airline.flightservice.exception.DuplicateFlightException;
import com.airline.flightservice.exception.FlightNotFoundException;
import com.airline.flightservice.service.FlightService;


import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(FlightController.class)
class FlightControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FlightService flightService;


    private FlightRequest createValidRequest() {

        return FlightRequest.builder()
                .flightNumber("AI202")
                .source("Delhi")
                .destination("Mumbai")
                .departure(LocalDateTime.now().plusDays(2))
                .arrival(LocalDateTime.now().plusDays(2).plusHours(2))
                .totalSeats(100)
                .price(new BigDecimal("5000.00"))
                .build();
    }


    private FlightResponse createFlightResponse() {

        return FlightResponse.builder()
                .id(1L)
                .flightNumber("AI202")
                .source("delhi")
                .destination("mumbai")
                .departure(LocalDateTime.now().plusDays(2))
                .arrival(LocalDateTime.now().plusDays(2).plusHours(2))
                .totalSeats(100)
                .availableSeats(100)
                .price(new BigDecimal("5000.00"))
                .build();
    }


    // 1. POST returns 201 Created
    @Test
    void shouldReturn201WhenFlightCreated() throws Exception {

        FlightRequest request = createValidRequest();

        when(flightService.createFlight(any(FlightRequest.class)))
                .thenReturn(createFlightResponse());

        mockMvc.perform(post("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flightNumber").value("AI202"));
    }


    // 2. GET returns 200 OK
    @Test
    void shouldReturn200WhenFlightFound() throws Exception {

        when(flightService.getFlightById(1L))
                .thenReturn(createFlightResponse());

        mockMvc.perform(get("/api/v1/flights/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.flightNumber").value("AI202"));
    }


    // 3. Invalid request returns 400
    @Test
    void shouldReturn400ForInvalidRequest() throws Exception {

        FlightRequest request = createValidRequest();

        request.setFlightNumber("");

        mockMvc.perform(post("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(flightService, never())
                .createFlight(any(FlightRequest.class));
    }


    // 4. Missing flight returns 404
    @Test
    void shouldReturn404WhenFlightDoesNotExist() throws Exception {

        when(flightService.getFlightById(99L))
                .thenThrow(new FlightNotFoundException(99L));

        mockMvc.perform(get("/api/v1/flights/99"))
                .andExpect(status().isNotFound());
    }


    // 5. Duplicate flight returns 409
    @Test
    void shouldReturn409WhenDuplicateFlightCreated() throws Exception {

        FlightRequest request = createValidRequest();

        when(flightService.createFlight(any(FlightRequest.class)))
                .thenThrow(new DuplicateFlightException("AI202"));

        mockMvc.perform(post("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }


    // 6. DELETE returns 204 No Content
    @Test
    void shouldReturn204WhenFlightDeleted() throws Exception {

        doNothing().when(flightService).deleteFlight(1L);

        mockMvc.perform(delete("/api/v1/flights/1"))
                .andExpect(status().isNoContent());

        verify(flightService).deleteFlight(1L);
    }

    //7. Pagination and sorting returns 200 OK
    @Test
    void shouldReturn200WhenFlightsAreFetched() throws Exception {

        Page<FlightResponse> flightPage =
                new PageImpl<>(
                        List.of(createFlightResponse()),
                        PageRequest.of(0, 10),
                        1
                );

        when(flightService.getAllFlights(any(Pageable.class)))
                .thenReturn(flightPage);

        mockMvc.perform(get("/api/v1/flights")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "id")
                        .param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].flightNumber")
                        .value("AI202"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // 8. Invalid page number returns 400
    @Test
    void shouldReturn400WhenPageSizeIsInvalid() throws Exception {

        mockMvc.perform(get("/api/v1/flights")
                        .param("page", "0")
                        .param("size", "200"))
                .andExpect(status().isBadRequest());
    }

}
