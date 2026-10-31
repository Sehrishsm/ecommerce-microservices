
package com.ecommerce.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ecommerce.notification.repository.ProcessedEventRepository;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:notification-test;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.kafka.listener.auto-startup=false",
        "eureka.client.enabled=false"
})
class NotificationProcessingServiceTest {

    @Autowired
    private NotificationProcessingService notificationProcessingService;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @BeforeEach
    void cleanDatabase() {
        processedEventRepository.deleteAll();
    }

    @Test
    void shouldRecordNewEvent() {
        boolean processed = notificationProcessingService.process(
                "event-101",
                "OrderCreated",
                "Order 101 created");

        assertTrue(processed);
        assertTrue(processedEventRepository.existsByEventId("event-101"));
        assertEquals(1, processedEventRepository.count());
    }

    @Test
    void shouldSkipDuplicateEventId() {
        boolean firstAttempt = notificationProcessingService.process(
                "event-102",
                "OrderCreated",
                "Order 102 created");

        boolean secondAttempt = notificationProcessingService.process(
                "event-102",
                "OrderCreated",
                "Duplicate order notification");

        assertTrue(firstAttempt);
        assertFalse(secondAttempt);
        assertEquals(1, processedEventRepository.count());
    }

    @Test
    void shouldProcessDifferentEventIdsIndependently() {
        notificationProcessingService.process(
                "event-103",
                "OrderCreated",
                "Order 103 created");

        notificationProcessingService.process(
                "event-104",
                "PaymentSucceeded",
                "Payment succeeded");

        assertEquals(2, processedEventRepository.count());
    }

    @Test
    void shouldRejectBlankEventIdWithoutRecordingIt() {
        assertThrows(
                IllegalArgumentException.class,
                () -> notificationProcessingService.process(
                        " ",
                        "OrderCreated",
                        "Order created"));

        assertEquals(0, processedEventRepository.count());
    }
}
