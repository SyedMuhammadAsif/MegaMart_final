package com.megamart.order_payment_service.service;

import com.megamart.order_payment_service.entity.ProcessingLocation;
import com.megamart.order_payment_service.repository.ProcessingLocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataInitializationService implements CommandLineRunner {
    
    private final ProcessingLocationRepository locationRepository;
    
    @Override
    public void run(String... args) throws Exception {
        initializeProcessingLocations();
    }
    
    private void initializeProcessingLocations() {
        if (locationRepository.count() == 0) {
            log.info("Initializing processing locations...");
            
            ProcessingLocation warehouse1 = ProcessingLocation.builder()
                .name("Main Warehouse")
                .city("New York")
                .state("NY")
                .country("USA")
                .address("123 Warehouse St")
                .active(true)
                .build();
                
            ProcessingLocation warehouse2 = ProcessingLocation.builder()
                .name("West Coast Center")
                .city("Los Angeles")
                .state("CA")
                .country("USA")
                .address("456 Processing Ave")
                .active(true)
                .build();
                
            ProcessingLocation warehouse3 = ProcessingLocation.builder()
                .name("Central Hub")
                .city("Chicago")
                .state("IL")
                .country("USA")
                .address("789 Distribution Blvd")
                .active(true)
                .build();
            
            locationRepository.save(warehouse1);
            locationRepository.save(warehouse2);
            locationRepository.save(warehouse3);
            
            log.info("Processing locations initialized successfully");
        }
    }
}