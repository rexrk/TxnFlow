package txnflow.paymentadapterservice.service.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import txnflow.paymentadapterservice.dto.event.WalletCreditEvent;
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

    public static final String KAFKA_TOPIC = "wallet.credit.on-topup";
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
            WalletCreditEvent walletCreditEvent = new WalletCreditEvent(
                    event.getLedgerId(),
                    event.getUserId(),
                    event.getAmount()
            );

            kafkaTemplate.send(KAFKA_TOPIC, event.getUserId().toString(), walletCreditEvent).get();

            event.setStatus(OutboxStatus.PROCESSED);
            log.info("Successfully processed outbox event id={} ledgerId={}", event.getId(), event.getLedgerId());

        } catch (Exception ex) {
            int retry = event.getRetryCount() + 1;
            event.setRetryCount(retry);
            event.setStatus(retry >= MAX_RETRY ? OutboxStatus.FAILED : OutboxStatus.PENDING);

            log.error("Failed processing outbox event id={} retry={}", event.getId(), retry, ex);

        }
    }

}