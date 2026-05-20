package ru.itmo.ordermanagement.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.ordermanagement.model.entity.OutboxEvent;
import ru.itmo.ordermanagement.model.enums.OutboxEventStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findBySourceServiceAndStatusAndNextAttemptAtBeforeOrderByCreatedAtAsc(
            String sourceService,
            OutboxEventStatus status,
            LocalDateTime now,
            Pageable pageable
    );
}
