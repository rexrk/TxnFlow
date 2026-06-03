package txnflow.notificationservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
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

    Optional<NotificationEvent> findByNotificationTypeAndReferenceId(
            NotificationType notificationType,
            UUID referenceId
    );

    List<NotificationEvent> findByStatusAndNextRetryAtLessThanEqual(
            NotificationStatus status,
            Instant time
    );

}