package txnflow.paymentadapterservice.scheduler;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import txnflow.paymentadapterservice.enums.OutboxStatus;
import txnflow.paymentadapterservice.repository.OutboxRepository;
import txnflow.paymentadapterservice.service.internal.OutboxProcessor;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxScheduler {

    private final OutboxRepository outboxRepository;
    private final OutboxProcessor outboxProcessor;

    private static final int BATCH_SIZE = 100;

    @PostConstruct
    public void init() {
        log.info("Outbox Scheduler initiated");
    }

    @Scheduled(fixedDelay = 5000)
    public void processOutbox() {
        List<UUID> events = outboxRepository.fetchPendingIds(OutboxStatus.PENDING.name(), BATCH_SIZE);

        if (events.isEmpty()) return;

        log.info("Outbox batch started. size={}", events.size());

        for (UUID event : events) outboxProcessor.processSingleEvent(event);

        log.info("Outbox batch processed successfully. total={}", events.size());
    }
}