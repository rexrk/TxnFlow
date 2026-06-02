package txnflow.notificationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import txnflow.notificationservice.entity.NotificationEvent;
import txnflow.notificationservice.enums.NotificationStatus;
import txnflow.notificationservice.enums.NotificationType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<NotificationEvent, UUID> {

    Optional<NotificationEvent> findByEventId(UUID eventId);

    List<NotificationEvent> findByUserId(UUID userId);

    @Modifying
    @Query("""
         update NotificationEvent n
         set n.status = 'PROCESSING'
         where n.eventId = :eventId
         and n.status = 'RECEIVED'
    """)
    int claimEvent(UUID eventId);

    Optional<NotificationEvent> findByNotificationTypeAndReferenceId(
            NotificationType notificationType,
            UUID referenceId
    );

    List<NotificationEvent> findByStatusAndNextRetryAtLessThanEqual(
            NotificationStatus status,
            Instant time
    );

}