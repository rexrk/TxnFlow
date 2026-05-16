package txnflow.walletservice.wallet.dto.response;

import txnflow.walletservice.wallet.enums.WalletStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletResponse(
        UUID id,
        UUID userId,
        BigDecimal balance,
        String currency,
        WalletStatus status
) {
}