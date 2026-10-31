
package com.ecommerce.auth.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class UserRegisteredEventListener {

    private final UserRegisteredEventProducer eventProducer;

    public UserRegisteredEventListener(
            UserRegisteredEventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        eventProducer.publishUserRegistered(event);
    }
}
