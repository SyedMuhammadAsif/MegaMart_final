package com.megamart.useradminserver.service;

import com.megamart.useradminserver.dto.PaymentMethodDto;
import com.megamart.useradminserver.entity.User;
import com.megamart.useradminserver.entity.UserPaymentMethod;
import com.megamart.useradminserver.exception.ResourceNotFoundException;
import com.megamart.useradminserver.repository.UserPaymentMethodRepository;
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
class PaymentMethodServiceImplTest {

    @Mock
    private UserPaymentMethodRepository paymentMethodRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PaymentMethodServiceImpl paymentMethodService;

    private User testUser;
    private UserPaymentMethod testPaymentMethod;
    private PaymentMethodDto cardDto;
    private PaymentMethodDto upiDto;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("John Doe");

        testPaymentMethod = new UserPaymentMethod();
        testPaymentMethod.setId(1L);
        testPaymentMethod.setUser(testUser);
        testPaymentMethod.setType(UserPaymentMethod.PaymentType.card);
        testPaymentMethod.setCardNumber("**** **** **** 1234");
        testPaymentMethod.setCardholderName("John Doe");
        testPaymentMethod.setExpiryMonth("12");
        testPaymentMethod.setExpiryYear("2025");
        testPaymentMethod.setIsDefault(true);

        cardDto = new PaymentMethodDto();
        cardDto.setType(UserPaymentMethod.PaymentType.card);
        cardDto.setCardNumber("1234567890123456");
        cardDto.setCardholderName("John Doe");
        cardDto.setExpiryMonth("12");
        cardDto.setExpiryYear("2025");
        cardDto.setIsDefault(true);

