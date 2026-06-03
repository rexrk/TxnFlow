package txnflow.walletservice.kafka.event;

import java.util.UUID;

public record TopupCompletedEvent(
        UUID ledgerId,
        UUID userId,
        Long amount
) {}