package com.ecommerce.auth.event;

import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserRegisteredEventListenerTest {

    @Mock
    private UserRegisteredEventProducer eventProducer;

    @Test
    void onUserRegisteredShouldDelegateEventToProducer() {
        UserRegisteredEventListener listener =
                new UserRegisteredEventListener(eventProducer);

        UserRegisteredEvent event = new UserRegisteredEvent(
                "test-event-123",
                "UserRegistered",
                Instant.parse("2026-10-24T12:00:00Z"),
                42L,
                "student@example.com");

        listener.onUserRegistered(event);

        verify(eventProducer).publishUserRegistered(event);
    }
}
