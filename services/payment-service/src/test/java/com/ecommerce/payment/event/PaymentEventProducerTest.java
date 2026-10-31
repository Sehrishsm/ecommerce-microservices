package com.ecommerce.payment.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.ecommerce.payment.entity.Payment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PaymentEventProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private PaymentEventProducer producer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        producer = new PaymentEventProducer(kafkaTemplate, objectMapper);
    }

    @Test
    void publishPaymentSucceededShouldWaitForKafkaAcknowledgement()
            throws Exception {

        Payment payment = mock(Payment.class);
        when(payment.getId()).thenReturn(34L);
        when(payment.getOrderId()).thenReturn(3L);
        when(payment.getAmount()).thenReturn(new BigDecimal("24.99"));

        SendResult<String, String> sendResult = mock(SendResult.class);
        RecordMetadata metadata = mock(RecordMetadata.class);

        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        when(metadata.topic()).thenReturn("payment.succeeded");
        when(metadata.partition()).thenReturn(0);
        when(metadata.offset()).thenReturn(10L);

        when(kafkaTemplate.send(
                eq("payment.succeeded"), eq("34"), anyString()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        producer.publishPaymentSucceeded(payment);

        ArgumentCaptor<String> messageCaptor =
                ArgumentCaptor.forClass(String.class);

        verify(kafkaTemplate).send(
                eq("payment.succeeded"), eq("34"), messageCaptor.capture());

        JsonNode event = objectMapper.readTree(messageCaptor.getValue());

        assertEquals("PaymentSucceeded", event.path("eventType").asText());
        assertEquals("34", event.path("paymentId").asText());
        assertEquals("3", event.path("orderId").asText());
        assertEquals("24.99", event.path("amount").asText());
        assertEquals("USD", event.path("currency").asText());
        assertEquals(false, event.path("eventId").asText().isBlank());
        assertEquals(false, event.path("occurredAt").asText().isBlank());
    }

    @Test
    void publishPaymentSucceededShouldReportKafkaSendFailure() {

        Payment payment = mock(Payment.class);
        when(payment.getId()).thenReturn(34L);
        when(payment.getOrderId()).thenReturn(3L);
        when(payment.getAmount()).thenReturn(new BigDecimal("24.99"));

        when(kafkaTemplate.send(
                eq("payment.succeeded"), eq("34"), anyString()))
                .thenReturn(CompletableFuture.failedFuture(
                        new RuntimeException("Kafka unavailable")));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> producer.publishPaymentSucceeded(payment));

        assertEquals(
                "Failed to publish payment event to topic "
                        + "payment.succeeded for payment 34",
                exception.getMessage());
    }
}
