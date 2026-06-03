package txnflow.paymentadapterservice.dto.event;

import java.util.UUID;

public record TopupCompletedEvent(
        UUID eventId,
        UUID ledgerId,
        UUID userId,
        String email,
        Long amount
) {}