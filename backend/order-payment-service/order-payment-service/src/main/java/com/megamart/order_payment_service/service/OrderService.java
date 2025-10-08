package com.megamart.order_payment_service.service;

import com.megamart.order_payment_service.client.UserAdminClient;
import com.megamart.order_payment_service.dto.*;
import com.megamart.order_payment_service.entity.Order;
import com.megamart.order_payment_service.entity.OrderItem;
import com.megamart.order_payment_service.entity.Payment;
import com.megamart.order_payment_service.exception.InvalidOrderStatusException;
import com.megamart.order_payment_service.exception.InvalidRequestException;
import com.megamart.order_payment_service.exception.OrderNotFoundException;
import com.megamart.order_payment_service.exception.ServiceException;
import com.megamart.order_payment_service.repository.OrderRepository;
import com.megamart.order_payment_service.repository.OrderTrackingRepository;
import com.megamart.order_payment_service.entity.OrderTracking;
import com.megamart.order_payment_service.service.interfaces.OrderServiceInterface;
import com.megamart.order_payment_service.service.interfaces.UserDataServiceInterface;
import com.megamart.order_payment_service.client.CartServiceClient;
import com.megamart.order_payment_service.client.ProductServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService implements OrderServiceInterface {
    
    private static final String PHONE = "phone";
    
    private final OrderRepository orderRepository;
    private final OrderTrackingRepository trackingRepository;
    private final UserDataServiceInterface userDataService;

    private final MappingService mappingService;
    private final CartServiceClient cartServiceClient;
    private final ProductServiceClient productServiceClient;
    private final UserAdminClient userAdminClient;


    @Override
    public OrderResponse createOrder(OrderRequest request) {
        log.info("Creating order for user: {}", request.getUserId());
        
        checkIfRequestIsValid(request);
        
        AddressDto shippingAddress = getOrCreateAddress(request);
        PaymentMethodDto paymentMethod = getOrCreatePaymentMethod(request);
        
        Order order = createNewOrder(request, shippingAddress.getId());

        order.setShippingFullName(shippingAddress.getFullName());
        order.setShippingAddressLine1(shippingAddress.getAddressLine1());
        order.setShippingAddressLine2(shippingAddress.getAddressLine2());
        order.setShippingCity(shippingAddress.getCity());
        order.setShippingState(shippingAddress.getState());
        order.setShippingPostalCode(shippingAddress.getPostalCode());
        order.setShippingCountry(shippingAddress.getCountry());
        order.setShippingPhone(shippingAddress.getPhone());
        
        addItemsToOrder(order, request.getItems());
        createPaymentForOrder(order, request.getTotal(), paymentMethod.getId());
        
        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully with ID: {}", savedOrder.getId());
        
        return mappingService.mapToOrderResponse(savedOrder, shippingAddress, paymentMethod);
    }
    
    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        AddressDto address = getAddressDetails(order.getUserId(), order.getShippingAddressId());
        PaymentMethodDto paymentMethod = getPaymentMethodDetails(order);
        
        OrderResponse response = mappingService.mapToOrderResponse(order, address, paymentMethod);
        
        try {
            Map<String, Object> userDetails = userAdminClient.getUserById(order.getUserId());
            if (userDetails != null) {
                response.setCustomerName((String) userDetails.get("name"));
                response.setCustomerEmail((String) userDetails.get("email"));
                response.setCustomerPhone((String) userDetails.get(PHONE));
                log.info("Fetched user details: name={}, email={}, phone={}", 
                    userDetails.get("name"), userDetails.get("email"), userDetails.get(PHONE));
            }
        } catch (Exception e) {
            log.warn("Failed to fetch user details for userId: {}", order.getUserId(), e);
        }
        
        return response;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(Long userId, Pageable pageable) {
        Page<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return orders.map(this::convertOrderWithDetails);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        try {
            Page<Order> orders = orderRepository.findAll(pageable);
            return orders.map(order -> {
                OrderResponse response = new OrderResponse();
                response.setId(order.getId());
                response.setUserId(order.getUserId());
                response.setTotal(order.getTotal());
                response.setOrderStatus(order.getOrderStatus().toString());
                response.setOrderDate(order.getOrderDate());
                response.setPaymentType(order.getPaymentType().toString());
                response.setPaymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().toString() : "COMPLETED");
                
                // Include shipping address from stored fields
                if (order.getShippingFullName() != null) {
                    AddressDto shippingAddress = AddressDto.builder()
                        .fullName(order.getShippingFullName())
                        .addressLine1(order.getShippingAddressLine1())
                        .addressLine2(order.getShippingAddressLine2())
                        .city(order.getShippingCity())
                        .state(order.getShippingState())
                        .postalCode(order.getShippingPostalCode())
                        .country(order.getShippingCountry())
                        .phone(order.getShippingPhone())
                        .build();
                    response.setShippingAddress(shippingAddress);
                }
                
                return response;
            });
        } catch (Exception e) {
            log.error("Error getting all orders: {}", e.getMessage());
            throw new ServiceException("Failed to retrieve orders");
        }
    }
    
    @Override
    public OrderResponse updateOrderStatus(Long orderId, String status) {
        return updateOrderStatus(orderId, status, null, null);
    }
    
    public OrderResponse updateOrderStatus(Long orderId, String status, Long locationId, String notes) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        updateStatus(order, status);
        Order savedOrder = orderRepository.save(order);
        
        if (locationId != null || notes != null) {
            addTrackingEntry(orderId, status, locationId, notes);
        }
        
        AddressDto address = getAddressDetails(order.getUserId(), order.getShippingAddressId());
        PaymentMethodDto paymentMethod = getPaymentMethodDetails(order);
        
        return mappingService.mapToOrderResponse(savedOrder, address, paymentMethod);
    }
    
    private void addTrackingEntry(Long orderId, String status, Long locationId, String notes) {
        OrderTracking tracking = OrderTracking.builder()
            .orderId(orderId)
            .status(status)
            .location(locationId != null ? "Location ID: " + locationId : null)
            .description("Status updated to " + status)
            .processingNotes(notes)
            .updatedBy("Admin")
            .build();
        
        trackingRepository.save(tracking);
        log.info("Added tracking entry for order {}: status={}, location={}, notes={}", 
                orderId, status, locationId, notes);
    }
    
    @Override
    public OrderResponse cancelOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        checkIfCanCancel(order);
        
        restoreStockForOrder(order);
        
        order.setOrderStatus(Order.OrderStatus.CANCELLED);
        
        if (order.getPayment() != null &&
            order.getPayment().getPaymentStatus() == Payment.PaymentStatus.COMPLETED) {
            order.getPayment().setPaymentStatus(Payment.PaymentStatus.REFUNDED);
            order.setPaymentStatus(Order.PaymentStatus.REFUNDED);
        }
        
        Order savedOrder = orderRepository.save(order);
        
        AddressDto address = getAddressDetails(order.getUserId(), order.getShippingAddressId());
        PaymentMethodDto paymentMethod = getPaymentMethodDetails(order);
        
        return mappingService.mapToOrderResponse(savedOrder, address, paymentMethod);
    }
    
    private void checkIfRequestIsValid(OrderRequest request) {
        if (request.getAddressId() == null && request.getNewAddress() == null) {
            throw new InvalidRequestException("address", "Either addressId or newAddress must be provided");
        }
        
        if (request.getPaymentMethodId() == null && request.getNewPaymentMethod() == null) {
            throw new InvalidRequestException("paymentMethod", "Either paymentMethodId or newPaymentMethod must be provided");
        }
    }
    
    private AddressDto getOrCreateAddress(OrderRequest request) {
        if (request.getAddressId() != null) {
            return getAddressDetails(request.getUserId(), request.getAddressId());
        } else {
            return userDataService.createAddress(request.getUserId(), request.getNewAddress());
        }
    }
    
    private PaymentMethodDto getOrCreatePaymentMethod(OrderRequest request) {
        if (request.getPaymentMethodId() != null) {
            return getPaymentMethodDetails(request.getUserId(), request.getPaymentMethodId());
        } else {
            return userDataService.createPaymentMethod(request.getUserId(), request.getNewPaymentMethod());
        }
    }
    
    private Order createNewOrder(OrderRequest request, Long addressId) {
        Order.PaymentType paymentType = Order.PaymentType.valueOf(request.getPaymentType().toUpperCase());
        Order.PaymentStatus paymentStatus = (paymentType == Order.PaymentType.COD) 
            ? Order.PaymentStatus.PENDING 
            : Order.PaymentStatus.COMPLETED;
        
        log.info("Creating order with payment type: {} and payment status: {}", paymentType, paymentStatus);
        
        return Order.builder()
                .userId(request.getUserId())
                .total(request.getTotal())
                .paymentType(paymentType)
                .paymentStatus(paymentStatus)
                .orderDate(LocalDateTime.now())
                .shippingAddressId(addressId)
                .build();
    }
    
    private void addItemsToOrder(Order order, List<OrderItemRequestDto> items) {
        List<OrderItem> orderItems = items.stream()
                .map(itemRequest -> OrderItem.builder()
                        .order(order)
                        .productId(itemRequest.getProductId())
                        .quantity(itemRequest.getQuantity())
                        .lineTotal(itemRequest.getLineTotal())
                        .build())
                .toList();
        
        order.setOrderItems(orderItems);
    }
    
    private void createPaymentForOrder(Order order, BigDecimal amount, Long paymentMethodId) {
        // Set payment status based on payment type
        Payment.PaymentStatus paymentStatus = order.getPaymentType() == Order.PaymentType.COD 
            ? Payment.PaymentStatus.PENDING 
            : Payment.PaymentStatus.COMPLETED;
            
        Order.PaymentStatus orderPaymentStatus = order.getPaymentType() == Order.PaymentType.COD 
            ? Order.PaymentStatus.PENDING 
            : Order.PaymentStatus.COMPLETED;
        
        Payment payment = Payment.builder()
                .order(order)
                .userId(order.getUserId())
                .amount(amount)
                .paymentMethodId(paymentMethodId)
                .paymentStatus(paymentStatus)
                .build();
        
        order.setPayment(payment);
        order.setPaymentStatus(orderPaymentStatus);
    }
    
    private void updateStatus(Order order, String status) {
        try {
            Order.OrderStatus newStatus = Order.OrderStatus.valueOf(status.toUpperCase());
            validateStatusTransition(order.getOrderStatus(), newStatus);
            order.setOrderStatus(newStatus);
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("status", "Invalid order status: " + status + ". Valid statuses: PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED");
        }
    }
    
    private void validateStatusTransition(Order.OrderStatus currentStatus, Order.OrderStatus newStatus) {
        boolean isValidTransition = switch (currentStatus) {
            case PENDING -> newStatus == Order.OrderStatus.CONFIRMED || newStatus == Order.OrderStatus.CANCELLED;
            case CONFIRMED -> newStatus == Order.OrderStatus.PROCESSING || newStatus == Order.OrderStatus.CANCELLED;
            case PROCESSING -> newStatus == Order.OrderStatus.SHIPPED || newStatus == Order.OrderStatus.CANCELLED;
            case SHIPPED -> newStatus == Order.OrderStatus.DELIVERED;
            case DELIVERED -> false;
            case CANCELLED -> false;
        };
        
        if (!isValidTransition) {
            String allowedStatuses = getAllowedStatusesForCurrent(currentStatus);
            throw new InvalidOrderStatusException(
                String.format("Cannot change status from %s to %s. Allowed transitions: %s", 
                    currentStatus, newStatus, allowedStatuses)
            );
        }
    }
    
    private String getAllowedStatusesForCurrent(Order.OrderStatus currentStatus) {
        return switch (currentStatus) {
            case PENDING -> "CONFIRMED, CANCELLED";
            case CONFIRMED -> "PROCESSING, CANCELLED";
            case PROCESSING -> "SHIPPED, CANCELLED";
            case SHIPPED -> "DELIVERED";
            case DELIVERED -> "None (Final state)";
            case CANCELLED -> "None (Final state)";
        };
    }
    
    private void checkIfCanCancel(Order order) {
        if (order.getOrderStatus() == Order.OrderStatus.DELIVERED) {
            throw new InvalidOrderStatusException("Cannot cancel delivered order");
        }
        
        if (order.getOrderStatus() == Order.OrderStatus.SHIPPED) {
            throw new InvalidOrderStatusException("Cannot cancel shipped order. Order is already in transit.");
        }
        
        if (order.getOrderStatus() == Order.OrderStatus.CANCELLED) {
            throw new InvalidOrderStatusException("Order is already cancelled");
        }
        
        if (order.getOrderStatus() != Order.OrderStatus.PENDING && 
            order.getOrderStatus() != Order.OrderStatus.CONFIRMED && 
            order.getOrderStatus() != Order.OrderStatus.PROCESSING) {
            throw new InvalidOrderStatusException("Cannot cancel order in " + order.getOrderStatus() + " status");
        }
    }
    
    private AddressDto getAddressDetails(Long userId, Long addressId) {
        try {
            if (addressId != null) {
                return userDataService.getAddress(userId, addressId);
            }
            return null;
        } catch (Exception e) {
            log.error("Error getting address for userId: {} addressId: {}", userId, addressId, e);
            return null;
        }
    }
    
    private PaymentMethodDto getPaymentMethodDetails(Long userId, Long paymentMethodId) {
        try {
            return userDataService.getPaymentMethod(userId, paymentMethodId);
        } catch (Exception e) {
            log.error("Error getting payment method: {}", e.getMessage());
            return null;
        }
    }
    
    private PaymentMethodDto getPaymentMethodDetails(Order order) {
        if (order.getPayment() != null && order.getPayment().getPaymentMethodId() != null) {
            return getPaymentMethodDetails(order.getUserId(), order.getPayment().getPaymentMethodId());
        }
        return null;
    }
    
    private OrderResponse convertOrderWithDetails(Order order) {
        AddressDto address = getAddressDetails(order.getUserId(), order.getShippingAddressId());
        PaymentMethodDto paymentMethod = getPaymentMethodDetails(order);
        return mappingService.mapToOrderResponse(order, address, paymentMethod);
    }
    
    @Override
    public OrderResponse createOrderFromCart(Long userId, AddressRequestDto address, PaymentMethodRequestDto paymentMethod) {
        log.info("Creating order from cart for user: {}", userId);
        
        com.megamart.order_payment_service.dto.CartResponseDto cart;
        try {
            cart = cartServiceClient.getCart(userId.toString());
        } catch (Exception e) {
            log.error("Error getting cart for user {}: {}", userId, e.getMessage());
            throw new InvalidRequestException("cart", "Unable to retrieve cart for user: " + userId);
        }
        
        if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new InvalidRequestException("cart", "Cart is empty");
        }
        
        for (com.megamart.order_payment_service.dto.CartItemDto item : cart.getItems()) {
            validateProductStock(item.getProductId(), item.getQuantity());
        }
        
        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setUserId(userId);
        orderRequest.setTotal(BigDecimal.valueOf(cart.getTotalPrice()));
        orderRequest.setPaymentType(paymentMethod.getType());
        orderRequest.setNewAddress(address);
        orderRequest.setNewPaymentMethod(paymentMethod);
        
        List<OrderItemRequestDto> orderItems = cart.getItems().stream()
            .map(cartItem -> {
                OrderItemRequestDto orderItem = new OrderItemRequestDto();
                orderItem.setProductId(cartItem.getProductId());
                orderItem.setQuantity(cartItem.getQuantity());
                orderItem.setLineTotal(BigDecimal.valueOf(cartItem.getLineTotal()));
                return orderItem;
            })
            .toList();
        orderRequest.setItems(orderItems);
        
        OrderResponse orderResponse = createOrder(orderRequest);
        
        for (com.megamart.order_payment_service.dto.CartItemDto item : cart.getItems()) {
            reduceProductStock(item.getProductId(), item.getQuantity());
        }
        
        try {
            cartServiceClient.clearCart(userId.toString());
        } catch (Exception e) {
            log.warn("Failed to clear cart for user {}: {}", userId, e.getMessage());
        }
        
        return orderResponse;
    }
    
    private void validateProductStock(Long productId, Integer quantity) {
        try {
            com.megamart.order_payment_service.dto.ProductResponseDto product = productServiceClient.getProductById(productId);
            if (product == null || !product.isSuccess() || product.getData() == null) {
                throw new InvalidRequestException("product", "Product not found: " + productId);
            }
            
            if (product.getData().getStock() < quantity) {
                throw new InvalidRequestException("stock", 
                    "Insufficient stock for product " + productId + ". Available: " + 
                    product.getData().getStock() + ", Required: " + quantity);
            }
        } catch (Exception e) {
            log.error("Error validating product stock: {}", e.getMessage());
            throw new InvalidRequestException("product", "Error validating product: " + productId);
        }
    }
    
    private void reduceProductStock(Long productId, Integer quantity) {
        try {
            Map<String, Integer> request = Map.of("stockChange", -quantity);
            productServiceClient.updateStock(productId, request);
            log.info("Reduced stock for product {} by {}", productId, quantity);
        } catch (Exception e) {
            log.error("Error reducing stock for product {}: {}", productId, e.getMessage());
        }
    }
    
    private void restoreStockForOrder(Order order) {
        if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
            for (OrderItem item : order.getOrderItems()) {
                restoreProductStock(item.getProductId(), item.getQuantity());
            }
        }
    }
    
    private void restoreProductStock(Long productId, Integer quantity) {
        try {
            Map<String, Integer> request = Map.of("stockChange", quantity);
            productServiceClient.updateStock(productId, request);
            log.info("Restored stock for product {} by {}", productId, quantity);
        } catch (Exception e) {
            log.error("Error restoring stock for product {}: {}", productId, e.getMessage());
        }
    }
    
    public List<OrderTracking> getOrderTrackingHistory(Long orderId) {
        return trackingRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
    }
    
    public void deleteOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        
        if (order.getOrderStatus() != Order.OrderStatus.DELIVERED) {
            restoreStockForOrder(order);
        }
        
        List<OrderTracking> trackingHistory = trackingRepository.findByOrderIdOrderByCreatedAtAsc(orderId);
        trackingRepository.deleteAll(trackingHistory);
        
        orderRepository.delete(order);
        log.info("Order {} deleted successfully", orderId);
    }
    
    public List<Map<String, Object>> getSimpleOrdersList() {
        try {
            List<Order> orders = orderRepository.findAll();
            return orders.stream().map(order -> {
                Map<String, Object> orderMap = new HashMap<>();
                orderMap.put("id", order.getId());
                orderMap.put("userId", order.getUserId());
                orderMap.put("total", order.getTotal());
                orderMap.put("orderStatus", order.getOrderStatus().toString());
                orderMap.put("paymentType", order.getPaymentType().toString());
                orderMap.put("paymentStatus", order.getPaymentStatus().toString());
                orderMap.put("orderDate", order.getOrderDate());
                orderMap.put("shippingAddressId", order.getShippingAddressId());
                
                Map<String, Object> shippingAddress = new HashMap<>();
                shippingAddress.put("fullName", order.getShippingFullName());
                shippingAddress.put("addressLine1", order.getShippingAddressLine1());
                shippingAddress.put("addressLine2", order.getShippingAddressLine2());
                shippingAddress.put("city", order.getShippingCity());
                shippingAddress.put("state", order.getShippingState());
                shippingAddress.put("postalCode", order.getShippingPostalCode());
                shippingAddress.put("country", order.getShippingCountry());
                shippingAddress.put(PHONE, order.getShippingPhone());
                orderMap.put("shippingAddress", shippingAddress);
                
                return orderMap;
            }).toList();
        } catch (Exception e) {
            log.error("Error getting simple orders: {}", e.getMessage());
            return new java.util.ArrayList<>();
        }
    }
} 