package txnflow.walletservice.kafka.event;

import java.util.UUID;

public record WalletCreditEvent(
        UUID ledgerId,
        UUID userId,
        Long amount
) {}