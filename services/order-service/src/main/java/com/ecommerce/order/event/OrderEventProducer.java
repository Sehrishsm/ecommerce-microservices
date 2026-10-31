package com.ecommerce.order.event;

import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OrderEventProducer {

    private static final Logger logger =
            LoggerFactory.getLogger(OrderEventProducer.class);

    private static final String ORDER_CREATED_TOPIC = "order.created";
    private static final String ORDER_CANCELLED_TOPIC = "order.cancelled";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OrderEventProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishOrderCreated(OrderCreatedEvent event) {
        publish(
                ORDER_CREATED_TOPIC,
                event.orderId(),
                event,
                "OrderCreated");
    }

    public void publishOrderCancelled(OrderCancelledEvent event) {
        publish(
                ORDER_CANCELLED_TOPIC,
                event.orderId(),
                event,
                "OrderCancelled");
    }

    private void publish(
            String topic,
            Long orderId,
            Object event,
            String eventType) {

        final String message;

        try {
            message = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize " + eventType
                            + " event for order " + orderId,
                    exception);
        }

        try {
            RecordMetadata metadata = kafkaTemplate
                    .send(topic, orderId.toString(), message)
                    .get()
                    .getRecordMetadata();

            logger.info(
                    "Published order event: topic={}, partition={}, offset={}, orderId={}",
                    metadata.topic(),
                    metadata.partition(),
                    metadata.offset(),
                    orderId);

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Interrupted while publishing " + eventType
                            + " event for order " + orderId,
                    exception);

        } catch (ExecutionException exception) {
            throw new IllegalStateException(
                    "Failed to publish " + eventType
                            + " event to topic " + topic
                            + " for order " + orderId,
                    exception.getCause());
        }
    }
}