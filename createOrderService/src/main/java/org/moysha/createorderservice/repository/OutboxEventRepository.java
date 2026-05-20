package org.moysha.createorderservice.repository;

import org.moysha.createorderservice.model.entity.OutboxEvent;
import org.moysha.createorderservice.model.enums.OutboxEventStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
