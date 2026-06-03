package txnflow.notificationservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import txnflow.notificationservice.enums.NotificationStatus;
import txnflow.notificationservice.enums.NotificationType;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "notification_event",
        indexes = {
                @Index(name = "idx_notification_retry", columnList = "status,next_retry_at"),
                @Index(name = "idx_notification_user_id", columnList = "user_id"),
                @Index(
                        name = "idx_notification_type_reference",
                        columnList = "notification_type, reference_id"
                )
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_notification_event_event_id", columnNames = "event_id")
        }
)
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "reference_id", nullable = false, updatable = false)
    private UUID referenceId;

    @Column(nullable = false)
    private String recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;

    @Column(name = "provider_response_id")
    private String providerResponseId;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "failure_reason", length = 2000)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private Instant createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private Instant updatedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

}