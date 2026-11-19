package com.airline.flightservice.controller;

import com.airline.flightservice.dto.FlightRequest;
import com.airline.flightservice.dto.FlightResponse;
import com.airline.flightservice.exception.DuplicateFlightException;
import com.airline.flightservice.exception.FlightNotFoundException;
import com.airline.flightservice.service.FlightService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FlightController.class)
class FlightControllerTest {

    private static final LocalDateTime DEPARTURE = LocalDateTime.now().plusDays(3).withNano(0);
    private static final LocalDateTime ARRIVAL = DEPARTURE.plusHours(2);

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
                .departure(DEPARTURE)
                .arrival(ARRIVAL)
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
                .departure(DEPARTURE)
                .arrival(ARRIVAL)
                .totalSeats(100)
                .availableSeats(100)
                .price(new BigDecimal("5000.00"))
                .build();
    }

    @Test
    void shouldReturn201WhenFlightCreated() throws Exception {
        FlightRequest request = createValidRequest();
        when(flightService.createFlight(any(FlightRequest.class)))
                .thenReturn(createFlightResponse());

        mockMvc.perform(post("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.flightNumber").value("AI202"));

        ArgumentCaptor<FlightRequest> requestCaptor = ArgumentCaptor.forClass(FlightRequest.class);
        verify(flightService).createFlight(requestCaptor.capture());
        assertEquals("AI202", requestCaptor.getValue().getFlightNumber());
        assertEquals(100, requestCaptor.getValue().getTotalSeats());
    }

    @Test
    void shouldReturn200WhenFlightFound() throws Exception {
        when(flightService.getFlightById(1L)).thenReturn(createFlightResponse());

        mockMvc.perform(get("/api/v1/flights/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.flightNumber").value("AI202"));

        verify(flightService).getFlightById(1L);
    }

    @Test
    void shouldReturn400ForInvalidRequest() throws Exception {
        FlightRequest request = createValidRequest();
        request.setFlightNumber("");

        mockMvc.perform(post("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(flightService, never()).createFlight(any(FlightRequest.class));
    }

    @Test
    void shouldReturn404WhenFlightDoesNotExist() throws Exception {
        when(flightService.getFlightById(99L))
                .thenThrow(new FlightNotFoundException(99L));

        mockMvc.perform(get("/api/v1/flights/99"))
                .andExpect(status().isNotFound());

        verify(flightService).getFlightById(99L);
    }

    @Test
    void shouldReturn409WhenDuplicateFlightCreated() throws Exception {
        FlightRequest request = createValidRequest();
        when(flightService.createFlight(any(FlightRequest.class)))
                .thenThrow(new DuplicateFlightException("AI202"));

        mockMvc.perform(post("/api/v1/flights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());

        verify(flightService).createFlight(any(FlightRequest.class));
    }

    @Test
    void shouldReturn204WhenFlightDeleted() throws Exception {
        // Mockito void methods do nothing by default; no doNothing() stubbing needed.
        mockMvc.perform(delete("/api/v1/flights/1"))
                .andExpect(status().isNoContent());

        verify(flightService).deleteFlight(1L);
    }

    @Test
    void shouldReturn200WithCorrectPaginationAndSorting() throws Exception {
        Page<FlightResponse> flightPage = new PageImpl<>(
                List.of(createFlightResponse()),
                PageRequest.of(0, 10),
                1
        );
        when(flightService.getAllFlights(any(Pageable.class))).thenReturn(flightPage);

        mockMvc.perform(get("/api/v1/flights")
                        .param("page", "0")
                        .param("size", "10")
                        .param("sortBy", "id")
                        .param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].flightNumber").value("AI202"))
                .andExpect(jsonPath("$.totalElements").value(1));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(flightService).getAllFlights(pageableCaptor.capture());
        Pageable actual = pageableCaptor.getValue();
        assertEquals(0, actual.getPageNumber());
        assertEquals(10, actual.getPageSize());
        Sort.Order order = actual.getSort().getOrderFor("id");
        assertNotNull(order, "Expected sorting by id");
        assertEquals(Sort.Direction.ASC, order.getDirection());
    }

    @Test
    void shouldReturn400WhenPageSizeIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/flights")
                        .param("page", "0")
                        .param("size", "200"))
                .andExpect(status().isBadRequest());

        verify(flightService, never()).getAllFlights(any(Pageable.class));
    }
}
