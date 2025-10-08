package com.megamart.order_payment_service.controller;

import com.megamart.order_payment_service.dto.ProcessingLocationDto;
import com.megamart.order_payment_service.entity.ProcessingLocation;
import com.megamart.order_payment_service.repository.ProcessingLocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/processing-locations")
@RequiredArgsConstructor
public class ProcessingLocationController {
    
    private final ProcessingLocationRepository locationRepository;
    
    @GetMapping
    public ResponseEntity<List<ProcessingLocationDto>> getAllLocations() {
        List<ProcessingLocation> locations = locationRepository.findByActiveTrue();
        List<ProcessingLocationDto> locationDtos = locations.stream().map(ProcessingLocationDto::fromEntity).toList();
        return ResponseEntity.ok(locationDtos);
    }
}