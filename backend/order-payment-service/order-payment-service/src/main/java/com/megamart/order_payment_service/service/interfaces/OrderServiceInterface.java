package com.megamart.order_payment_service.service.interfaces;

import com.megamart.order_payment_service.dto.OrderRequest;
import com.megamart.order_payment_service.dto.OrderResponse;
import com.megamart.order_payment_service.dto.AddressRequestDto;
import com.megamart.order_payment_service.dto.PaymentMethodRequestDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderServiceInterface {
    
    OrderResponse createOrder(OrderRequest request);
    
    OrderResponse getOrderById(Long orderId);
    
    Page<OrderResponse> getUserOrders(Long userId, Pageable pageable);
    
    Page<OrderResponse> getAllOrders(Pageable pageable);
    
    OrderResponse updateOrderStatus(Long orderId, String status);
    
    OrderResponse cancelOrder(Long orderId);
    
    OrderResponse createOrderFromCart(Long userId, AddressRequestDto address, PaymentMethodRequestDto paymentMethod);
} 