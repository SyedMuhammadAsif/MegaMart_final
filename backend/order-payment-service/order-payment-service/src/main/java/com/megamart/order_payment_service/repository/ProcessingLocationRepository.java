package com.megamart.order_payment_service.repository;

import com.megamart.order_payment_service.entity.ProcessingLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessingLocationRepository extends JpaRepository<ProcessingLocation, Long> {
    
    List<ProcessingLocation> findByActiveTrue();
}