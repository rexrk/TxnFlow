package txnflow.walletservice.transaction.dto.response;

import txnflow.walletservice.transaction.enums.TransactionCategory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletTransactionListItemResponse(
        UUID transactionId,
        UUID referenceId,
        UUID counterpartyId,
        TransactionCategory category,
        BigDecimal amount,
        Instant createdAt
) {
}