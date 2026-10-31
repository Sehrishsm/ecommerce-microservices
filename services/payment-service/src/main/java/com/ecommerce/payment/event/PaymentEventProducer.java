
package com.ecommerce.payment.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.ecommerce.payment.entity.Payment;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class PaymentEventProducer {

    private static final Logger logger = LoggerFactory.getLogger(PaymentEventProducer.class);

    private static final String PAYMENT_SUCCEEDED_TOPIC = "payment.succeeded";
    private static final String PAYMENT_FAILED_TOPIC = "payment.failed";
    private static final String CURRENCY = "USD";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public PaymentEventProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    // Existing method: retained for backward compatibility.
    public void publishPaymentSucceeded(Payment payment) {
        PaymentSucceededEvent event = new PaymentSucceededEvent(
                UUID.randomUUID().toString(),
                "PaymentSucceeded",
                Instant.now(),
                payment.getId(),
                payment.getOrderId(),
                formatAmount(payment.getAmount()),
                CURRENCY);

        publishPaymentSucceeded(event);
    }

    // New method: publishes the exact event stored in the outbox.
    public void publishPaymentSucceeded(PaymentSucceededEvent event) {
        publish(PAYMENT_SUCCEEDED_TOPIC, event.paymentId(), event);
    }

    // Existing method: retained for backward compatibility.
    public void publishPaymentFailed(Payment payment) {
        PaymentFailedEvent event = new PaymentFailedEvent(
                UUID.randomUUID().toString(),
                "PaymentFailed",
                Instant.now(),
                payment.getId(),
                payment.getOrderId(),
                formatAmount(payment.getAmount()),
                CURRENCY);

        publishPaymentFailed(event);
    }

    // New method: publishes the exact event stored in the outbox.
    public void publishPaymentFailed(PaymentFailedEvent event) {
        publish(PAYMENT_FAILED_TOPIC, event.paymentId(), event);
    }

    private String formatAmount(BigDecimal amount) {
        return amount.toPlainString();
    }

    private void publish(String topic, Long paymentId, Object event) {
        final String message;

        try {
            message = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize payment event for payment " + paymentId,
                    exception);
        }

        try {
            RecordMetadata metadata = kafkaTemplate
                    .send(topic, paymentId.toString(), message)
                    .get()
                    .getRecordMetadata();

            logger.info(
                    "Published payment event: topic={}, partition={}, offset={}, paymentId={}",
                    metadata.topic(),
                    metadata.partition(),
                    metadata.offset(),
                    paymentId);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                    "Interrupted while publishing payment event for payment "
                            + paymentId,
                    exception);
        } catch (ExecutionException exception) {
            throw new IllegalStateException(
                    "Failed to publish payment event to topic "
                            + topic + " for payment " + paymentId,
                    exception.getCause());
        }
    }
}
