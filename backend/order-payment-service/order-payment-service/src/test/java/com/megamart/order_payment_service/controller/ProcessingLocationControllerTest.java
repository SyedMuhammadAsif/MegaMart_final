package com.megamart.order_payment_service.controller;

import com.megamart.order_payment_service.entity.ProcessingLocation;
import com.megamart.order_payment_service.repository.ProcessingLocationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProcessingLocationController.class)
class ProcessingLocationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProcessingLocationRepository locationRepository;

    @Test
    @WithMockUser
    void getAllLocations_Success() throws Exception {
        ProcessingLocation location = ProcessingLocation.builder()
                .id(1L)
                .name("Main Warehouse")
                .city("New York")
                .state("NY")
                .country("USA")
                .address("123 Main St")
                .active(true)
                .build();

        when(locationRepository.findByActiveTrue()).thenReturn(Arrays.asList(location));

        mockMvc.perform(get("/api/processing-locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Main Warehouse"))
                .andExpect(jsonPath("$[0].city").value("New York"));
    }

    @Test
    @WithMockUser
    void getAllLocations_EmptyList() throws Exception {
        when(locationRepository.findByActiveTrue()).thenReturn(Arrays.asList());

        mockMvc.perform(get("/api/processing-locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}