
package com.ecommerce.payment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ecommerce.payment.dto.PaymentRequest;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.exception.GlobalExceptionHandler;
import com.ecommerce.payment.service.PaymentService;

@WebMvcTest(PaymentController.class)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void pingShouldReturnOk() throws Exception {
        mockMvc.perform(get("/payments/ping"))
                .andExpect(status().isOk());
    }

    @Test
    void createPaymentShouldReturnCreated() throws Exception {
        Payment payment = new Payment(
                1001L, new BigDecimal("49.99"), "controller-test-key");

        when(paymentService.createPayment(any(PaymentRequest.class)))
                .thenReturn(payment);

        mockMvc.perform(post("/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "orderId": 1001,
                          "amount": 49.99,
                          "idempotencyKey": "controller-test-key"
                        }
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(1001))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(paymentService).createPayment(any(PaymentRequest.class));
    }

    @Test
    void createPaymentShouldRejectMissingOrderId() throws Exception {
        mockMvc.perform(post("/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "amount": 49.99,
                          "idempotencyKey": "invalid-controller-key"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Order ID is required"));

        verify(paymentService, never())
                .createPayment(any(PaymentRequest.class));
    }

    @Test
    void getPaymentByIdShouldReturnPayment() throws Exception {
        Payment payment = new Payment(
                1001L, new BigDecimal("49.99"), "controller-test-key");

        when(paymentService.getPaymentById(1L)).thenReturn(payment);

        mockMvc.perform(get("/payments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1001))
                .andExpect(jsonPath("$.amount").value(49.99));
    }

    @Test
    void getMissingPaymentShouldReturnNotFound() throws Exception {
        when(paymentService.getPaymentById(999999L))
                .thenThrow(new IllegalArgumentException("Payment not found: 999999"));

        mockMvc.perform(get("/payments/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("Payment not found: 999999"));
    }

    @Test
    void createPaymentShouldReturnConflictForIdempotencyConflict()
            throws Exception {

        when(paymentService.createPayment(any(PaymentRequest.class)))
                .thenThrow(new IllegalStateException(
                        "Idempotency key has already been used with different payment details"));

        mockMvc.perform(post("/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "orderId": 1001,
                          "amount": 49.99,
                          "idempotencyKey": "controller-test-key"
                        }
                        """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());
    }
}
