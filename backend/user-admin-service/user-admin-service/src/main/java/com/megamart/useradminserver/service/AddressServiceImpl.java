package com.megamart.useradminserver.service;

import com.megamart.useradminserver.dto.AddressDto;
import com.megamart.useradminserver.entity.User;
import com.megamart.useradminserver.entity.UserAddress;
import com.megamart.useradminserver.exception.ResourceNotFoundException;
import com.megamart.useradminserver.repository.UserAddressRepository;
import com.megamart.useradminserver.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AddressServiceImpl implements AddressService {
    
    private static final String ADDRESS_NOT_FOUND_MESSAGE = "Address not found with id: ";
    
    private final UserAddressRepository addressRepository;
    private final UserRepository userRepository;

    @Override
    public List<UserAddress> getUserAddresses(Long userId) {
        validateUserExists(userId);
        return addressRepository.findByUser_Id(userId);
    }

    @Override
    public UserAddress getAddressById(Long userId, Long addressId) {
        validateUserExists(userId);
        
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException(ADDRESS_NOT_FOUND_MESSAGE + addressId));
        
        if (!address.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Address not found for user: " + userId);
        }
        
        return address;
    }

    @Override
    public UserAddress addAddress(Long userId, AddressDto addressDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        
        UserAddress address = new UserAddress();
        address.setUser(user);
        mapDtoToEntity(addressDto, address);
        
        return addressRepository.save(address);
    }

    @Override
    public UserAddress updateAddress(Long userId, Long addressId, AddressDto addressDto) {
        validateUserExists(userId);
        
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException(ADDRESS_NOT_FOUND_MESSAGE + addressId));
        
        if (!address.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Address not found for user: " + userId);
        }
        
        mapDtoToEntity(addressDto, address);
        return addressRepository.save(address);
    }

    @Override
    public void deleteAddress(Long userId, Long addressId) {
        validateUserExists(userId);
        
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException(ADDRESS_NOT_FOUND_MESSAGE + addressId));
        
        if (!address.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Address not found for user: " + userId);
        }
        
        addressRepository.delete(address);
    }

    @Override
    public void setDefaultAddress(Long userId, Long addressId) {
        validateUserExists(userId);
        
        UserAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException(ADDRESS_NOT_FOUND_MESSAGE + addressId));
        
        if (!address.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Address not found for user: " + userId);
        }
        
        // Reset all addresses to non-default
        List<UserAddress> userAddresses = addressRepository.findByUser_Id(userId);
        userAddresses.forEach(addr -> addr.setIsDefault(false));
        addressRepository.saveAll(userAddresses);
        
        // Set the selected address as default
        address.setIsDefault(true);
        addressRepository.save(address);
    }

    private void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }
    }

    private void mapDtoToEntity(AddressDto dto, UserAddress entity) {
        entity.setFullName(dto.getFullName());
        entity.setAddressLine1(dto.getAddressLine1());
        entity.setAddressLine2(dto.getAddressLine2());
        entity.setCity(dto.getCity());
        entity.setState(dto.getState());
        entity.setPostalCode(dto.getPostalCode());
        entity.setCountry(dto.getCountry());
        entity.setPhone(dto.getPhone());
        entity.setIsDefault(dto.getIsDefault());
    }
}