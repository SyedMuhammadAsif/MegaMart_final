package com.megamart.cartwishlist.service;

import com.megamart.cartwishlist.client.ProductServiceClient;
import com.megamart.cartwishlist.dto.*;
import com.megamart.cartwishlist.model.Cart;
import com.megamart.cartwishlist.model.CartItem;
import com.megamart.cartwishlist.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductServiceClient productServiceClient;

    @InjectMocks
    private CartService cartService;

    private Cart cart;
    private CartItem cartItem;
    private ProductDto productDto;

    @BeforeEach
    void setUp() {
        cart = Cart.builder()
                .id(1L)
                .userId(1L)
                .total(BigDecimal.ZERO)
                .items(new ArrayList<>())
                .build();

        cartItem = CartItem.builder()
                .id(1L)
                .cart(cart)
                .productId(1L)
                .quantity(2)
                .lineTotal(BigDecimal.valueOf(20.0))
                .build();

        productDto = ProductDto.builder()
                .productId(1L)
                .name("Test Product")
                .price(BigDecimal.valueOf(10.0))
                .stock(100)
                .build();
    }

    @Test
    void getCart_WhenCartExists_ShouldReturnCart() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponseDto result = cartService.getCart(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1L, result.getUserId());
        verify(cartRepository).findByUserId(1L);
        verify(cartRepository).save(cart);
    }

    @Test
    void getCart_WhenCartDoesNotExist_ShouldCreateNewCart() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponseDto result = cartService.getCart(1L);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        verify(cartRepository).findByUserId(1L);
        verify(cartRepository, times(2)).save(any(Cart.class));
    }

    @Test
    void addItem_WhenNewItem_ShouldAddToCart() {
        AddItemRequestDto request = new AddItemRequestDto();
        request.setProductId(1L);
        request.setQuantity(2);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productServiceClient.getProduct(1L)).thenReturn(Optional.of(productDto));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponseDto result = cartService.addItem(1L, request);

        assertNotNull(result);
        assertEquals(1, cart.getItems().size());
        assertEquals(2, cart.getItems().get(0).getQuantity());
        verify(cartRepository).save(cart);
    }

    @Test
    void addItem_WhenItemExists_ShouldUpdateQuantity() {
        AddItemRequestDto request = new AddItemRequestDto();
        request.setProductId(1L);
        request.setQuantity(3);

        cart.getItems().add(cartItem);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productServiceClient.getProduct(1L)).thenReturn(Optional.of(productDto));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponseDto result = cartService.addItem(1L, request);

        assertNotNull(result);
        assertEquals(1, cart.getItems().size());
        assertEquals(5, cart.getItems().get(0).getQuantity());
        verify(cartRepository).save(cart);
    }

    @Test
    void addItem_WhenProductServiceFails_ShouldUseDefaultPrice() {
        AddItemRequestDto request = new AddItemRequestDto();
        request.setProductId(1L);
        request.setQuantity(2);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productServiceClient.getProduct(1L)).thenThrow(new RuntimeException("Service unavailable"));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponseDto result = cartService.addItem(1L, request);

        assertNotNull(result);
        assertEquals(1, cart.getItems().size());
        assertEquals(BigDecimal.valueOf(20.0), cart.getItems().get(0).getLineTotal());
        verify(cartRepository).save(cart);
    }

    @Test
    void updateQuantity_WhenItemExists_ShouldUpdateQuantity() {
        cart.getItems().add(cartItem);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(productServiceClient.getProduct(1L)).thenReturn(Optional.of(productDto));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponseDto result = cartService.updateQuantity(1L, 1L, 5);

        assertNotNull(result);
        assertEquals(5, cartItem.getQuantity());
        assertEquals(BigDecimal.valueOf(50.0), cartItem.getLineTotal());
        verify(cartRepository).save(cart);
    }

    @Test
    void updateQuantity_WhenItemNotFound_ShouldThrowException() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        assertThrows(IllegalArgumentException.class, () -> {
            cartService.updateQuantity(1L, 999L, 5);
        });

        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void updateQuantity_WhenQuantityInvalid_ShouldThrowException() {
        cart.getItems().add(cartItem);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        assertThrows(IllegalArgumentException.class, () -> {
            cartService.updateQuantity(1L, 1L, 0);
        });

        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void removeItem_WhenItemExists_ShouldRemoveItem() {
        cart.getItems().add(cartItem);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        CartResponseDto result = cartService.removeItem(1L, 1L);

        assertNotNull(result);
        assertTrue(cart.getItems().isEmpty());
        verify(cartRepository).save(cart);
    }

    @Test
    void clearCart_ShouldRemoveAllItems() {
        cart.getItems().add(cartItem);

        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        cartService.clearCart(1L);

        assertTrue(cart.getItems().isEmpty());
        assertEquals(BigDecimal.ZERO, cart.getTotal());
        verify(cartRepository).save(cart);
    }

    @Test
    void getOrCreateCart_WhenCartExists_ShouldReturnExistingCart() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.of(cart));

        Cart result = cartService.getOrCreateCart(1L);

        assertEquals(cart, result);
        verify(cartRepository).findByUserId(1L);
        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void getOrCreateCart_WhenCartDoesNotExist_ShouldCreateNewCart() {
        when(cartRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenReturn(cart);

        Cart result = cartService.getOrCreateCart(1L);

        assertNotNull(result);
        verify(cartRepository).findByUserId(1L);
        verify(cartRepository).save(any(Cart.class));
    }
}