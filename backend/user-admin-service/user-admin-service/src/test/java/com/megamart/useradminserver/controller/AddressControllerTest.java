package com.megamart.useradminserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.megamart.useradminserver.dto.AddressDto;
import com.megamart.useradminserver.entity.User;
import com.megamart.useradminserver.entity.UserAddress;
import com.megamart.useradminserver.service.AddressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AddressController.class, 
    excludeAutoConfiguration = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
    })
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.config.import=",
    "spring.cloud.config.enabled=false",
    "spring.cloud.discovery.enabled=false",
    "eureka.client.enabled=false"
})
class AddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AddressService addressService;

    @Autowired
    private ObjectMapper objectMapper;

    private UserAddress testAddress;
    private AddressDto addressDto;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("John Doe");

        testAddress = new UserAddress();
        testAddress.setId(1L);
        testAddress.setUser(testUser);
        testAddress.setFullName("John Doe");
        testAddress.setAddressLine1("123 Main St");
        testAddress.setCity("New York");
        testAddress.setState("NY");
        testAddress.setPostalCode("10001");
        testAddress.setCountry("USA");
        testAddress.setPhone("1234567890");
        testAddress.setIsDefault(true);

        addressDto = new AddressDto();
        addressDto.setFullName("John Doe");
        addressDto.setAddressLine1("123 Main St");
        addressDto.setCity("New York");
        addressDto.setState("NY");
        addressDto.setPostalCode("10001");
        addressDto.setCountry("USA");
        addressDto.setPhone("1234567890");
        addressDto.setIsDefault(true);
    }

    @Test
    void getUserAddresses_ShouldReturnAddressList() throws Exception {
        List<UserAddress> addresses = Arrays.asList(testAddress);
        when(addressService.getUserAddresses(1L)).thenReturn(addresses);

        mockMvc.perform(get("/api/users/1/addresses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].fullName").value("John Doe"))
                .andExpect(jsonPath("$[0].addressLine1").value("123 Main St"));

        verify(addressService).getUserAddresses(1L);
    }

    @Test
    void getAddressById_ShouldReturnAddress() throws Exception {
        when(addressService.getAddressById(1L, 1L)).thenReturn(testAddress);

        mockMvc.perform(get("/api/users/1/addresses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.fullName").value("John Doe"));

        verify(addressService).getAddressById(1L, 1L);
    }

    @Test
    void addAddress_ShouldCreateAddress() throws Exception {
        when(addressService.addAddress(eq(1L), any(AddressDto.class))).thenReturn(testAddress);

        mockMvc.perform(post("/api/users/1/addresses")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addressDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fullName").value("John Doe"));

        verify(addressService).addAddress(eq(1L), any(AddressDto.class));
    }

    @Test
    void updateAddress_ShouldUpdateAddress() throws Exception {
        when(addressService.updateAddress(eq(1L), eq(1L), any(AddressDto.class))).thenReturn(testAddress);

        mockMvc.perform(put("/api/users/1/addresses/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addressDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("John Doe"));

        verify(addressService).updateAddress(eq(1L), eq(1L), any(AddressDto.class));
    }

    @Test
    void deleteAddress_ShouldDeleteAddress() throws Exception {
        doNothing().when(addressService).deleteAddress(1L, 1L);

        mockMvc.perform(delete("/api/users/1/addresses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Address deleted successfully"));

        verify(addressService).deleteAddress(1L, 1L);
    }

    @Test
    void setDefaultAddress_ShouldSetDefaultAddress() throws Exception {
        doNothing().when(addressService).setDefaultAddress(1L, 1L);

        mockMvc.perform(put("/api/users/1/addresses/1/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Default address set successfully"));

        verify(addressService).setDefaultAddress(1L, 1L);
    }
}