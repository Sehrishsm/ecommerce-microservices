package com.ecommerce.payment.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import com.ecommerce.payment.entity.OutboxEvent;
import com.ecommerce.payment.entity.Payment;
import com.ecommerce.payment.entity.PaymentStatus;

import jakarta.persistence.EntityManager;

@DataJpaTest
@TestPropertySource(properties = {
                "spring.datasource.url=jdbc:h2:mem:paymenttestdb;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop"
})
class PaymentRepositoryTest {

        @Autowired
        private PaymentRepository paymentRepository;

        @Autowired
        private EntityManager entityManager;

        @Autowired
        private OutboxEventRepository outboxEventRepository;

        @Test
        void saveAndRetrievePaymentShouldPersistFieldsAndInitializeVersion() {
                Payment payment = new Payment(
                                1001L,
                                new BigDecimal("49.99"),
                                "integration-test-key-1");

                Payment saved = paymentRepository.saveAndFlush(payment);

                assertNotNull(saved.getId());
                assertEquals(PaymentStatus.PENDING, saved.getStatus());
                assertNotNull(saved.getCreatedAt());
                assertNotNull(saved.getUpdatedAt());
                assertNotNull(saved.getVersion());

                Long paymentId = saved.getId();

                entityManager.clear();

                Payment retrieved = paymentRepository.findById(paymentId)
                                .orElseThrow();

                assertEquals(1001L, retrieved.getOrderId());
                assertEquals(0, new BigDecimal("49.99")
                                .compareTo(retrieved.getAmount()));
                assertEquals("integration-test-key-1",
                                retrieved.getIdempotencyKey());
                assertEquals(PaymentStatus.PENDING, retrieved.getStatus());
        }

        @Test
        void updatingPaymentStatusShouldIncrementVersion() {
                Payment payment = new Payment(
                                1002L,
                                new BigDecimal("25.00"),
                                "integration-test-key-2");

                Payment saved = paymentRepository.saveAndFlush(payment);
                Long initialVersion = saved.getVersion();

                saved.markProcessing();
                Payment updated = paymentRepository.saveAndFlush(saved);

                assertEquals(PaymentStatus.PROCESSING, updated.getStatus());
                assertEquals(initialVersion + 1L, updated.getVersion());
                assertNotNull(updated.getUpdatedAt());
        }

        @Test
        void saveAndRetrieveOutboxEventShouldPersistFields() {
                OutboxEvent event = new OutboxEvent(
                                "event-payment-101",
                                "Payment",
                                101L,
                                "PaymentSucceeded",
                                "payment.succeeded",
                                "{\"eventId\":\"event-payment-101\"}");

                OutboxEvent saved = outboxEventRepository.saveAndFlush(event);

                assertNotNull(saved.getId());
                assertEquals("event-payment-101", saved.getEventId());
                assertEquals("Payment", saved.getAggregateType());
                assertEquals(101L, saved.getAggregateId());
                assertEquals("PaymentSucceeded", saved.getEventType());
                assertEquals("payment.succeeded", saved.getTopic());
                assertEquals(OutboxEvent.Status.PENDING, saved.getStatus());
                assertEquals(0, saved.getAttemptCount());

                Long eventId = saved.getId();

                entityManager.clear();

                OutboxEvent retrieved = outboxEventRepository.findById(eventId)
                                .orElseThrow();

                assertEquals("event-payment-101", retrieved.getEventId());
                assertEquals(
                                "{\"eventId\":\"event-payment-101\"}",
                                retrieved.getPayload());
                assertEquals(OutboxEvent.Status.PENDING, retrieved.getStatus());
        }

        @Test
        void markingOutboxEventPublishedShouldPersistPublishedStatus() {
                OutboxEvent event = new OutboxEvent(
                                "event-payment-102",
                                "Payment",
                                102L,
                                "PaymentFailed",
                                "payment.failed",
                                "{\"eventId\":\"event-payment-102\"}");

                OutboxEvent saved = outboxEventRepository.saveAndFlush(event);

                saved.markPublished();

                OutboxEvent updated = outboxEventRepository.saveAndFlush(saved);

                assertEquals(OutboxEvent.Status.PUBLISHED, updated.getStatus());
                assertNotNull(updated.getPublishedAt());
                assertEquals(0, updated.getAttemptCount());
        }
}
