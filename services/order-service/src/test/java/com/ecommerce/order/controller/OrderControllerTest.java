
package com.ecommerce.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.ecommerce.order.client.dto.PaymentServiceResponse;
import com.ecommerce.order.dto.OrderCreateRequest;
import com.ecommerce.order.dto.OrderItemResponse;
import com.ecommerce.order.dto.OrderResponse;
import com.ecommerce.order.dto.PaymentInitiationResponse;
import com.ecommerce.order.entity.enums.OrderStatus;
import com.ecommerce.order.entity.enums.PaymentStatus;
import com.ecommerce.order.exception.GlobalExceptionHandler;
import com.ecommerce.order.service.OrderNotFoundException;
import com.ecommerce.order.service.OrderService;
import com.ecommerce.order.service.PaymentInitiationService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private OrderService orderService;

        @MockitoBean
        private PaymentInitiationService paymentInitiationService;

        private OrderResponse sampleOrderResponse() {
                return new OrderResponse(
                                5L,
                                10L,
                                OrderStatus.PENDING,
                                PaymentStatus.PENDING,
                                new BigDecimal("51.00"),
                                LocalDateTime.of(2026, 10, 21, 12, 0),
                                0L,
                                List.of(new OrderItemResponse(
                                                1L,
                                                2L,
                                                "Keyboard",
                                                new BigDecimal("25.50"),
                                                2,
                                                new BigDecimal("51.00"))));
        }

        @Test
        void pingShouldReturnSuccess() throws Exception {
                mockMvc.perform(get("/orders/ping"))
                                .andExpect(status().isOk())
                                .andExpect(content().string("Hello from Order Service!"));
        }

        @Test
        void createOrderShouldReturnCreatedResponse() throws Exception {
                when(orderService.createOrder(any(OrderCreateRequest.class)))
                                .thenReturn(sampleOrderResponse());

                String requestJson = """
                                {
                                  "customerId": 10,
                                  "items": [
                                    {
                                      "productId": 2,
                                      "productName": "Keyboard",
                                      "unitPrice": 25.50,
                                      "quantity": 2
                                    }
                                  ]
                                }
                                """;

                mockMvc.perform(post("/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(5))
                                .andExpect(jsonPath("$.customerId").value(10))
                                .andExpect(jsonPath("$.status").value("PENDING"))
                                .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
                                .andExpect(jsonPath("$.totalAmount").value(51.00))
                                .andExpect(jsonPath("$.items[0].productName").value("Keyboard"))
                                .andExpect(jsonPath("$.items[0].quantity").value(2));

                verify(orderService).createOrder(any(OrderCreateRequest.class));
        }

        @Test
        void createOrderShouldRejectInvalidRequest() throws Exception {
                String requestJson = """
                                {
                                  "customerId": 0,
                                  "items": []
                                }
                                """;

                mockMvc.perform(post("/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestJson))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value("BAD_REQUEST"))
                                .andExpect(jsonPath("$.message").exists());
        }

        @Test
        void getOrderByIdShouldReturnOrder() throws Exception {
                when(orderService.getOrderById(5L))
                                .thenReturn(sampleOrderResponse());

                mockMvc.perform(get("/orders/5"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(5))
                                .andExpect(jsonPath("$.customerId").value(10))
                                .andExpect(jsonPath("$.totalAmount").value(51.00));
        }

        @Test
        void getMissingOrderShouldReturnNotFound() throws Exception {
                when(orderService.getOrderById(99L))
                                .thenThrow(new OrderNotFoundException(99L));

                mockMvc.perform(get("/orders/99"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                                .andExpect(jsonPath("$.message").value("Order not found: 99"));
        }

        @Test
        void getOrdersByCustomerShouldReturnList() throws Exception {
                when(orderService.getOrdersByCustomerId(10L))
                                .thenReturn(List.of(sampleOrderResponse()));

                mockMvc.perform(get("/orders/customer/10"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].customerId").value(10))
                                .andExpect(jsonPath("$[0].id").value(5));
        }

        @Test
        void getOrdersByInvalidCustomerShouldReturnBadRequest() throws Exception {
                when(orderService.getOrdersByCustomerId(0L))
                                .thenThrow(new IllegalArgumentException(
                                                "Customer ID must be a positive number"));

                mockMvc.perform(get("/orders/customer/0"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
        }

        @Test
        void cancelOrderShouldReturnUpdatedOrder() throws Exception {
                OrderResponse cancelledResponse = new OrderResponse(
                                5L,
                                10L,
                                OrderStatus.CANCELLED,
                                PaymentStatus.PENDING,
                                new BigDecimal("51.00"),
                                LocalDateTime.of(2026, 10, 21, 12, 0),
                                1L,
                                List.of());

                when(orderService.cancelOrder(5L)).thenReturn(cancelledResponse);

                mockMvc.perform(post("/orders/5/cancel"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("CANCELLED"))
                                .andExpect(jsonPath("$.id").value(5));
        }

        @Test
        void cancelOrderInInvalidStateShouldReturnConflict() throws Exception {
                when(orderService.cancelOrder(5L))
                                .thenThrow(new IllegalStateException(
                                                "Order cannot be cancelled in its current state"));

                mockMvc.perform(post("/orders/5/cancel"))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.status").value("CONFLICT"))
                                .andExpect(jsonPath("$.message").value(
                                                "Order cannot be cancelled in its current state"));
        }

        @Test
        void initiatePaymentShouldReturnOrderAndPayment() throws Exception {
                OrderResponse paymentPendingOrder = new OrderResponse(
                                5L,
                                10L,
                                OrderStatus.PAYMENT_PENDING,
                                PaymentStatus.PENDING,
                                new BigDecimal("51.00"),
                                LocalDateTime.of(2026, 10, 21, 12, 0),
                                1L,
                                List.of());

                PaymentServiceResponse payment = new PaymentServiceResponse(
                                12L,
                                "order-5-payment",
                                5L,
                                new BigDecimal("51.00"),
                                "PENDING",
                                LocalDateTime.of(2026, 10, 23, 12, 0),
                                LocalDateTime.of(2026, 10, 23, 12, 0),
                                0L);

                when(paymentInitiationService.initiatePayment(5L))
                                .thenReturn(new PaymentInitiationResponse(
                                                paymentPendingOrder, payment));

                mockMvc.perform(post("/orders/5/payment"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.order.id").value(5))
                                .andExpect(jsonPath("$.order.status").value("PAYMENT_PENDING"))
                                .andExpect(jsonPath("$.payment.id").value(12))
                                .andExpect(jsonPath("$.payment.orderId").value(5))
                                .andExpect(jsonPath("$.payment.amount").value(51.00))
                                .andExpect(jsonPath("$.payment.status").value("PENDING"))
                                .andExpect(jsonPath("$.payment.idempotencyKey")
                                                .value("order-5-payment"));

                verify(paymentInitiationService).initiatePayment(5L);
        }
}
