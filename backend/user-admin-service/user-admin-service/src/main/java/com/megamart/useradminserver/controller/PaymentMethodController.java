package com.megamart.useradminserver.controller;

import com.megamart.useradminserver.dto.MessageDto;
import com.megamart.useradminserver.dto.PaymentMethodDto;
import com.megamart.useradminserver.dto.UserPaymentMethodResponseDto;
import com.megamart.useradminserver.entity.UserPaymentMethod;
import com.megamart.useradminserver.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/users/{userId}/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @GetMapping
    public ResponseEntity<List<UserPaymentMethodResponseDto>> getUserPaymentMethods(@PathVariable Long userId) {
        List<UserPaymentMethod> methods = paymentMethodService.getUserPaymentMethods(userId);
        List<UserPaymentMethodResponseDto> methodDtos = methods.stream().map(UserPaymentMethodResponseDto::fromEntity).toList();
        return ResponseEntity.ok(methodDtos);
    }

    @PostMapping
    public ResponseEntity<UserPaymentMethodResponseDto> addPaymentMethod(@PathVariable Long userId, @Valid @RequestBody PaymentMethodDto paymentMethodDto) {
        UserPaymentMethod paymentMethod = paymentMethodService.addPaymentMethod(userId, paymentMethodDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(UserPaymentMethodResponseDto.fromEntity(paymentMethod));
    }

    @PutMapping("/{methodId}")
    public ResponseEntity<UserPaymentMethodResponseDto> updatePaymentMethod(@PathVariable Long userId, @PathVariable Long methodId, @Valid @RequestBody PaymentMethodDto paymentMethodDto) {
        UserPaymentMethod paymentMethod = paymentMethodService.updatePaymentMethod(userId, methodId, paymentMethodDto);
        return ResponseEntity.ok(UserPaymentMethodResponseDto.fromEntity(paymentMethod));
    }

    @DeleteMapping("/{methodId}")
    public ResponseEntity<MessageDto> deletePaymentMethod(@PathVariable Long userId, @PathVariable Long methodId) {
        paymentMethodService.deletePaymentMethod(userId, methodId);
        return ResponseEntity.ok(new MessageDto("Payment method deleted successfully"));
    }
}