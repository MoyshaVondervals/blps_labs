package org.moysha.notificationservice.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.moysha.notificationservice.model.enums.RecipientType;


import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications", uniqueConstraints = {
        @UniqueConstraint(name = "uk_notifications_external_event_id", columnNames = "external_event_id")
})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "recipient_type", nullable = false, length = 50)
    private RecipientType recipientType;

    @Column(name = "recipient_id", nullable = false)
    private Long recipientId;


    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "external_event_id")
    private UUID externalEventId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.isRead == null) {
            this.isRead = false;
        }
    }
}
