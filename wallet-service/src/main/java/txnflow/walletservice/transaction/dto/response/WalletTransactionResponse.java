package txnflow.walletservice.transaction.dto.response;

import txnflow.walletservice.transaction.enums.TransactionCategory;
import txnflow.walletservice.transaction.enums.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletTransactionResponse(
        UUID id,
        UUID transferId,
        TransactionType type,
        TransactionCategory category,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String currency,
        String description,
        CounterpartyDetails counterparty,
        Instant createdAt
) {
}