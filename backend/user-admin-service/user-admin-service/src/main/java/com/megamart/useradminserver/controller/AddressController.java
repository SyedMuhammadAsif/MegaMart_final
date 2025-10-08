package com.megamart.useradminserver.controller;

import com.megamart.useradminserver.dto.AddressDto;
import com.megamart.useradminserver.dto.MessageDto;
import com.megamart.useradminserver.dto.UserAddressResponseDto;
import com.megamart.useradminserver.entity.UserAddress;
import com.megamart.useradminserver.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/addresses")

@RequiredArgsConstructor
@Tag(name = "Address Management", description = "APIs for managing user addresses")
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    @Operation(summary = "Get user addresses", description = "Get all addresses for a specific user")
    public ResponseEntity<List<UserAddressResponseDto>> getUserAddresses(@PathVariable Long userId) {
        List<UserAddress> addresses = addressService.getUserAddresses(userId);
        List<UserAddressResponseDto> addressDtos = addresses.stream().map(UserAddressResponseDto::fromEntity).toList();
        return ResponseEntity.ok(addressDtos);
    }

    @GetMapping("/{addressId}")
    @Operation(summary = "Get address by ID", description = "Get a specific address by address ID for a user")
    public ResponseEntity<UserAddressResponseDto> getAddressById(@PathVariable Long userId, @PathVariable Long addressId) {
        UserAddress address = addressService.getAddressById(userId, addressId);
        return ResponseEntity.ok(UserAddressResponseDto.fromEntity(address));
    }

    @PostMapping
    @Operation(summary = "Add new address", description = "Add a new address for a user")
    public ResponseEntity<UserAddressResponseDto> addAddress(@PathVariable Long userId, @Valid @RequestBody AddressDto addressDto) {
        UserAddress address = addressService.addAddress(userId, addressDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserAddressResponseDto.fromEntity(address));
    }

    @PutMapping("/{addressId}")
    @Operation(summary = "Update address", description = "Update an existing address")
    public ResponseEntity<UserAddressResponseDto> updateAddress(@PathVariable Long userId, @PathVariable Long addressId, @Valid @RequestBody AddressDto addressDto) {
        UserAddress address = addressService.updateAddress(userId, addressId, addressDto);
        return ResponseEntity.ok(UserAddressResponseDto.fromEntity(address));
    }

    @DeleteMapping("/{addressId}")
    @Operation(summary = "Delete address", description = "Delete a user address")
    public ResponseEntity<MessageDto> deleteAddress(@PathVariable Long userId, @PathVariable Long addressId) {
        addressService.deleteAddress(userId, addressId);
        return ResponseEntity.ok(new MessageDto("Address deleted successfully"));
    }

    @PutMapping("/{addressId}/default")
    @Operation(summary = "Set default address", description = "Set an address as the default address for a user")
    public ResponseEntity<MessageDto> setDefaultAddress(@PathVariable Long userId, @PathVariable Long addressId) {
        addressService.setDefaultAddress(userId, addressId);
        return ResponseEntity.ok(new MessageDto("Default address set successfully"));
    }
}