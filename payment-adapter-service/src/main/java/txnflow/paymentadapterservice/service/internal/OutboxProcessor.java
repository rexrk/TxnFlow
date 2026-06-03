package txnflow.paymentadapterservice.service.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import txnflow.paymentadapterservice.constant.KafkaTopic;
import txnflow.paymentadapterservice.dto.event.TopupCompletedEvent;
import txnflow.paymentadapterservice.dto.event.TopupFailedEvent;
import txnflow.paymentadapterservice.entity.OutboxEvent;
import txnflow.paymentadapterservice.enums.OutboxStatus;
import txnflow.paymentadapterservice.repository.OutboxRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final int MAX_RETRY = 5;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleEvent(UUID eventId) {
        outboxRepository.findById(eventId)
                .ifPresentOrElse(
                        this::executeTransmission,
                        () -> log.warn("Outbox event not found id={}", eventId)
                );

    }

    private void executeTransmission(OutboxEvent event) {
        if (event.getStatus() == OutboxStatus.PROCESSED) return;

        try {
            TopupCompletedEvent topupCompletedEvent = new TopupCompletedEvent(
                    UUID.randomUUID(),
                    event.getLedgerId(),
                    event.getUserId(),
                    event.getEmail(),
                    event.getAmount()
            );

            kafkaTemplate.send(KafkaTopic.TOPUP_COMPLETED, event.getUserId().toString(), topupCompletedEvent);

            event.setStatus(OutboxStatus.PROCESSED);
            log.info("Successfully processed outbox event id={} ledgerId={}", event.getId(), event.getLedgerId());

        } catch (Exception ex) {
            int retry = event.getRetryCount() + 1;
            event.setRetryCount(retry);
            event.setStatus(retry >= MAX_RETRY ? OutboxStatus.FAILED : OutboxStatus.PENDING);

            if(retry >= MAX_RETRY) {
                TopupFailedEvent topupFailedEvent = new TopupFailedEvent(
                        UUID.randomUUID(),
                        event.getLedgerId(),
                        event.getUserId(),
                        event.getEmail(),
                        event.getAmount()
                );
                kafkaTemplate.send(KafkaTopic.TOPUP_FAILED, event.getUserId().toString(), topupFailedEvent);
            }

            log.error("Failed processing outbox event id={} retry={}", event.getId(), retry, ex);

        }
    }

}