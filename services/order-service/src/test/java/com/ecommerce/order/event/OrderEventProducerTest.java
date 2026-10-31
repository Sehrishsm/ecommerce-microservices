package com.ecommerce.order.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class OrderEventProducerTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OrderEventProducer producer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        producer = new OrderEventProducer(kafkaTemplate, objectMapper);
    }

    @Test
    void publishOrderCreatedShouldWaitForKafkaAcknowledgement()
            throws Exception {

        OrderCreatedEvent event = new OrderCreatedEvent(
                "event-created-101",
                "OrderCreated",
                Instant.parse("2026-10-27T10:00:00Z"),
                101L,
                7L,
                new BigDecimal("49.98"),
                "USD");

        mockSuccessfulSend("order.created", 101L);

        producer.publishOrderCreated(event);

        org.mockito.ArgumentCaptor<String> messageCaptor =
                org.mockito.ArgumentCaptor.forClass(String.class);

        verify(kafkaTemplate).send(
                eq("order.created"),
                eq("101"),
                messageCaptor.capture());

        JsonNode json = objectMapper.readTree(messageCaptor.getValue());

        assertEquals("event-created-101", json.path("eventId").asText());
        assertEquals("OrderCreated", json.path("eventType").asText());
        assertEquals("101", json.path("orderId").asText());
        assertEquals("7", json.path("customerId").asText());
        assertEquals("49.98", json.path("totalAmount").asText());
        assertEquals("USD", json.path("currency").asText());
    }

    @Test
    void publishOrderCancelledShouldWaitForKafkaAcknowledgement()
            throws Exception {

        OrderCancelledEvent event = new OrderCancelledEvent(
                "event-cancelled-102",
                "OrderCancelled",
                Instant.parse("2026-10-27T10:05:00Z"),
                102L,
                8L);

        mockSuccessfulSend("order.cancelled", 102L);

        producer.publishOrderCancelled(event);

        org.mockito.ArgumentCaptor<String> messageCaptor =
                org.mockito.ArgumentCaptor.forClass(String.class);

        verify(kafkaTemplate).send(
                eq("order.cancelled"),
                eq("102"),
                messageCaptor.capture());

        JsonNode json = objectMapper.readTree(messageCaptor.getValue());

        assertEquals("event-cancelled-102", json.path("eventId").asText());
        assertEquals("OrderCancelled", json.path("eventType").asText());
        assertEquals("102", json.path("orderId").asText());
        assertEquals("8", json.path("customerId").asText());
    }

    @Test
    void publishOrderCreatedShouldReportKafkaSendFailure() {

        OrderCreatedEvent event = new OrderCreatedEvent(
                "event-created-101",
                "OrderCreated",
                Instant.parse("2026-10-27T10:00:00Z"),
                101L,
                7L,
                new BigDecimal("49.98"),
                "USD");

        when(kafkaTemplate.send(
                eq("order.created"),
                eq("101"),
                anyString()))
                .thenReturn(CompletableFuture.failedFuture(
                        new RuntimeException("Kafka unavailable")));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> producer.publishOrderCreated(event));

        assertEquals(
                "Failed to publish OrderCreated event to topic "
                        + "order.created for order 101",
                exception.getMessage());
    }

    private void mockSuccessfulSend(String topic, Long orderId) {

        SendResult<String, String> sendResult = mock(SendResult.class);
        RecordMetadata metadata = mock(RecordMetadata.class);

        when(sendResult.getRecordMetadata()).thenReturn(metadata);
        when(metadata.topic()).thenReturn(topic);
        when(metadata.partition()).thenReturn(0);
        when(metadata.offset()).thenReturn(10L);

        when(kafkaTemplate.send(
                eq(topic),
                eq(orderId.toString()),
                anyString()))
                .thenReturn(CompletableFuture.completedFuture(sendResult));
    }
}