        upiDto = new PaymentMethodDto();
        upiDto.setType(UserPaymentMethod.PaymentType.upi);
        upiDto.setUpiId("john@upi");
        upiDto.setIsDefault(false);
    }

    @Test
    void getUserPaymentMethods_ShouldReturnPaymentMethods_WhenUserExists() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(paymentMethodRepository.findByUser_Id(1L)).thenReturn(Arrays.asList(testPaymentMethod));

        List<UserPaymentMethod> result = paymentMethodService.getUserPaymentMethods(1L);

        assertEquals(1, result.size());
        assertEquals(testPaymentMethod, result.get(0));
        verify(userRepository).existsById(1L);
        verify(paymentMethodRepository).findByUser_Id(1L);
    }

    @Test
    void getUserPaymentMethods_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> paymentMethodService.getUserPaymentMethods(1L));
        verify(userRepository).existsById(1L);
        verify(paymentMethodRepository, never()).findByUser_Id(1L);
    }

    @Test
    void addPaymentMethod_ShouldCreateCardPaymentMethod_WhenValidCardData() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(paymentMethodRepository.save(any(UserPaymentMethod.class))).thenReturn(testPaymentMethod);

        UserPaymentMethod result = paymentMethodService.addPaymentMethod(1L, cardDto);

        assertEquals(testPaymentMethod, result);
        verify(userRepository).findById(1L);
        verify(paymentMethodRepository).save(any(UserPaymentMethod.class));
    }

    @Test
    void addPaymentMethod_ShouldCreateUpiPaymentMethod_WhenValidUpiData() {
        UserPaymentMethod upiPaymentMethod = new UserPaymentMethod();
        upiPaymentMethod.setId(2L);
        upiPaymentMethod.setUser(testUser);
        upiPaymentMethod.setType(UserPaymentMethod.PaymentType.upi);
        upiPaymentMethod.setUpiId("john@upi");
        upiPaymentMethod.setIsDefault(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(paymentMethodRepository.save(any(UserPaymentMethod.class))).thenReturn(upiPaymentMethod);

        UserPaymentMethod result = paymentMethodService.addPaymentMethod(1L, upiDto);

        assertEquals(upiPaymentMethod, result);
        verify(userRepository).findById(1L);
        verify(paymentMethodRepository).save(any(UserPaymentMethod.class));
    }

    @Test
    void addPaymentMethod_ShouldThrowException_WhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentMethodService.addPaymentMethod(1L, cardDto));
        verify(userRepository).findById(1L);
        verify(paymentMethodRepository, never()).save(any(UserPaymentMethod.class));
    }

    @Test
    void updatePaymentMethod_ShouldUpdatePaymentMethod_WhenValidData() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(testPaymentMethod));
        when(paymentMethodRepository.save(any(UserPaymentMethod.class))).thenReturn(testPaymentMethod);

        UserPaymentMethod result = paymentMethodService.updatePaymentMethod(1L, 1L, cardDto);

        assertEquals(testPaymentMethod, result);
        verify(userRepository).existsById(1L);
        verify(paymentMethodRepository).findById(1L);
        verify(paymentMethodRepository).save(testPaymentMethod);
    }

    @Test
    void updatePaymentMethod_ShouldThrowException_WhenPaymentMethodNotFound() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentMethodService.updatePaymentMethod(1L, 1L, cardDto));
        verify(paymentMethodRepository).findById(1L);
        verify(paymentMethodRepository, never()).save(any(UserPaymentMethod.class));
    }

    @Test
    void updatePaymentMethod_ShouldThrowException_WhenPaymentMethodNotBelongsToUser() {
        User otherUser = new User();
        otherUser.setId(2L);
        testPaymentMethod.setUser(otherUser);

        when(userRepository.existsById(1L)).thenReturn(true);
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(testPaymentMethod));

        assertThrows(ResourceNotFoundException.class, () -> paymentMethodService.updatePaymentMethod(1L, 1L, cardDto));
        verify(paymentMethodRepository, never()).save(any(UserPaymentMethod.class));
    }

    @Test
    void deletePaymentMethod_ShouldDeletePaymentMethod_WhenPaymentMethodExists() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(testPaymentMethod));

        paymentMethodService.deletePaymentMethod(1L, 1L);

        verify(userRepository).existsById(1L);
        verify(paymentMethodRepository).findById(1L);
        verify(paymentMethodRepository).delete(testPaymentMethod);
    }

    @Test
    void deletePaymentMethod_ShouldThrowException_WhenPaymentMethodNotFound() {
        when(userRepository.existsById(1L)).thenReturn(true);
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentMethodService.deletePaymentMethod(1L, 1L));
        verify(paymentMethodRepository, never()).delete(any(UserPaymentMethod.class));
    }

    @Test
    void deletePaymentMethod_ShouldThrowException_WhenPaymentMethodNotBelongsToUser() {
        User otherUser = new User();
        otherUser.setId(2L);
        testPaymentMethod.setUser(otherUser);

        when(userRepository.existsById(1L)).thenReturn(true);
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(testPaymentMethod));

        assertThrows(ResourceNotFoundException.class, () -> paymentMethodService.deletePaymentMethod(1L, 1L));
        verify(paymentMethodRepository, never()).delete(any(UserPaymentMethod.class));
    }

    @Test
    void maskCardNumber_ShouldMaskCardNumber_WhenValidCardNumber() {
        PaymentMethodServiceImpl service = new PaymentMethodServiceImpl(paymentMethodRepository, userRepository);
        
        // Test via the mapDtoToEntity method by creating a card payment method
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(paymentMethodRepository.save(any(UserPaymentMethod.class))).thenAnswer(invocation -> {
            UserPaymentMethod method = invocation.getArgument(0);
            assertEquals("**** **** **** 3456", method.getCardNumber());
            return method;
        });

        cardDto.setCardNumber("1234567890123456");
        service.addPaymentMethod(1L, cardDto);

        verify(paymentMethodRepository).save(any(UserPaymentMethod.class));
    }

    @Test
    void maskCardNumber_ShouldReturnAsterisks_WhenCardNumberTooShort() {
        PaymentMethodServiceImpl service = new PaymentMethodServiceImpl(paymentMethodRepository, userRepository);
        
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(paymentMethodRepository.save(any(UserPaymentMethod.class))).thenAnswer(invocation -> {
            UserPaymentMethod method = invocation.getArgument(0);
            assertEquals("****", method.getCardNumber());
            return method;
        });

        cardDto.setCardNumber("123");
        service.addPaymentMethod(1L, cardDto);

        verify(paymentMethodRepository).save(any(UserPaymentMethod.class));
    }
}