package com.megamart.order_payment_service.repository;

import com.megamart.order_payment_service.entity.OrderTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderTrackingRepository extends JpaRepository<OrderTracking, Long> {
    
    List<OrderTracking> findByOrderIdOrderByCreatedAtAsc(Long orderId);
}