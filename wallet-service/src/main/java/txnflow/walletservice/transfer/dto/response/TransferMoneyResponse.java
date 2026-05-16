package txnflow.walletservice.transfer.dto.response;

import txnflow.walletservice.transfer.enums.TransferStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferMoneyResponse(
        UUID transferId,
        TransferStatus status,
        BigDecimal amount,
        String currency,
        UUID senderWalletId,
        UUID receiverWalletId,
        Instant createdAt
) {
}