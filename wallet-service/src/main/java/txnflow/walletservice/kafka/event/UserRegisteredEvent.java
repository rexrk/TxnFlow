package txnflow.walletservice.kafka.event;

import java.util.UUID;

public record UserRegisteredEvent(
        UUID userId,
        String email
) {}