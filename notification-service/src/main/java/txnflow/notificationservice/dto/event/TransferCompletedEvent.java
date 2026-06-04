package txnflow.notificationservice.dto.event;

import java.util.UUID;

public record TransferCompletedEvent(
        UUID eventId,
        UUID userId,
        UUID transferId,
        String email,
        Long amount,
        String receiverEmail
) {
}
