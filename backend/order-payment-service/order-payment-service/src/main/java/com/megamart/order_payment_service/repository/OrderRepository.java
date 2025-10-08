package com.megamart.order_payment_service.repository;

import com.megamart.order_payment_service.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems LEFT JOIN FETCH o.payment WHERE o.id = :id")
    Optional<Order> findByIdWithDetails(@Param("id") Long id);
    
    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.orderItems LEFT JOIN FETCH o.payment WHERE o.userId = :userId ORDER BY o.createdAt DESC")
    List<Order> findByUserIdWithDetails(@Param("userId") Long userId);
    
    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);
} 