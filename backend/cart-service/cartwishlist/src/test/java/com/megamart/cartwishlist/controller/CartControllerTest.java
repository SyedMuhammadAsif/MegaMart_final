package com.megamart.cartwishlist.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.megamart.cartwishlist.dto.*;
import com.megamart.cartwishlist.service.CartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CartService cartService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getCart_ShouldReturnCart() throws Exception {
        CartResponseDto response = CartResponseDto.builder()
                .id(1L)
                .userId(1L)
                .total(BigDecimal.valueOf(20.0))
                .items(Collections.emptyList())
                .totalItems(0)
                .totalPrice(BigDecimal.ZERO)
                .build();

        when(cartService.getCart(1L)).thenReturn(response);

        mockMvc.perform(get("/cart/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.total").value(20.0));

        verify(cartService).getCart(1L);
    }

    @Test
    void addItem_ShouldAddItemToCart() throws Exception {
        AddItemRequestDto request = new AddItemRequestDto();
        request.setProductId(1L);
        request.setQuantity(2);

        CartItemResponseDto item = CartItemResponseDto.builder()
                .id(1L)
                .productId(1L)
                .quantity(2)
                .lineTotal(BigDecimal.valueOf(20.0))
                .build();

        CartResponseDto response = CartResponseDto.builder()
                .id(1L)
                .userId(1L)
                .total(BigDecimal.valueOf(20.0))
                .items(Arrays.asList(item))
                .totalItems(2)
                .totalPrice(BigDecimal.valueOf(20.0))
                .build();

        when(cartService.addItem(eq(1L), any(AddItemRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/cart/1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(2))
                .andExpect(jsonPath("$.items[0].productId").value(1))
                .andExpect(jsonPath("$.items[0].quantity").value(2));

        verify(cartService).addItem(eq(1L), any(AddItemRequestDto.class));
    }

    @Test
    void updateQuantity_ShouldUpdateItemQuantity() throws Exception {
        UpdateQuantityRequestDto request = new UpdateQuantityRequestDto();
        request.setQuantity(3);

        CartItemResponseDto item = CartItemResponseDto.builder()
                .id(1L)
                .productId(1L)
                .quantity(3)
                .lineTotal(BigDecimal.valueOf(30.0))
                .build();

        CartResponseDto response = CartResponseDto.builder()
                .id(1L)
                .userId(1L)
                .total(BigDecimal.valueOf(30.0))
                .items(Arrays.asList(item))
                .totalItems(3)
                .totalPrice(BigDecimal.valueOf(30.0))
                .build();

        when(cartService.updateQuantity(1L, 1L, 3)).thenReturn(response);

        mockMvc.perform(patch("/cart/1/items/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.total").value(30.0));

        verify(cartService).updateQuantity(1L, 1L, 3);
    }

    @Test
    void removeItem_ShouldRemoveItemFromCart() throws Exception {
        CartResponseDto response = CartResponseDto.builder()
                .id(1L)
                .userId(1L)
                .total(BigDecimal.ZERO)
                .items(Collections.emptyList())
                .totalItems(0)
                .totalPrice(BigDecimal.ZERO)
                .build();

        when(cartService.removeItem(1L, 1L)).thenReturn(response);

        mockMvc.perform(delete("/cart/1/items/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.items").isEmpty());

        verify(cartService).removeItem(1L, 1L);
    }

    @Test
    void clearCart_ShouldClearAllItems() throws Exception {
        doNothing().when(cartService).clearCart(1L);

        mockMvc.perform(delete("/cart/1"))
                .andExpect(status().isNoContent());

        verify(cartService).clearCart(1L);
    }

    @Test
    void health_ShouldReturnHealthStatus() throws Exception {
        mockMvc.perform(get("/cart/health"))
                .andExpect(status().isOk())
                .andExpect(content().string("Cart Service is running on port 8087"));
    }

    @Test
    void addItem_WithInvalidData_ShouldReturnBadRequest() throws Exception {
        AddItemRequestDto request = new AddItemRequestDto();
        request.setQuantity(-1);

        mockMvc.perform(post("/cart/1/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(cartService, never()).addItem(anyLong(), any(AddItemRequestDto.class));
    }

    @Test
    void updateQuantity_WithInvalidItemId_ShouldReturnBadRequest() throws Exception {
        UpdateQuantityRequestDto request = new UpdateQuantityRequestDto();
        request.setQuantity(2);

        mockMvc.perform(patch("/cart/1/items/0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(cartService, never()).updateQuantity(anyLong(), anyLong(), anyInt());
    }

    @Test
    void removeItem_WithInvalidItemId_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(delete("/cart/1/items/0"))
                .andExpect(status().isBadRequest());

        verify(cartService, never()).removeItem(anyLong(), anyLong());
    }
}