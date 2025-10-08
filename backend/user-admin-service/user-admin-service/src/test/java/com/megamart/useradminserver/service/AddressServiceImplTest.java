package com.megamart.useradminserver.service;

import com.megamart.useradminserver.dto.AddressDto;
import com.megamart.useradminserver.entity.User;
import com.megamart.useradminserver.entity.UserAddress;
import com.megamart.useradminserver.exception.ResourceNotFoundException;
import com.megamart.useradminserver.repository.UserAddressRepository;
import com.megamart.useradminserver.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock
    private UserAddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AddressServiceImpl addressService;

    private User testUser;
    private UserAddress testAddress;
    private AddressDto addressDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("John Doe");

        testAddress = new UserAddress();
        testAddress.setId(1L);
        testAddress.setUser(testUser);
        testAddress.setFullName("John Doe");
        testAddress.setAddressLine1("123 Main St");
        testAddress.setCity("New York");
        testAddress.setState("NY");
        testAddress.setPostalCode("10001");
        testAddress.setCountry("USA");
        testAddress.setPhone("1234567890");
        testAddress.setIsDefault(true);

        addressDto = new AddressDto();
        addressDto.setFullName("John Doe");
        addressDto.setAddressLine1("123 Main St");
        addressDto.setCity("New York");
        addressDto.setState("NY");
        addressDto.setPostalCode("10001");
        addressDto.setCountry("USA");
        addressDto.setPhone("1234567890");
        addressDto.setIsDefault(true);
    }

    @Test
    void getUserAddresses_ShouldReturnAddresses_WhenUserExists() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(addressRepository.findByUser_Id(1L)).thenReturn(Arrays.asList(testAddress));

        List<UserAddress> result = addressService.getUserAddresses(1L);

        assertEquals(1, result.size());
        assertEquals(testAddress, result.get(0));
        verify(userRepository).existsById(1L);
        verify(addressRepository).findByUser_Id(1L);
    }

    @Test
    void getUserAddresses_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> addressService.getUserAddresses(1L));
        verify(userRepository).existsById(1L);
        verify(addressRepository, never()).findByUser_Id(1L);
    }

    @Test
    void getAddressById_ShouldReturnAddress_WhenAddressExists() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(addressRepository.findById(1L)).thenReturn(Optional.of(testAddress));

        UserAddress result = addressService.getAddressById(1L, 1L);

        assertEquals(testAddress, result);
        verify(userRepository).existsById(1L);
        verify(addressRepository).findById(1L);
    }

    @Test
    void getAddressById_ShouldThrowException_WhenAddressNotFound() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(addressRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> addressService.getAddressById(1L, 1L));
        verify(addressRepository).findById(1L);
    }

    @Test
    void getAddressById_ShouldThrowException_WhenAddressNotBelongsToUser() {
        User otherUser = new User();
        otherUser.setId(2L);
        testAddress.setUser(otherUser);

        when(userRepository.existsById(1L)).thenReturn(true);
        when(addressRepository.findById(1L)).thenReturn(Optional.of(testAddress));

        assertThrows(ResourceNotFoundException.class, () -> addressService.getAddressById(1L, 1L));
        verify(addressRepository).findById(1L);
    }

    @Test
    void addAddress_ShouldCreateAddress_WhenValidData() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(addressRepository.save(any(UserAddress.class))).thenReturn(testAddress);

        UserAddress result = addressService.addAddress(1L, addressDto);

        assertEquals(testAddress, result);
        verify(userRepository).findById(1L);
        verify(addressRepository).save(any(UserAddress.class));
    }

    @Test
    void addAddress_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> addressService.addAddress(1L, addressDto));
        verify(userRepository).findById(1L);
        verify(addressRepository, never()).save(any(UserAddress.class));
    }

    @Test
    void updateAddress_ShouldUpdateAddress_WhenValidData() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(addressRepository.findById(1L)).thenReturn(Optional.of(testAddress));
        when(addressRepository.save(any(UserAddress.class))).thenReturn(testAddress);

        UserAddress result = addressService.updateAddress(1L, 1L, addressDto);

        assertEquals(testAddress, result);
        verify(userRepository).existsById(1L);
        verify(addressRepository).findById(1L);
        verify(addressRepository).save(testAddress);
    }

    @Test
    void updateAddress_ShouldThrowException_WhenAddressNotBelongsToUser() {
        User otherUser = new User();
        otherUser.setId(2L);
        testAddress.setUser(otherUser);

        when(userRepository.existsById(1L)).thenReturn(true);
        when(addressRepository.findById(1L)).thenReturn(Optional.of(testAddress));

        assertThrows(ResourceNotFoundException.class, () -> addressService.updateAddress(1L, 1L, addressDto));
        verify(addressRepository, never()).save(any(UserAddress.class));
    }

    @Test
    void deleteAddress_ShouldDeleteAddress_WhenAddressExists() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(addressRepository.findById(1L)).thenReturn(Optional.of(testAddress));

        addressService.deleteAddress(1L, 1L);

        verify(userRepository).existsById(1L);
        verify(addressRepository).findById(1L);
        verify(addressRepository).delete(testAddress);
    }

    @Test
    void deleteAddress_ShouldThrowException_WhenAddressNotFound() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(addressRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> addressService.deleteAddress(1L, 1L));
        verify(addressRepository, never()).delete(any(UserAddress.class));
    }

    @Test
    void setDefaultAddress_ShouldSetDefaultAddress_WhenAddressExists() {
        UserAddress otherAddress = new UserAddress();
        otherAddress.setId(2L);
        otherAddress.setUser(testUser);
        otherAddress.setIsDefault(true);

        when(userRepository.existsById(1L)).thenReturn(true);
        when(addressRepository.findById(1L)).thenReturn(Optional.of(testAddress));
        when(addressRepository.findByUser_Id(1L)).thenReturn(Arrays.asList(testAddress, otherAddress));
        when(addressRepository.saveAll(anyList())).thenReturn(Arrays.asList(testAddress, otherAddress));
        when(addressRepository.save(testAddress)).thenReturn(testAddress);

        addressService.setDefaultAddress(1L, 1L);

        verify(userRepository).existsById(1L);
        verify(addressRepository).findById(1L);
        verify(addressRepository).findByUser_Id(1L);
        verify(addressRepository).saveAll(anyList());
        verify(addressRepository).save(testAddress);
        assertTrue(testAddress.getIsDefault());
    }
}