
package com.ecommerce.notification.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.notification.entity.ProcessedEvent;

public interface ProcessedEventRepository
        extends JpaRepository<ProcessedEvent, Long> {

    Optional<ProcessedEvent> findByEventId(String eventId);

    boolean existsByEventId(String eventId);
}
