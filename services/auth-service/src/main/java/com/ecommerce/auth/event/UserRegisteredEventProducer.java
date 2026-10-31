
package com.ecommerce.auth.event;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class UserRegisteredEventProducer {

    private static final String USER_REGISTERED_TOPIC = "user.registered";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public UserRegisteredEventProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishUserRegistered(UserRegisteredEvent event) {
        try {
            String message = objectMapper.writeValueAsString(event);

            kafkaTemplate.send(
                    USER_REGISTERED_TOPIC,
                    event.userId().toString(),
                    message);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize UserRegistered event for user "
                            + event.userId(),
                    exception);
        }
    }
}
