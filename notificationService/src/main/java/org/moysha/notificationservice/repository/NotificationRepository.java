package org.moysha.notificationservice.repository;

import org.moysha.notificationservice.model.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    boolean existsByExternalEventId(UUID externalEventId);
}
