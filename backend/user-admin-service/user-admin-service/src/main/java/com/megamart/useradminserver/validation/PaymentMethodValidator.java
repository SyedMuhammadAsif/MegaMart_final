package com.megamart.useradminserver.validation;

import com.megamart.useradminserver.dto.PaymentMethodDto;
import com.megamart.useradminserver.entity.UserPaymentMethod;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;

public class PaymentMethodValidator implements ConstraintValidator<ValidPaymentMethod, PaymentMethodDto> {

    @Override
    public void initialize(ValidPaymentMethod constraintAnnotation) {
        // This method is intentionally empty as no initialization is required
        // for this validator. The validation logic is handled in isValid() method.
    }

    @Override
    public boolean isValid(PaymentMethodDto dto, ConstraintValidatorContext context) {
        if (dto == null || dto.getType() == null) {
            return false;
        }

        context.disableDefaultConstraintViolation();

        if (dto.getType() == UserPaymentMethod.PaymentType.card) {
            return validateCardDetails(dto, context);
        } else if (dto.getType() == UserPaymentMethod.PaymentType.upi) {
            return validateUpiDetails(dto, context);
        }

        return false;
    }

    private boolean validateCardDetails(PaymentMethodDto dto, ConstraintValidatorContext context) {
        boolean isValid = true;

        if (dto.getCardNumber() == null || dto.getCardNumber().trim().isEmpty()) {
            context.buildConstraintViolationWithTemplate("Card number is required for card payments")
                    .addConstraintViolation();
            isValid = false;
        } else {
            String cardNumber = dto.getCardNumber().replaceAll("\\s", "");
            if (cardNumber.length() < 13 || cardNumber.length() > 19 || !cardNumber.matches("\\d+")) {
                context.buildConstraintViolationWithTemplate("Card number must be 13-19 digits")
                        .addConstraintViolation();
                isValid = false;
            }
        }

        if (dto.getCardholderName() == null || dto.getCardholderName().trim().length() < 2) {
            context.buildConstraintViolationWithTemplate("Cardholder name is required (minimum 2 characters)")
                    .addConstraintViolation();
            isValid = false;
        }

        if (dto.getExpiryMonth() == null || dto.getExpiryYear() == null) {
            context.buildConstraintViolationWithTemplate("Expiry month and year are required")
                    .addConstraintViolation();
            isValid = false;
        } else {
            try {
                int month = Integer.parseInt(dto.getExpiryMonth());
                int year = Integer.parseInt(dto.getExpiryYear());
                LocalDate now = LocalDate.now();
                LocalDate expiry = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1);
                
                if (expiry.isBefore(now)) {
                    context.buildConstraintViolationWithTemplate("Card has expired")
                            .addConstraintViolation();
                    isValid = false;
                }
            } catch (Exception e) {
                context.buildConstraintViolationWithTemplate("Invalid expiry date format")
                        .addConstraintViolation();
                isValid = false;
            }
        }

        return isValid;
    }

    private boolean validateUpiDetails(PaymentMethodDto dto, ConstraintValidatorContext context) {
        if (dto.getUpiId() == null || dto.getUpiId().trim().isEmpty()) {
            context.buildConstraintViolationWithTemplate("UPI ID is required for UPI payments")
                    .addConstraintViolation();
            return false;
        }

        String upiId = dto.getUpiId().trim();
        if (!upiId.matches("^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+$")) {
            context.buildConstraintViolationWithTemplate("Invalid UPI ID format (e.g., user@paytm)")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}