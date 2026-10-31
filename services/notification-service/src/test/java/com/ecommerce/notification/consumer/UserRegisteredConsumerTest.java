package com.ecommerce.notification.consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ecommerce.notification.service.NotificationProcessingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@ExtendWith(MockitoExtension.class)
class UserRegisteredConsumerTest {

    @Mock
    private NotificationProcessingService notificationProcessingService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Test
    void shouldDeserializeAndDelegateUserRegisteredEvent() throws Exception {
        UserRegisteredConsumer consumer =
                new UserRegisteredConsumer(objectMapper, notificationProcessingService);

        String json = """
                {
                    "eventId": "user-event-test-001",
                    "eventType": "UserRegistered",
                    "occurredAt": "2026-10-24T06:00:00Z",
                    "userId": 42,
                    "email": "student@example.com"
                }
                """;

        assertDoesNotThrow(() -> consumer.consume(json));

        verify(notificationProcessingService).process(
                eq("user-event-test-001"),
                eq("UserRegistered"),
                eq("User registered: user 42, email student@example.com"));

        verifyNoMoreInteractions(notificationProcessingService);
    }
}