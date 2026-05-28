package txnflow.walletservice.wallet.dto.request;

import java.util.UUID;

public record WalletCreditEvent(
        UUID ledgerId,
        UUID userId,
        Long amount
) {}