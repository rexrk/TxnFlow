package txnflow.notificationservice.dto.event;

import java.util.UUID;

public record TopupFailedEvent(
        UUID eventId,
        UUID ledgerId,
        UUID userId,
        String email,
        Long amount
) {}