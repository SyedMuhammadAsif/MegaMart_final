package com.megamart.order_payment_service.controller;

import com.megamart.order_payment_service.dto.*;
import com.megamart.order_payment_service.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Orders", description = "Order management operations")
public class OrderController {

    private final OrderService orderService;

    @Operation(summary = "Create new order", description = "Create a new order with items, address, and payment method")
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request) {
        log.info("Creating order for user: {}", request.getUserId());
        OrderResponse orderResponse = orderService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(orderResponse);
    }

    @Operation(summary = "Create order from cart", description = "Create order from user's cart items")
    @PostMapping("/from-cart/{userId}")
    public ResponseEntity<OrderResponse> createOrderFromCart(
            @PathVariable Long userId,
            @Valid @RequestBody OrderFromCartRequestDto request) {
        log.info("Creating order from cart for user: {}", userId);
        OrderResponse orderResponse = orderService.createOrderFromCart(userId, request.getAddress(), request.getPaymentMethod());
        return ResponseEntity.status(HttpStatus.CREATED).body(orderResponse);
    }

    @Operation(summary = "Get order by ID", description = "Retrieve order details by order ID")
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long id) {
        OrderResponse orderResponse = orderService.getOrderById(id);
        return ResponseEntity.ok(orderResponse);
    }

    @Operation(summary = "Get user orders", description = "Get all orders for a specific user with pagination")
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<OrderResponse>> getUserOrders(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<OrderResponse> orders = orderService.getUserOrders(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(orders);
    }

    @Operation(summary = "Get all orders", description = "Get all orders with pagination (Admin only)")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<OrderResponse> orders = orderService.getAllOrders(PageRequest.of(page, size));
        return ResponseEntity.ok(orders);
    }

    @Operation(
        summary = "Update order status",
        description = "Update order status following proper workflow: PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED"
    )
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @Parameter(
                name = "status",
                description = "New order status - must follow proper workflow",
                in = ParameterIn.QUERY,
                required = true,
                schema = @Schema(
                    type = "string",
                    allowableValues = {"PENDING", "CONFIRMED", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED"},
                    example = "PROCESSING"
                )
            )
            @RequestParam String status,
            @RequestParam(required = false) Long locationId,
            @RequestParam(required = false) String notes) {
        OrderResponse orderResponse = orderService.updateOrderStatus(id, status, locationId, notes);
        return ResponseEntity.ok(orderResponse);
    }

    @Operation(summary = "Cancel order", description = "Cancel an existing order (auto-refund if payment completed)")
    @PutMapping("/{id}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id) {
        OrderResponse orderResponse = orderService.cancelOrder(id);
        return ResponseEntity.ok(orderResponse);
    }

    @Operation(summary = "Delete order", description = "Delete an order (Admin only)")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<OrderDeleteResponseDto> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.ok(new OrderDeleteResponseDto("Order deleted successfully", id));
    }

    @Operation(summary = "Get order tracking", description = "Get order tracking information")
    @GetMapping("/{id}/tracking")
    public ResponseEntity<OrderTrackingResponseDto> getOrderTracking(@PathVariable Long id) {
        log.info("Fetching tracking for order: {}", id);
        List<com.megamart.order_payment_service.entity.OrderTracking> trackingHistory = orderService.getOrderTrackingHistory(id);
        return ResponseEntity.ok(new OrderTrackingResponseDto(id, trackingHistory));
    }

    @Operation(summary = "Health check", description = "Check if the service is running")
    @GetMapping("/health")
    public ResponseEntity<HealthResponseDto> health() {
        return ResponseEntity.ok(new HealthResponseDto("Order-Payment Service", "UP", "9098", System.currentTimeMillis()));
    }

    @Operation(summary = "Test orders endpoint", description = "Test if orders can be retrieved")
    @GetMapping("/debug/test")
    public ResponseEntity<HealthResponseDto> testOrders() {
        return ResponseEntity.ok(new HealthResponseDto("Order service is running", "SUCCESS", null, System.currentTimeMillis()));
    }
}