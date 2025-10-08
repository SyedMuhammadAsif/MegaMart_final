package com.megamart.useradminserver.service;

import com.megamart.useradminserver.dto.AddressDto;
import com.megamart.useradminserver.entity.UserAddress;

import java.util.List;

public interface AddressService {
    List<UserAddress> getUserAddresses(Long userId);
    UserAddress getAddressById(Long userId, Long addressId);
    UserAddress addAddress(Long userId, AddressDto addressDto);
    UserAddress updateAddress(Long userId, Long addressId, AddressDto addressDto);
    void deleteAddress(Long userId, Long addressId);
    void setDefaultAddress(Long userId, Long addressId);
}