package txnflow.paymentadapterservice.scheduler;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import txnflow.paymentadapterservice.dto.response.WalletCreditEvent;
import txnflow.paymentadapterservice.entity.OutboxEvent;
import txnflow.paymentadapterservice.enums.OutboxStatus;
import txnflow.paymentadapterservice.repository.OutboxRepository;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxScheduler {

    private final OutboxRepository outboxRepository;

    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRY = 5;

    @PostConstruct
    public void init() {
        log.info("Outbox Scheduler initiated");
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void processOutbox() {
        List<OutboxEvent> events = outboxRepository.fetchPending(BATCH_SIZE);

        if (events.isEmpty()) return;

        log.info("Outbox batch started. size={}", events.size());

        for (OutboxEvent event : events) {

            try {

                // simulate send (Kafka later)
                // kafkaTemplate.send(...).get();

//                new WalletCreditEvent(
//                        event.getId(),
//                        event.getUserId(),
//                        event.getAmount()
//                );

                event.setStatus(OutboxStatus.PROCESSED);

                log.info(
                        "Successfully processed outbox event id={} ledgerId={}",
                        event.getId(),
                        event.getLedgerId()
                );

            } catch (Exception ex) {

                int retry = event.getRetryCount() + 1;
                event.setRetryCount(retry);

                log.error(
                        "Failed processing outbox event id={} retry={}",
                        event.getId(),
                        retry,
                        ex
                );

                event.setStatus(retry >= MAX_RETRY
                        ? OutboxStatus.FAILED
                        : OutboxStatus.PENDING);

                log.warn(
                        "Outbox event id={} marked as {}",
                        event.getId(),
                        event.getStatus()
                );
            }
        }

        outboxRepository.saveAll(events);

        log.info("Outbox batch processed successfully. total={}", events.size());
    }
}