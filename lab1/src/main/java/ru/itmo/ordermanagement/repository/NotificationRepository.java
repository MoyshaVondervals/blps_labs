package ru.itmo.ordermanagement.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.itmo.ordermanagement.model.entity.Notification;
import ru.itmo.ordermanagement.model.enums.RecipientType;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @EntityGraph(attributePaths = {"order"})
    Page<Notification> findByRecipientTypeAndRecipientIdOrderByCreatedAtDesc(
            RecipientType recipientType, Long recipientId, Pageable pageable);

    @EntityGraph(attributePaths = {"order"})
    Page<Notification> findByRecipientTypeAndRecipientIdAndIsReadFalseOrderByCreatedAtDesc(
            RecipientType recipientType, Long recipientId, Pageable pageable);
}
