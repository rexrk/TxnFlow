package txnflow.notificationservice.service.internal;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import txnflow.notificationservice.dto.request.NotificationRequest;
import txnflow.notificationservice.entity.NotificationEvent;
import txnflow.notificationservice.enums.NotificationStatus;
import txnflow.notificationservice.repository.NotificationRepository;
import txnflow.notificationservice.service.EmailService;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResendEmailService implements EmailService {

    private final NotificationRepository repository;
    private final Resend resend;

    @Override
    public void send(NotificationRequest request) throws ResendException {

        NotificationEvent entity;

        try {
            // 1. Fetch existing
            Optional<NotificationEvent> existing = repository.findByEventId(request.eventId());

            // idempotency check
            if (existing.isPresent()) {
                entity = existing.get();

                // already completed
                if (entity.getStatus() == NotificationStatus.SENT) return;

            } else {
                // create new
                entity = NotificationEvent.builder()
                        .eventId(request.eventId())
                        .userId(request.userId())
                        .recipient(request.recipient())
                        .referenceId(request.referenceId())
                        .notificationType(request.type())
                        .status(NotificationStatus.RECEIVED)
                        .retryCount(0)
                        .build();

                repository.save(entity);

            }
            int updated = repository.claimEvent(request.eventId());
            if (updated == 0) return;

            // 2. Send email (external call)
            CreateEmailResponse providerResponse = resend.emails().send(request.email());

            // 3. mark success
            entity.setStatus(NotificationStatus.SENT);
            entity.setProviderResponseId(providerResponse.getId());
            entity.setSentAt(Instant.now());

            repository.save(entity);

        } catch (ResendException e) {
            throw e;
        }
        catch (DataIntegrityViolationException ex) {
            log.debug("Notification already claimed. eventId={}", request.eventId());
            return;

        } catch (Exception ex) {

            log.error("Notification failed eventId={}", request.eventId(), ex);

            boolean retryKafka = true;

            try {
                Optional<NotificationEvent> existing = repository.findByEventId(request.eventId());

                if (existing.isPresent()) {

                    entity = existing.get();

                    int retryCount = entity.getRetryCount() + 1;

                    entity.setRetryCount(retryCount);
                    entity.setFailureReason(ex.getMessage());

                    if (retryCount < 5) {
                        entity.setStatus(NotificationStatus.RECEIVED);
                        repository.save(entity);
                    }
                    else {
                        entity.setStatus(NotificationStatus.FAILED);
                        entity.setNextRetryAt(Instant.now().plusSeconds(60));
                        repository.save(entity);

                        log.error("Notification moved to scheduler retry. eventId={}, retryCount={}",
                                request.eventId(),
                                retryCount
                        );

                        retryKafka = false;
                    }
                }

            } catch (Exception dbEx) {
                log.error("DB update also failed eventId={}", request.eventId(), dbEx);
                throw dbEx;
            }

            if (retryKafka) {
                throw ex;
            }
        }
    }
}
