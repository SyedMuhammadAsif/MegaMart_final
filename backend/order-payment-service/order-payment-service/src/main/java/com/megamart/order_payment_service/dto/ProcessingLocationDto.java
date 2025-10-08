package com.megamart.order_payment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.megamart.order_payment_service.entity.ProcessingLocation;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingLocationDto {
    private Long id;
    private String name;
    private String address;
    private String city;
    private String state;
    private String country;
    private Boolean active;

    public static ProcessingLocationDto fromEntity(ProcessingLocation location) {
        return new ProcessingLocationDto(
            location.getId(),
            location.getName(),
            location.getAddress(),
            location.getCity(),
            location.getState(),
            location.getCountry(),
            location.getActive()
        );
    }
}