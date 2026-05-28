package txnflow.paymentadapterservice.dto.response;

import java.util.UUID;

public record WalletCreditEvent(
        UUID ledgerId,
        UUID userId,
        Long amount
) {}