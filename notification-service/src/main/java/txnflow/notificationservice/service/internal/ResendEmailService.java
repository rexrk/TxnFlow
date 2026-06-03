package txnflow.notificationservice.service.internal;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.core.net.RequestOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import txnflow.notificationservice.dto.request.NotificationRequest;
import txnflow.notificationservice.entity.NotificationEvent;
import txnflow.notificationservice.enums.NotificationStatus;
import txnflow.notificationservice.exception.EmailDeliveryException;
import txnflow.notificationservice.repository.NotificationRepository;
import txnflow.notificationservice.service.EmailService;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResendEmailService implements EmailService {

    private final NotificationRepository repository;
    private final Resend resend;
    private static final int MAX_KAFKA_RETRIES = 5;

    @Override
    public void send(NotificationRequest request) {
        try {
            // 1. ENSURE ROW EXISTS (IDEMPOTENCY BASE)
            NotificationEvent entity;
            try {
                entity = repository.findByEventId(request.eventId())
                        .orElseGet(() -> repository.save(
                                NotificationEvent.builder()
                                        .eventId(request.eventId())
                                        .userId(request.userId())
                                        .recipient(request.recipient())
                                        .referenceId(request.referenceId())
                                        .notificationType(request.type())
                                        .status(NotificationStatus.RECEIVED)
                                        .retryCount(0)
                                        .build()
                        ));

            } catch (DataIntegrityViolationException e) {
                entity = repository.findByEventId(request.eventId())
                        .orElseThrow();
            }

            // 2. DUPLICATE MESSAGE CHECK
            if (entity.getStatus() == NotificationStatus.SENT ||
                    entity.getStatus() == NotificationStatus.FAILED) {
                return;
            }

            // 3. SEND EMAIL (EXTERNAL CALL)
            CreateEmailResponse providerResponse = sendEmail(request);

            // 4. UPDATE TO SENT
            markSuccess(entity, providerResponse);

            log.info("Email sent. eventId={}", request.eventId());

        } catch (Exception e) {
            NotificationEvent failedEntity = repository.findByEventId(request.eventId())
                    .orElseThrow(() -> new IllegalStateException("Notification not found: " + request.eventId()));

            int retryCount = failedEntity.getRetryCount() + 1;
            failedEntity.setRetryCount(retryCount);

            if (retryCount >= MAX_KAFKA_RETRIES) {
                // Max retries reached: Mark FAILED for scheduler
                markAsFailed(failedEntity, e);

                log.warn("Max retries reached. Moved to scheduler. eventId={} retryCount={}",
                        request.eventId(), retryCount);

            } else {
                // Still retryable: Keep RECEIVED, throw to Kafka
                failedEntity.setStatus(NotificationStatus.RECEIVED);
                repository.save(failedEntity);

                log.info("Retryable error. eventId={} retryCount={}/{}",
                        request.eventId(), retryCount, MAX_KAFKA_RETRIES);

                throw new EmailDeliveryException("EMAIL_SEND_FAILED", e);

            }
        }

    }

    private CreateEmailResponse sendEmail(NotificationRequest request) {
        try {
            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey(request.eventId().toString())
                    .build();

            return resend.emails().send(request.email(), options);

        } catch (ResendException e) {
            throw new EmailDeliveryException("EMAIL_SEND_FAILED", e);

        }
    }

    private void markAsFailed(NotificationEvent entity,  Exception e) {
        entity.setStatus(NotificationStatus.FAILED);
        entity.setFailureReason(e.getMessage());
        entity.setNextRetryAt(Instant.now().plusSeconds(60));
        repository.save(entity);
    }

    private void markSuccess(NotificationEvent entity, CreateEmailResponse response) {
        entity.setStatus(NotificationStatus.SENT);
        entity.setProviderResponseId(response.getId());
        entity.setSentAt(Instant.now());
        repository.save(entity);
    }

}
