
package com.ecommerce.payment.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class PaymentRequestTest {

    private ValidatorFactory validatorFactory;
    private Validator validator;

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @Test
    void validRequestShouldHaveNoViolations() {
        PaymentRequest request = createValidRequest();

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
        validatorFactory.close();
    }

    @Test
    void missingOrderIdShouldBeRejected() {
        PaymentRequest request = createValidRequest();
        request.setOrderId(null);

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        validatorFactory.close();
    }

    @Test
    void zeroAmountShouldBeRejected() {
        PaymentRequest request = createValidRequest();
        request.setAmount(new BigDecimal("0.00"));

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size());
        validatorFactory.close();
    }

    @Test
    void blankIdempotencyKeyShouldBeRejected() {
        PaymentRequest request = createValidRequest();
        request.setIdempotencyKey("   ");

        Set<ConstraintViolation<PaymentRequest>> violations = validator.validate(request);

        assertTrue(violations.stream().anyMatch(
                violation -> violation.getPropertyPath()
                        .toString().equals("idempotencyKey")));
        validatorFactory.close();
    }

    private PaymentRequest createValidRequest() {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(1001L);
        request.setAmount(new BigDecimal("49.99"));
        request.setIdempotencyKey("payment-test-key");
        return request;
    }
}
