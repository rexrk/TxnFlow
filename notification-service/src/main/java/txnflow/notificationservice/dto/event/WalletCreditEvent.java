package txnflow.notificationservice.dto.event;

import java.util.UUID;

public record WalletCreditEvent(
        UUID ledgerId,
        UUID userId,
        Long amount
) {}